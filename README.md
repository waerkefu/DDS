# 代驾代办 H5 服务平台

一套面向代驾与车辆代办业务的多终端平台：**客户端 H5** 下单、**服务商接单 H5** 履约、**管理后台**统一管控。

核心链路：`下单 → 计价 → 接单 → 履约 → 模拟支付 → 完成`，并通过状态机、条件更新、乐观锁与幂等机制保障订单状态与金额的一致性。

---

## 一、目录结构

```
代驾HTNL5/
├── backend/                     # Spring Boot 后端（三端共用的 REST API）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/daijia/
│       │   ├── DaijiaApplication.java
│       │   ├── common/          # Result / ResultCode / BusinessException / 分页 / 单号生成
│       │   ├── config/          # MyBatis-Plus、WebMvc、OpenAPI、字段自动填充
│       │   ├── security/        # JWT、登录上下文、鉴权拦截器、密码加密
│       │   ├── infra/           # Redis 封装（可降级）、幂等服务、订单配置
│       │   ├── user/            # 账号、角色、权限
│       │   ├── provider/        # 服务商档案
│       │   ├── customer/        # 用户车辆、常用地址
│       │   ├── pricing/         # 计价规则与费用计算
│       │   ├── order/           # 订单、状态机、状态日志、超时任务
│       │   └── payment/         # 模拟支付与退款
│       └── resources/
│           ├── application.yml
│           └── application-dev.yml
├── frontend/
│   ├── customer-h5/             # 客户端 H5        (5174)
│   ├── provider-h5/             # 服务商接单 H5     (5175)
│   └── admin/                   # 管理后台          (5173)
├── docs/db/init.sql             # 建表 DDL + 初始化数据
└── .trae/specs/build-daijia-h5-platform/   # 规格文档（spec / tasks / checklist）
```

---

## 二、技术栈

| 层次 | 选型 |
|------|------|
| 后端 | Java 21、Spring Boot 3.2.5、MyBatis-Plus 3.5.5、Lombok、SpringDoc OpenAPI 2.3.0 |
| 认证 | JJWT 0.12.5（HS256）+ 自研鉴权拦截器 + PBKDF2 密码加密 |
| 存储 | MySQL 8.x（业务数据）、Redis（缓存 / 幂等加速，**可选**） |
| 前端 | Vue 3.4、Vite 5、Pinia 2、Vue Router 4、Element Plus 2.7、Axios |

> **说明**：本项目未使用 Spring Security，而是用轻量 `AuthInterceptor` + `@RequireRole` 注解实现登录校验与角色鉴权；
> 密码加密使用 JDK 原生 `PBKDF2WithHmacSHA256`（存储格式 `pbkdf2$迭代次数$盐值$摘要`）。
> 两者均无额外依赖，行为与 Spring Security + BCrypt 等价。

---

## 三、环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.x
- Node.js 18+ 与 npm
- Redis 6+（**可选**，未安装时后端自动降级，见第六节）

---

## 四、快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < docs/db/init.sql
```

脚本会自动创建 `daijia_h5` 相关的 13 张表，并写入演示角色、账号、服务商与计价规则。

> ⚠️ 脚本开头包含 `DROP TABLE IF EXISTS`，**仅可在开发/测试库执行**。
> 若尚未建库，可先执行：`CREATE DATABASE daijia_h5 DEFAULT CHARACTER SET utf8mb4;`

### 2. 启动后端

修改 `backend/src/main/resources/application-dev.yml` 中的数据库账号密码：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

然后启动：

```bash
cd backend
mvn spring-boot:run
```

- 服务端口：`http://localhost:8080`
- 接口文档（Swagger UI）：`http://localhost:8080/swagger-ui.html`

### 3. 启动三端前端

分别在三个目录执行：

```bash
cd frontend/admin        && npm install && npm run dev   # http://localhost:5173
cd frontend/customer-h5  && npm install && npm run dev   # http://localhost:5174
cd frontend/provider-h5  && npm install && npm run dev   # http://localhost:5175
```

三端均已在 `vite.config.js` 中配置 `/api` 代理到 `http://localhost:8080`，无需额外处理跨域。

---

## 五、演示账号

| 终端 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 管理后台 | `admin` | `admin123` | 平台管理员 |
| 客户端 H5 | `customer1` | `admin123` | 演示客户 |
| 服务商 H5 | `provider1` | `admin123` | 演示服务商（已启用、在线、接单中） |

---

## 六、核心业务说明

### 订单类型
`IMMEDIATE` 即时代驾、`SCHEDULED` 预约代驾、`INSPECTION` 年审代办、`RENEWAL` 换证代办。

### 订单状态机

```
PENDING_ACCEPT ──► ACCEPTED ──► IN_SERVICE ──► PENDING_PAYMENT ──► COMPLETED
      │                │                              │
      ├──► CANCELLED   ├──► CANCELLED                 └──► CANCELLED
      └──► CLOSED（超时未接单自动关闭）
```

