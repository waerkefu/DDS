package com.daijia.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户车辆
 */
@Data
@TableName("vehicle")
public class Vehicle implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String plateNo;

    /** 车型编码，对应计价规则 vehicle_type */
    private String vehicleType;

    private String vehicleModel;

    private String color;

    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
