package com.daijia.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daijia.common.BusinessException;
import com.daijia.common.PageResult;
import com.daijia.common.ResultCode;
import com.daijia.user.dto.UserQuery;
import com.daijia.user.dto.UserUpdateRequest;
import com.daijia.user.dto.UserVO;
import com.daijia.user.entity.SysUser;
import com.daijia.user.entity.SysUserRole;
import com.daijia.user.enums.UserType;
import com.daijia.user.mapper.SysRoleMapper;
import com.daijia.user.mapper.SysUserMapper;
import com.daijia.user.mapper.SysUserRoleMapper;
import com.daijia.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getPhone, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        wrapper.eq(query.getUserType() != null, SysUser::getUserType, query.getUserType());
        wrapper.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void update(Long id, UserUpdateRequest request) {
        requireUser(id);
        SysUser update = new SysUser();
        update.setId(id);
        update.setNickname(request.getNickname());
        update.setPhone(request.getPhone());
        update.setRemark(request.getRemark());
        sysUserMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        assertStatus(status);
        requireUser(id);
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        sysUserMapper.updateById(update);
    }

    @Override
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        // 先清空原有关联，再批量写入
        sysUserRoleMapper.deleteByUserId(id);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(id);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    private void assertStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态值只能为 0 或 1");
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setUserType(user.getUserType());
        UserType userType = UserType.of(user.getUserType());
        vo.setUserTypeDesc(userType == null ? null : userType.getDescription());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        vo.setRoles(roles == null ? new ArrayList<>() : roles);
        return vo;
    }
}
