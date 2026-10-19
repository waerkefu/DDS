# 代驾代办 H5 服务平台 —— 代码浏览说明书

> 按 `tasks.md` 的 Task 1–13 顺序组织。每一节对应一个可独立阅读的功能模块。
> 每个功能点包含：**做什么**、**入口文件**、**调用链路**、**关键设计**。

---

# 第一部分 · 后端基础设施（Task 1–3）

## 模块 1：工程骨架与统一响应（Task 1）

### 做什么
建立 Spring Boot 3 工程底座：统一 JSON 响应体、全局异常处理、分页参数、单号生成器。

### 关键文件

| 文件 | 职责 |
|---|---|
| [Result.java](backend/src/main/java/com/daijia/common/Result.java) | 统一响应 `{code, message, data}` |
| [ResultCode.java](backend/src/main/java/com/daijia/common/ResultCode.java) | 全部业务错误码枚举（1xxx 账号 / 2xxx 订单 / 3xxx 计价 / 4xxx 支付） |
| [BusinessException.java](backend/src/main/java/com/daijia/common/BusinessException.java) | 业务异常，携带 `code` |
| [GlobalExceptionHandler.java](backend/src/main/java/com/daijia/common/GlobalExceptionHandler.java) | `@RestControllerAdvice` 兜底所有异常，转为 `Result.fail` |
| [PageQuery.java](backend/src/main/java/com/daijia/common/PageQuery.java) / [PageResult.java](backend/src/main/java/com/daijia/common/PageResult.java) | 分页输入 / 输出 |
| [NoGenerator.java](backend/src/main/java/com/daijia/common/NoGenerator.java) | 业务单号：`DO`(订单)/`PY`(支付)/`RF`(退款)/`SP`(服务商) + 时间戳 + 随机数 |
| [MybatisPlusConfig.java](backend/src/main/java/com/daijia/config/MybatisPlusConfig.java) | 注册乐观锁插件 + 分页插件 |
| [MyMetaObjectHandler.java](backend/src/main/java/com/daijia/config/MyMetaObjectHandler.java) | 自动填充 `createTime` / `updateTime` |

### 调用链路
```
Controller 抛 BusinessException
  → GlobalExceptionHandler.handleBusinessException()
  → Result.fail(code, message)
  → 前端 axios 拦截器按 code 分支处理
```

### 关键设计
- 金额全程用 `INT` 存**分**，避免浮点误差；
- `PageQuery.safePageSize()` 上限 100，防止恶意大分页。

---

## 模块 2：数据库（Task 2）

### 做什么
13 张表的 DDL + 初始化数据。

### 关键文件
- [init.sql](docs/db/init.sql)

### 表关系速览
```
sys_user（系统用户）  sys_user_role（系统用户角色关联） sys_role（系统角色）  sys_role_permission（角色权限关联） sys_permission（系统权限） vehicle（车辆）
address（地址） provider（服务商）  order（订单） order_status_log（订单状态日志）
payment（支付） refund（退款）  pricing_rule（计价规则）

sys_user ──< sys_user_role >── sys_role ──< sys_role_permission >── sys_permission
sys_user ──< vehicle / address
sys_user ──< provider（一对一，user_id 唯一）
provider ──< orders
sys_user(customer) ──< orders ──< order_status_log
orders ──< payment ──< refund
pricing_rule（独立）
```

### 关键设计
payment（支付）
refund（退款）
- `orders.order_no` 唯一索引 → 防重复下单兜底；
- `orders.version` + MyBatis-Plus `@Version` → 乐观锁；
- `payment.idempotency_key` / `refund.idempotency_key` 唯一索引 → 幂等兜底；
- 表名用 `orders` 规避 MySQL 保留字 `ORDER`。

---

## 模块 3：认证与 RBAC（Task 3）

### 做什么
JWT 登录签发 / 校验、接口级角色控制（替代 Spring Security）。

### 关键文件