- `COMPLETED` / `CANCELLED` / `CLOSED` 为终态，不可再流转。
- 所有流转统一经过 `OrderStateMachine.assertCanTransit` 校验（见 [OrderStateMachine.java](backend/src/main/java/com/daijia/order/service/OrderStateMachine.java)），非法流转直接拒绝。

### 计价规则
`起步价 + 超出里程费 + 超出时长费`，叠加**夜间加价系数**（仅作用于里程费与时长费）；预约类订单再叠加**预约加价系数**；最终不低于最低消费。
金额全程以「分」为整数参与运算，向上取整，避免浮点误差。

### 数据一致性设计（防脏数据）

| 机制 | 实现位置 | 作用 |
|------|----------|------|
| 状态机前置校验 | `OrderStateMachine` | 拒绝一切非法状态流转 |
| 条件更新（CAS） | `OrdersMapper.casStatus` | 只更新状态符合预期的行，影响行数为 0 即判定并发冲突 |
| 抢单原子绑定 | `OrdersMapper.grabOrder` | `WHERE status='PENDING_ACCEPT' AND provider_id IS NULL`，保证并发抢单只有一个赢家 |
| 乐观锁 | `orders.version` + `@Version` | 防止并发覆盖更新 |
| 幂等键 | `IdempotencyService` + `payment.idempotency_key` / `orders.order_no` 唯一索引 | 下单、支付、退款重复提交只生效一次 |
| 支付回调幂等 | `PaymentMapper.markPaid` 状态 CAS | 重复回调仅处理一次 |
| 金额守恒 | `PaymentServiceImpl` | 约束 `已退款 ≤ 已支付 ≤ 订单实际金额` |
| 状态审计 | `order_status_log` | 每次流转记录操作人、来源/目标状态与时间，可追溯 |
| 事务边界 | `@Transactional` | 状态变更与关联写库同事务，失败整体回滚 |

### Redis 的定位
Redis 在本项目中**只承担加速与削峰职责**（幂等预判、抢单防抖），不是正确性来源：

- `app.redis.enabled: true` 时正常使用；
- Redis 未安装、连接失败或超时，`RedisHelper` 会自动熔断 30 秒并**降级放行**；
- 此时订单状态与金额的正确性完全由数据库的唯一索引、条件更新与乐观锁保证，核心链路依然可用。

---

## 七、主要接口一览

| 模块 | 方法与路径 | 权限 |
|------|-----------|------|
| 认证 | `POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/me` | 前两个公开 |
| 客户端下单 | `POST /api/customer/orders/estimate`、`POST /api/customer/orders`、`GET /api/customer/orders`、`GET /api/customer/orders/{orderNo}`、`PUT /api/customer/orders/{orderNo}/cancel` | CUSTOMER |
| 服务商接单 | `GET /api/provider/orders/hall`、`POST /api/provider/orders/{orderNo}/grab`、`PUT /api/provider/orders/{orderNo}/start`、`PUT /api/provider/orders/{orderNo}/finish` | PROVIDER |
| 服务商档案 | `GET /api/provider/profile`、`PUT /api/provider/profile/online`、`PUT /api/provider/profile/accept` | PROVIDER |
| 支付 | `POST /api/payments/orders/{orderNo}/pay`、`GET /api/payments/orders/{orderNo}` | CUSTOMER / ADMIN |
| 支付回调 | `POST /api/public/payment/mock-callback` | 公开（模拟网关回调） |
| 管理后台 | `/api/admin/users`、`/api/admin/roles`、`/api/admin/permissions`、`/api/admin/providers`、`/api/admin/orders`、`/api/admin/pricing-rules` | ADMIN |

完整参数与响应结构见 Swagger UI。

---

## 八、已知限制与说明

1. **支付为模拟通道**：`channel = MOCK`，接口形态（统一下单 / 回调 / 退款 + 状态机）已对齐真实渠道，后续可替换为微信或支付宝实现。
2. **预约时间与里程为模拟值**：H5 端未接入地图与路径规划，里程/时长按起终点文本模拟生成，下单页已标注。
3. **管理后台部分能力未实现 UI**：如「新增用户」（后端未提供该接口），仅实现账号编辑、启停与角色分配。
4. **自动化测试未包含**：当前环境本地 Maven 仓库缺少 JUnit / Mockito / spring-boot-starter-test，未编写单元与集成测试，验收按功能路径手工执行。
5. **离线构建**：若本机 Maven 处于离线模式，`mvn compile` 可正常执行；`mvn package` / `mvn test` 需要联网下载 surefire 等构建插件。
6. **计价规则唯一性**：由服务层校验（同城市 + 同车型不可重复），数据库层面为普通索引（因为逻辑删除与唯一索引会互相冲突）。
