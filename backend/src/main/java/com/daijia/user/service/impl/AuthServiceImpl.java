package com.daijia.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;
import com.daijia.security.JwtUtil;
import com.daijia.security.PasswordEncoder;
import com.daijia.user.dto.CurrentUserVO;
import com.daijia.user.dto.LoginRequest;
import com.daijia.user.dto.LoginVO;
import com.daijia.user.dto.RegisterRequest;
import com.daijia.user.dto.UserVO;
import com.daijia.user.entity.SysRole;
import com.daijia.user.entity.SysUser;
import com.daijia.user.entity.SysUserRole;
import com.daijia.user.enums.UserType;
import com.daijia.user.mapper.SysPermissionMapper;
import com.daijia.user.mapper.SysRoleMapper;
import com.daijia.user.mapper.SysUserMapper;
import com.daijia.user.mapper.SysUserRoleMapper;
import com.daijia.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 注册用户的默认角色编码 */
    private static final String DEFAULT_ROLE_CODE = "CUSTOMER";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginRequest request) {
        String account = request.getUsername();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .and(w -> w.eq(SysUser::getUsername, account).or().eq(SysUser::getPhone, account)));
        SysUser user = users.isEmpty() ? null : users.get(0);

        if (user == null || !PasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.ACCOUNT_PASSWORD_ERROR);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(now);
        sysUserMapper.updateById(update);
        user.setLastLoginTime(now);

        return buildLoginVO(user);
    }

    @Override
    public LoginVO register(RegisterRequest request) {
        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .and(w -> w.eq(SysUser::getUsername, request.getUsername())
                        .or().eq(SysUser::getPhone, request.getPhone())));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }

        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "默认客户角色未配置");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(PasswordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setUserType(UserType.CUSTOMER.getCode());
        user.setStatus(1);
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);

        return buildLoginVO(user);
    }

    @Override
    public CurrentUserVO currentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setUserType(user.getUserType());
        vo.setRoles(sysRoleMapper.selectRoleCodesByUserId(userId));
        vo.setPermissions(sysPermissionMapper.selectPermCodesByUserId(userId));
        return vo;
    }

    private LoginVO buildLoginVO(SysUser user) {
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getId());

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setPhone(user.getPhone());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setUserType(user.getUserType());
        UserType userType = UserType.of(user.getUserType());
        userVO.setUserTypeDesc(userType == null ? null : userType.getDescription());
        userVO.setStatus(user.getStatus());
        userVO.setLastLoginTime(user.getLastLoginTime());
        userVO.setCreateTime(user.getCreateTime());
        userVO.setRoles(roles == null ? new ArrayList<>() : roles);

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType()));
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setUser(userVO);
        return vo;
    }
}
