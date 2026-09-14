package com.daijia.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daijia.common.BusinessException;
import com.daijia.common.NoGenerator;
import com.daijia.common.PageResult;
import com.daijia.common.ResultCode;
import com.daijia.provider.dto.ProviderQuery;
import com.daijia.provider.dto.ProviderSaveRequest;
import com.daijia.provider.dto.ProviderVO;
import com.daijia.provider.entity.Provider;
import com.daijia.provider.mapper.ProviderMapper;
import com.daijia.provider.service.ProviderService;
import com.daijia.user.entity.SysUser;
import com.daijia.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务商档案服务实现
 */
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderMapper providerMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<ProviderVO> page(ProviderQuery query) {
        LambdaQueryWrapper<Provider> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(Provider::getRealName, keyword)
                    .or().like(Provider::getPhone, keyword)
                    .or().like(Provider::getProviderNo, keyword));
        }
        wrapper.eq(query.getStatus() != null, Provider::getStatus, query.getStatus());
        wrapper.eq(query.getAuditStatus() != null, Provider::getAuditStatus, query.getAuditStatus());
        wrapper.eq(StringUtils.hasText(query.getCityCode()), Provider::getCityCode, query.getCityCode());
        wrapper.orderByDesc(Provider::getId);

        Page<Provider> page = providerMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<ProviderVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Long create(ProviderSaveRequest request) {
        Provider provider = new Provider();
        provider.setUserId(request.getUserId());
        provider.setProviderNo(NoGenerator.providerNo());
        provider.setRealName(request.getRealName());
        provider.setPhone(request.getPhone());
        provider.setIdCardNo(request.getIdCardNo());
        provider.setServiceTypes(request.getServiceTypes());
        provider.setCityCode(request.getCityCode());
        provider.setVehiclePlate(request.getVehiclePlate());
        provider.setVehicleModel(request.getVehicleModel());
        provider.setDrivingYears(request.getDrivingYears() == null ? 0 : request.getDrivingYears());
        provider.setOnlineStatus(0);
        provider.setAcceptEnabled(0);
        provider.setAuditStatus(1);
        provider.setStatus(1);
        provider.setRemark(request.getRemark());
        providerMapper.insert(provider);
        return provider.getId();
    }

    @Override
    public void update(Long id, ProviderSaveRequest request) {
        requireProvider(id);
        Provider update = new Provider();
        update.setId(id);
        update.setRealName(request.getRealName());
        update.setPhone(request.getPhone());
        update.setIdCardNo(request.getIdCardNo());
        update.setServiceTypes(request.getServiceTypes());
        update.setCityCode(request.getCityCode());
        update.setVehiclePlate(request.getVehiclePlate());
        update.setVehicleModel(request.getVehicleModel());
        update.setDrivingYears(request.getDrivingYears());
        update.setRemark(request.getRemark());
        providerMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        assertStatus(status);
        requireProvider(id);
        Provider update = new Provider();
        update.setId(id);
        update.setStatus(status);
        providerMapper.updateById(update);
    }

    @Override
    public ProviderVO currentProfile(Long userId) {
        return toVO(requireProviderByUserId(userId));
    }

    @Override
    public void updateOnlineStatus(Long userId, Integer onlineStatus) {
        assertStatus(onlineStatus);
        Provider provider = requireProviderByUserId(userId);
        Provider update = new Provider();
        update.setId(provider.getId());
        update.setOnlineStatus(onlineStatus);
        providerMapper.updateById(update);
    }

    @Override
    public void updateAcceptSwitch(Long userId, Integer acceptEnabled) {
        assertStatus(acceptEnabled);
        Provider provider = requireProviderByUserId(userId);
        Provider update = new Provider();
        update.setId(provider.getId());
        update.setAcceptEnabled(acceptEnabled);
        providerMapper.updateById(update);
    }

    private Provider requireProvider(Long id) {
        Provider provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException(ResultCode.PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    private Provider requireProviderByUserId(Long userId) {
        Provider provider = providerMapper.selectByUserId(userId);
        if (provider == null) {
            throw new BusinessException(ResultCode.PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    private void assertStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态值只能为 0 或 1");
        }
    }

    private ProviderVO toVO(Provider provider) {
        ProviderVO vo = new ProviderVO();
        vo.setId(provider.getId());
        vo.setUserId(provider.getUserId());
        vo.setProviderNo(provider.getProviderNo());
        vo.setRealName(provider.getRealName());
        vo.setPhone(provider.getPhone());
        vo.setIdCardNo(provider.getIdCardNo());
        vo.setServiceTypes(provider.getServiceTypes());
        vo.setServiceTypeList(splitServiceTypes(provider.getServiceTypes()));
        vo.setCityCode(provider.getCityCode());
        vo.setVehiclePlate(provider.getVehiclePlate());
        vo.setVehicleModel(provider.getVehicleModel());
        vo.setDrivingYears(provider.getDrivingYears());
        vo.setRating(provider.getRating());
        vo.setOrderCount(provider.getOrderCount());
        vo.setOnlineStatus(provider.getOnlineStatus());
        vo.setAcceptEnabled(provider.getAcceptEnabled());
        vo.setAuditStatus(provider.getAuditStatus());
        vo.setStatus(provider.getStatus());
        vo.setRemark(provider.getRemark());
        vo.setCreateTime(provider.getCreateTime());

        if (provider.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(provider.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
        }
        return vo;
    }

    private List<String> splitServiceTypes(String serviceTypes) {
        if (!StringUtils.hasText(serviceTypes)) {
            return Collections.emptyList();
        }
        return Arrays.stream(serviceTypes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
