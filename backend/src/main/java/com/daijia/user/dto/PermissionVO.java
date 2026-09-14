package com.daijia.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限信息
 */
@Data
public class PermissionVO implements Serializable {

    private Long id;

    private Long parentId;

    private String permCode;

    private String permName;

    /** 1=目录 2=菜单 3=按钮/接口 */
    private Integer permType;

    private String path;

    private String component;

    private String icon;

    private Integer sortNo;
}