| 文件 | 职责 |
|---|---|
| [JwtUtil.java](backend/src/main/java/com/daijia/security/JwtUtil.java) | 签发 / 解析 JWT（HMAC-SHA256） |
| [JwtProperties.java](backend/src/main/java/com/daijia/security/JwtProperties.java) | 密钥、有效期、请求头配置 |
| [PasswordEncoder.java](backend/src/main/java/com/daijia/security/PasswordEncoder.java) | PBKDF2WithHmacSHA256 加密（格式：`pbkdf2$迭代次数$盐$摘要`） |
| [AuthInterceptor.java](backend/src/main/java/com/daijia/security/AuthInterceptor.java) | 拦截器：解析 Token → 查用户 → 查角色/权限 → `@RequireRole` 校验 |
| [RequireRole.java](backend/src/main/java/com/daijia/security/RequireRole.java) | 注解，标在 Controller 类或方法上 |
| [LoginUser.java](backend/src/main/java/com/daijia/security/LoginUser.java) / [UserContext.java](backend/src/main/java/com/daijia/security/UserContext.java) | 当前登录用户（ThreadLocal） |
| [WebConfig.java](backend/src/main/java/com/daijia/config/WebConfig.java) | 注册拦截器到 `/api/**`，白名单放行登录/注册/Swagger |

### 调用链路（登录）
```
POST /api/auth/login
  → AuthController.login()
  → AuthServiceImpl.login()
    → 按 username 或 phone 查 sys_user
    → PasswordEncoder.matches() 校验密码
    → JwtUtil.generateToken(userId, username, userType)
    → 返回 { token, user: { roles, permissions } }
```

### 调用链路（鉴权）
```
任何 /api/** 请求
  → AuthInterceptor.preHandle()
    → 解析 Authorization: Bearer <token>
    → JwtUtil.parseToken() → 得 userId
    → 查 sys_user（检查账号存在且未禁用）
    → 查 sys_user_role → sys_role 得角色码集合
    → 查 sys_role_permission → sys_permission 得权限码集合
    → 若 Controller 标了 @RequireRole("ADMIN") 而用户无该角色 → 抛 403
    → UserContext.set(loginUser)  → 后续业务代码可随时取当前用户
```

### 关键设计
- **禁用即时生效**：每次请求都查库校验 `status=1`，不只是登录时检查；
- `@RequireRole` 支持类级 + 方法级，方法级优先；
- 密码算法为 PBKDF2 而非 BCrypt（本地 Maven 仓库缺依赖），`init.sql` 中的演示密文已同步为该算法。

---

# 第二部分 · 核心业务后端（Task 4–9）

## 模块 4：用户与服务商管理（Task 4）

### 做什么
客户注册、用户分页/编辑/启停、服务商档案 CRUD、接单开关。

### 关键文件

| 层 | 文件 |
|---|---|
| Controller | [AdminUserController.java](backend/src/main/java/com/daijia/user/controller/AdminUserController.java) `/api/admin/users` —— `@RequireRole("ADMIN")` |
| Controller | [AdminProviderController.java](backend/src/main/java/com/daijia/provider/controller/AdminProviderController.java) `/api/admin/providers` |
| Controller | [ProviderProfileController.java](backend/src/main/java/com/daijia/provider/controller/ProviderProfileController.java) `/api/provider/profile` —— 服务商自己操作 |
| Service | [SysUserServiceImpl.java](backend/src/main/java/com/daijia/user/service/impl/SysUserServiceImpl.java) |
| Service | [ProviderServiceImpl.java](backend/src/main/java/com/daijia/provider/service/impl/ProviderServiceImpl.java) |
| Entity | [SysUser.java](backend/src/main/java/com/daijia/user/entity/SysUser.java) / [Provider.java](backend/src/main/java/com/daijia/provider/entity/Provider.java) |

### 服务商接单资格判定（Provider.canAcceptOrder 隐含逻辑）
```
onlineStatus == 1 && acceptEnabled == 1 && status == 1 && auditStatus == 1
```
四个条件缺一不可。接单大厅和抢单接口都会先走这个校验。

---

## 模块 5：计价引擎（Task 5）

### 做什么
按「城市 + 车型」查规则，算出订单金额。

### 关键文件
- [PricingServiceImpl.java](backend/src/main/java/com/daijia/pricing/service/impl/PricingServiceImpl.java) —— 核心算法
- [AdminPricingController.java](backend/src/main/java/com/daijia/pricing/controller/AdminPricingController.java) —— 规则 CRUD（管理端）

