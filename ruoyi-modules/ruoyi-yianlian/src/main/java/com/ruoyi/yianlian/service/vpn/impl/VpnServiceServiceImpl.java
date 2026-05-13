package com.ruoyi.yianlian.service.vpn.impl;

import com.alibaba.fastjson.JSON;
import com.ruoyi.yianlian.client.dto.vo.ReqCsServerVO;
import com.ruoyi.yianlian.domain.VpnService;
import com.ruoyi.yianlian.mapper.VpnServiceMapper;
import com.ruoyi.yianlian.service.vpn.IVpnServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class VpnServiceServiceImpl implements IVpnServiceService
{
    @Autowired
    private VpnServiceMapper serviceMapper;

    @Override
    public List<VpnService> selectServiceList(VpnService service)
    {
        List<VpnService> list = serviceMapper.selectServiceList(service);
        list.forEach(this::deserializeCsServerVos);
        return list;
    }

    @Override
    public VpnService selectServiceById(Long id)
    {
        VpnService service = serviceMapper.selectServiceById(id);
        if (service != null)
        {
            deserializeCsServerVos(service);
        }
        return service;
    }

    @Override
    public int insertService(VpnService service)
    {
        serializeCsServerVos(service);
        return serviceMapper.insertService(service);
    }

    @Override
    public int updateService(VpnService service)
    {
        serializeCsServerVos(service);
        return serviceMapper.updateService(service);
    }

    @Override
    public int deleteServiceById(Long id)
    {
        return serviceMapper.deleteServiceById(id);
    }

    @Override
    public int deleteServiceByIds(Long[] ids)
    {
        return serviceMapper.deleteServiceByIds(ids);
    }

    private void serializeCsServerVos(VpnService service)
    {
        if (service.getReqCsServerVos() != null && !service.getReqCsServerVos().isEmpty())
        {
            service.setReqCsServerVosJson(JSON.toJSONString(service.getReqCsServerVos()));
        }
    }

    private void deserializeCsServerVos(VpnService service)
    {
        if (StringUtils.hasText(service.getReqCsServerVosJson()))
        {
            service.setReqCsServerVos(
                JSON.parseArray(service.getReqCsServerVosJson(), ReqCsServerVO.class)
            );
        }
    }
}
