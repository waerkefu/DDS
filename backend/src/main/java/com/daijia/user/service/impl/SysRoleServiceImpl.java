package com.daijia.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;
import com.daijia.user.dto.PermissionVO;
import com.daijia.user.dto.RoleSaveRequest;
import com.daijia.user.dto.RoleVO;
import com.daijia.user.entity.SysPermission;
import com.daijia.user.entity.SysRole;
import com.daijia.user.entity.SysRolePermission;
import com.daijia.user.mapper.SysPermissionMapper;
import com.daijia.user.mapper.SysRoleMapper;
import com.daijia.user.mapper.SysRolePermissionMapper;
import com.daijia.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色与权限管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<RoleVO> listAll() {
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        return roles.stream().map(this::toRoleVO).collect(Collectors.toList());
    }

    @Override
    public Long create(RoleSaveRequest request) {
        if (!StringUtils.hasText(request.getRoleCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码不能为空");
        }
        Long exists = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        sysRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void update(Long id, RoleSaveRequest request) {
        SysRole exist = requireRole(id);
        if (StringUtils.hasText(request.getRoleCode())
                && !request.getRoleCode().equals(exist.getRoleCode())) {
            Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, request.getRoleCode()));
            if (count != null && count > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
            }
        }

        SysRole update = new SysRole();
        update.setId(id);
        if (StringUtils.hasText(request.getRoleCode())) {
            update.setRoleCode(request.getRoleCode());
        }
        update.setRoleName(request.getRoleName());
        update.setDescription(request.getDescription());
        sysRoleMapper.updateById(update);
    }

    @Override
    public List<PermissionVO> listPermissions() {
        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSortNo)
                .orderByAsc(SysPermission::getId));
        return permissions.stream().map(this::toPermissionVO).collect(Collectors.toList());
    }

    @Override
    public List<Long> listRolePermissionIds(Long roleId) {
        return sysPermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        requireRole(roleId);
        // 先清空原有权限，再批量写入
        sysRolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds == null) {
            return;
        }
        for (Long permissionId : permissionIds) {
            if (permissionId == null) {
                continue;
            }
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            sysRolePermissionMapper.insert(rolePermission);
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        return vo;
    }

    private PermissionVO toPermissionVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setPermCode(permission.getPermCode());
        vo.setPermName(permission.getPermName());
        vo.setPermType(permission.getPermType());
        vo.setPath(permission.getPath());
        vo.setComponent(permission.getComponent());
        vo.setIcon(permission.getIcon());
        vo.setSortNo(permission.getSortNo());
        return vo;
    }
}