### 计算公式（doCalculate 方法）
```
超里程费 = ceil(max(0, distanceM - baseDistanceM) × pricePerKm / 1000)
超时长费 = ceil(max(0, durationS - baseDurationS) × pricePerMin / 60)

夜间系数：服务时间落在 nightStart~nightEnd（支持跨零点）→ 对(里程费+时长费)乘 nightRatio
预约系数：仅 SCHEDULED 类型 → 对(起步价+变动费)乘 bookingRatio

total = max(basePrice + 变动费 × 夜间系数, minPrice) × 预约系数
```

### 关键设计
- 所有金额以**分**为整数运算，乘系数后用 `BigDecimal.setScale(0, CEILING)` 向上取整；
- `isNight()` 支持跨零点区间（如 22:00~06:00）；
- 规则唯一性由**服务层** `assertNotExists()` 保证，不用数据库唯一索引（因为逻辑删除字段冲突）。

---

## 模块 6：订单状态机与一致性（Task 6）

### 做什么
定义合法状态流转表，配合 CAS 更新防止并发脏写。

### 关键文件
- [OrderStateMachine.java](backend/src/main/java/com/daijia/order/service/OrderStateMachine.java) —— 流转表
- [OrdersMapper.java](backend/src/main/java/com/daijia/order/mapper/OrdersMapper.java) —— CAS SQL
- [OrderStatus.java](backend/src/main/java/com/daijia/order/enums/OrderStatus.java) —— 7 个状态枚举

### 状态流转图
```
PENDING_ACCEPT ──► ACCEPTED ──► IN_SERVICE ──► PENDING_PAYMENT ──► COMPLETED
     │                │                              │
     ├──► CANCELLED   ├──► CANCELLED                 └──► CANCELLED
     └──► CLOSED（超时）
```
终态（COMPLETED / CANCELLED / CLOSED）不可再流转。

### CAS 更新 SQL（OrdersMapper）
```sql
-- 通用状态流转
UPDATE orders SET status=?, version=version+1
WHERE id=? AND status=? AND deleted=0

-- 抢单（额外要求未绑定服务商）
UPDATE orders SET status=?, provider_id=?, version=version+1
WHERE id=? AND status=? AND provider_id IS NULL AND deleted=0

-- 超时关闭（定时任务批量执行）
UPDATE orders SET status='CLOSED', version=version+1
WHERE status='PENDING_ACCEPT' AND expire_time < NOW() AND deleted=0
```

### 关键设计
- **状态机前置校验 + CAS 更新 + 影响行数判断**：三步缺一不可；
- `rows == 0` 说明状态已被其他线程改过，抛 `ORDER_VERSION_CONFLICT`；
- Redis 只用于抢单短时防重（5 秒），正确性由数据库 CAS 兜底；
- 每次流转都写 `order_status_log` 审计日志。

---

## 模块 7：下单接口（Task 7）

### 做什么
客户创建订单，服务端计价，返回订单号与预估金额。

### 关键文件
- [CustomerOrderController.java](backend/src/main/java/com/daijia/order/controller/CustomerOrderController.java) `/api/customer/orders`
- [OrderServiceImpl.java](backend/src/main/java/com/daijia/order/service/impl/OrderServiceImpl.java) —— `create()` 方法

### 调用链路
```
POST /api/customer/orders  (Header: X-Idempotency-Key)
  → CustomerOrderController.create()
  → OrderServiceImpl.create(customerId, req, idempotencyKey)
    ① validateBookingTime()          —— 预约时间必须晚于现在且 ≤ 7 天
    ② 起终点不能相同
    ③ idempotencyService.acquireOrThrow()  —— Redis 幂等占位 5 分钟
    ④ pricingService.calculateAmount()     —— 服务端计价（不信任前端金额）
    ⑤ NoGenerator.orderNo()               —— 生成 DO 开头订单号
    ⑥ ordersMapper.insert()               —— DuplicateKeyException → 重复提交
    ⑦ saveStatusLog()                     —— 写入首条状态日志（null → PENDING_ACCEPT）
```

### 幂等三层防护
1. 前端生成随机 `X-Idempotency-Key`；
2. Redis `SETNX` 占位（5 分钟）；
3. `orders.order_no` 唯一索引兜底，重复插入转 `DUPLICATE_REQUEST`。

---

## 模块 8：服务商接单与履约（Task 8）

### 做什么
接单大厅列表、抢单、开始服务、结束服务、客户/管理员取消订单。

