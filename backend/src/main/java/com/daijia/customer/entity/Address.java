package com.daijia.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户常用地址
 */
@Data
@TableName("address")
public class Address implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String contactName;

    private String contactPhone;

    /** 1=起点 2=终点 */
    private Integer addressType;

    private String cityCode;

    private String addressDetail;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
