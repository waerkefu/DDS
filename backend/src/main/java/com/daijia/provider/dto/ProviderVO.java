package com.daijia.provider.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务商档案信息
 */
@Data
public class ProviderVO implements Serializable {

    private Long id;

    private Long userId;

    private String providerNo;

    private String realName;

    private String phone;

    private String idCardNo;

    /** 可服务类型，逗号分隔 */
    private String serviceTypes;

    /** 可服务类型列表 */
    private List<String> serviceTypeList;

    private String cityCode;

    private String vehiclePlate;

    private String vehicleModel;

    private Integer drivingYears;

    private BigDecimal rating;

    private Integer orderCount;

    /** 0=离线 1=在线 */
    private Integer onlineStatus;

    /** 0=停止接单 1=接单中 */
    private Integer acceptEnabled;

    /** 0=待审核 1=通过 2=驳回 */
    private Integer auditStatus;

    /** 1=启用 0=禁用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    /** 关联账号 */
    private String username;
}
