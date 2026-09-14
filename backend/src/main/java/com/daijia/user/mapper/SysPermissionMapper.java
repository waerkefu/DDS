package com.daijia.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daijia.user.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户拥有的权限编码
     */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.permission_id = p.id "
            + "JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 1")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询角色已分配的权限 ID
     */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