### 关键文件
- [ProviderOrderController.java](backend/src/main/java/com/daijia/order/controller/ProviderOrderController.java) `/api/provider/orders`
- [OrderServiceImpl.java](backend/src/main/java/com/daijia/order/service/impl/OrderServiceImpl.java) —— `grab()` / `startService()` / `finishService()`
- [OrderTimeoutTask.java](backend/src/main/java/com/daijia/order/task/OrderTimeoutTask.java) —— 每分钟扫超时订单

### 抢单链路（grab 方法）
```
POST /api/provider/orders/{orderNo}/grab
  → OrderServiceImpl.grab(providerUserId, orderNo)
    ① requireProvider() → 查服务商档案，校验 canAcceptOrder()
    ② idempotencyService.tryAcquire("grab:" + orderNo, 5s)  —— Redis 短时锁
    ③ 查订单，断言 status == PENDING_ACCEPT
    ④ orderStateMachine.assertCanTransit() —— 状态机校验
    ⑤ canServe() —— 服务类型匹配 + 城市匹配
    ⑥ ordersMapper.grabOrder() —— CAS：WHERE status='PENDING_ACCEPT' AND provider_id IS NULL
       rows == 0 → 已被抢，抛 ORDER_ALREADY_ACCEPTED
    ⑦ saveStatusLog() —— 写接单日志
    ⑧ finally: release Redis 锁
```

### 结束服务链路（finishService 方法）
```
PUT /api/provider/orders/{orderNo}/finish  { distanceM, durationS }
  → 状态机校验（IN_SERVICE → PENDING_PAYMENT）
  → pricingService.calculateAmount()  —— 按实际里程/时长重新结算
  → ordersMapper.casStatus()
  → 更新 actualAmount / distanceM / durationS / serviceEndTime
  → 写状态日志
```

### 超时关闭（OrderTimeoutTask）
```
@Scheduled(fixedDelay = 60_000)  —— 每分钟执行
  → OrdersMapper.closeExpiredOrders()
    UPDATE orders SET status='CLOSED'
    WHERE status='PENDING_ACCEPT' AND expire_time < NOW()
```

---

## 模块 9：模拟支付与退款（Task 9）

### 做什么
创建支付单、模拟支付回调、退款（金额守恒校验）。

### 关键文件
- [PaymentController.java](backend/src/main/java/com/daijia/payment/controller/PaymentController.java) `/api/customer/payments`
- [MockPaymentCallbackController.java](backend/src/main/java/com/daijia/payment/controller/MockPaymentCallbackController.java) `/api/public/payment/mock-callback` —— 白名单，无需登录
- [PaymentServiceImpl.java](backend/src/main/java/com/daijia/payment/service/impl/PaymentServiceImpl.java)

### 创建支付单链路
```
POST /api/customer/payments  (Header: X-Idempotency-Key)
  → PaymentServiceImpl.createPayment(customerId, orderNo, idempotencyKey)
    ① 校验订单归属（customerId 匹配）
    ② 断言 status == PENDING_PAYMENT
    ③ 查是否已有成功支付单 → 防重复支付
    ④ 幂等键查询 → 已有则直接返回已有支付单（幂等返回）
    ⑤ Redis 幂等占位 10 秒
    ⑥ 创建 Payment（status=PAYING），DuplicateKeyException → 返回已有支付单
    ⑦ 更新订单 payStatus = PAYING
```

### 支付回调链路（幂等处理）
```
POST /api/public/payment/mock-callback  { payNo, success, tradeNo }
  → PaymentServiceImpl.handleCallback()
    ① 按 payNo 查支付单
    ② success=false → 标记 FAILED，订单 payStatus 回退 UNPAID，直接返回
    ③ Redis 幂等占位 30 秒 → 重复回调直接返回
    ④ paymentMapper.markPaid() —— 状态 CAS：
       UPDATE payment SET status='SUCCESS', trade_no=?
       WHERE id=? AND status IN ('INIT','PAYING')
       rows == 0 → 已处理过，幂等返回
    ⑤ 状态机校验（PENDING_PAYMENT → COMPLETED）
    ⑥ 更新订单：payStatus=PAID, payAmount, finishTime
    ⑦ ordersMapper.casStatus() → 乐观锁兜底
    ⑧ 写状态日志「模拟支付成功」
```

