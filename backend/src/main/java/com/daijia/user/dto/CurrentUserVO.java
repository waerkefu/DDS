package com.daijia.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息（含角色与权限编码）
 */
@Data
public class CurrentUserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    /** 1=客户 2=服务商 3=管理员 */
    private Integer userType;

    /** 角色编码集合 */
    private List<String> roles;

    /** 权限编码集合 */
    private List<String> permissions;
}
