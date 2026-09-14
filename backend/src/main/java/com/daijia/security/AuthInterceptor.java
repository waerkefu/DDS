package com.daijia.security;

import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;
import com.daijia.user.entity.SysUser;
import com.daijia.user.mapper.SysPermissionMapper;
import com.daijia.user.mapper.SysRoleMapper;
import com.daijia.user.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 认证与鉴权拦截器：
 * 1. 解析并校验 JWT；
 * 2. 校验账号是否存在且未被禁用（保证禁用账号即时生效）；
 * 3. 按 {@link RequireRole} 注解做角色校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Claims claims = jwtUtil.parseToken(token);
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setUserType(user.getUserType());

        Set<String> roles = new HashSet<>(sysRoleMapper.selectRoleCodesByUserId(userId));
        loginUser.setRoles(roles);
        loginUser.setPermissions(new HashSet<>(sysPermissionMapper.selectPermCodesByUserId(userId)));

        RequireRole requireRole = resolveAnnotation(handlerMethod, RequireRole.class);
        if (requireRole != null && !matchRole(roles, requireRole)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String prefix = jwtProperties.getPrefix();
        if (StringUtils.hasText(prefix) && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }

    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(HandlerMethod handlerMethod, Class<A> type) {
        A annotation = handlerMethod.getMethodAnnotation(type);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(type);
    }

    private boolean matchRole(Set<String> roles, RequireRole requireRole) {
        if (requireRole.value().length == 0) {
            return true;
        }
        if (requireRole.requireAll()) {
            return roles.containsAll(Arrays.asList(requireRole.value()));
        }
        return Arrays.stream(requireRole.value()).anyMatch(roles::contains);
    }
}