### 退款金额守恒
```
已退款总额 + 本次退款 ≤ 已支付金额 ≤ 订单实际金额
```
违反约束抛 `REFUND_AMOUNT_EXCEED`。全部退完时支付单状态改为 `REFUNDED`。

---

# 第三部分 · 前端（Task 10–13）

## 模块 10：管理后台骨架（Task 10）

### 技术栈
Vue 3 + Vite + Pinia + Element Plus + Vue Router

### 关键文件

| 文件 | 职责 |
|---|---|
| [main.js](frontend/admin/src/main.js) | 入口：注册 Element Plus、Pinia、Router |
| [router/index.js](frontend/admin/src/router/index.js) | 路由 + 全局守卫（未登录跳 `/login`） |
| [stores/user.js](frontend/admin/src/stores/user.js) | 登录态（token 持久化到 localStorage） |
| [utils/request.js](frontend/admin/src/utils/request.js) | Axios 封装：注入 Token、统一 `{code,message,data}` 解析、401 自动跳登录 |
| [layout/index.vue](frontend/admin/src/layout/index.vue) | 侧边栏 + 顶栏布局，**按权限码动态过滤菜单** |

### 动态菜单逻辑（layout/index.vue）
```javascript
const menus = [
  { path: '/dashboard',       title: '首页',     perm: null },
  { path: '/system/user',     title: '用户管理', perm: 'system:user' },
  { path: '/system/provider', title: '服务商管理', perm: 'system:provider' },
  { path: '/system/role',     title: '角色权限', perm: 'system:role' },
  { path: '/order',           title: '订单管理', perm: 'order:list' },
  { path: '/pricing',         title: '计价规则', perm: 'pricing:rule' }
]
// 只显示用户拥有的权限码对应的菜单
visibleMenus = menus.filter(item => !item.perm || userStore.permissions.includes(item.perm))
```

### Axios 拦截器要点
- 请求拦截：自动注入 `Authorization: Bearer <token>`；
- 响应拦截：`code === 200` → 返回 `data`；`code === 401` → 清登录态 + 跳登录页；其他 → ElMessage 弹错误提示。

---

## 模块 11：管理后台业务页面（Task 11）

### 页面一览

| 页面 | 文件 | 对应后端接口 |
|---|---|---|
| 登录 | [views/login/index.vue](frontend/admin/src/views/login/index.vue) | `POST /api/auth/login` |
| 用户管理 | [views/system/user/index.vue](frontend/admin/src/views/system/user/index.vue) | `GET/PUT /api/admin/users` |
| 服务商管理 | [views/system/provider/index.vue](frontend/admin/src/views/system/provider/index.vue) | `GET/POST/PUT /api/admin/providers` |
| 角色权限 | [views/system/role/index.vue](frontend/admin/src/views/system/role/index.vue) | `GET/POST/PUT /api/admin/roles` |
| 订单管理 | [views/order/index.vue](frontend/admin/src/views/order/index.vue) | `GET /api/admin/orders` |
| 计价规则 | [views/pricing/index.vue](frontend/admin/src/views/pricing/index.vue) | `GET/POST/PUT/DELETE /api/admin/pricing-rules` |

---

## 模块 12：客户端 H5（Task 12）

### 技术栈
Vue 3 + Vite + Pinia + Element Plus（移动端适配）

### 关键文件

| 文件 | 职责 |
|---|---|
| [main.js](frontend/customer-h5/src/main.js) | 入口 |
| [utils/request.js](frontend/customer-h5/src/utils/request.js) | Axios 封装，token 存 localStorage |
| [api/order.js](frontend/customer-h5/src/api/order.js) | 下单相关 API |
| [api/payment.js](frontend/customer-h5/src/api/payment.js) | 支付相关 API |

### 页面与调用链

**下单页** [Home.vue](frontend/customer-h5/src/views/Home.vue)
```
选服务类型 → 填地址/车辆/联系人
  → 点「预估费用」→ POST /api/customer/orders/estimate → 展示费用明细
  → 点「立即下单」→ POST /api/customer/orders（带 X-Idempotency-Key）
  → 跳转订单详情
```
注意：必须先预估再下单（前端硬校验）。里程/时长为模拟值（按起终点文本长度生成伪随机数）。

**订单列表** [Orders.vue](frontend/customer-h5/src/views/Orders.vue)
```
状态筛选条 → GET /api/customer/orders?status=&pageNum=
点卡片 → 跳 /orders/{orderNo}
```

**订单详情** [OrderDetail.vue](frontend/customer-h5/src/views/OrderDetail.vue)
```
GET /api/customer/orders/{orderNo} → 展示状态时间线（order_status_log）
可操作：取消订单（PENDING_ACCEPT/ACCEPTED/PENDING_PAYMENT）
         去支付（PENDING_PAYMENT）→ POST /api/customer/payments → 跳模拟收银台
```

**支付结果页** [PayResult.vue](frontend/customer-h5/src/views/PayResult.vue)
```
模拟收银台确认 → POST /api/public/payment/mock-callback → 回订单详情
```

---

## 模块 13：服务商接单 H5（Task 13）

### 页面与调用链

**接单大厅** [Hall.vue](frontend/provider-h5/src/views/Hall.vue)
```
加载服务商档案 → GET /api/provider/profile
展示在线状态 / 接单开关
未开启接单 → 显示「一键开启」→ PUT /api/provider/profile/accept-enabled

每 15 秒轮询 → GET /api/provider/orders/hall（只显示 PENDING_ACCEPT 且服务类型匹配的订单）
点「立即接单」→ POST /api/provider/orders/{orderNo}/grab
  成功 → 跳订单详情
  失败（已被抢）→ 刷新大厅列表
```

**我的订单** [Orders.vue](frontend/provider-h5/src/views/Orders.vue)
```
GET /api/provider/orders?status= → 按状态筛选
点卡片 → 跳订单详情
```

**订单详情 / 履约** [OrderDetail.vue](frontend/provider-h5/src/views/OrderDetail.vue)
```
GET /api/provider/orders/{orderNo}
ACCEPTED  → 「开始服务」→ PUT /api/provider/orders/{orderNo}/start
IN_SERVICE → 「结束服务」→ 填实际里程/时长 → PUT /api/provider/orders/{orderNo}/finish
```

---

# 附录 A：完整订单生命周期调用链

```
客户 H5                后端                          服务商 H5
   │                     │                              │
   │── POST /orders ────►│ create()                     │
   │                     │  计价 + 落库 + 状态日志        │
   │                     │                              │
   │                     │◄── GET /orders/hall ─────────│ 轮询接单大厅
   │                     │                              │
   │                     │◄── POST /{no}/grab ──────────│ 抢单（CAS）
   │                     │  绑定服务商 + 状态→ACCEPTED    │
   │                     │                              │
   │                     │◄── PUT /{no}/start ──────────│ 开始服务
   │                     │  状态→IN_SERVICE              │
   │                     │                              │
   │                     │◄── PUT /{no}/finish ─────────│ 结束服务
   │                     │  重新计价 + 状态→PENDING_PAYMENT│
   │                     │                              │
   │── POST /payments ──►│ createPayment()              │
   │                     │  创建支付单                   │
   │                     │                              │
   │── POST /mock-callback►│ handleCallback()            │
   │                     │  支付CAS + 状态→COMPLETED     │
   │                     │                              │
```

# 附录 B：全局状态码与错误码速查

| 状态 | 说明 | 可操作方 |
|---|---|---|
| PENDING_ACCEPT | 待接单 | 客户取消 / 服务商抢单 / 系统超时关闭 |
| ACCEPTED | 已接单 | 服务商开始服务 / 客户取消 |
| IN_SERVICE | 服务中 | 服务商结束服务 |
| PENDING_PAYMENT | 待支付 | 客户支付 / 客户或管理员取消 |
| COMPLETED | 已完成 | 终态 |
| CANCELLED | 已取消 | 终态 |
| CLOSED | 超时关闭 | 终态 |

# 附录 C：技术偏差备忘

| 计划 | 实际 | 原因 |
|---|---|---|
| Spring Security | AuthInterceptor + @RequireRole | 本地 Maven 仓库缺依赖 |
| BCrypt | PBKDF2WithHmacSHA256 | 本地 Maven 仓库缺 spring-security-crypto |
| MapStruct | 手工对象转换 | 本地 Maven 仓库缺依赖 |
| JUnit 测试 | 无自动化测试 | 本地 Maven 仓库缺测试依赖 |
| order 表名 | orders | 规避 MySQL 保留字 ORDER |
