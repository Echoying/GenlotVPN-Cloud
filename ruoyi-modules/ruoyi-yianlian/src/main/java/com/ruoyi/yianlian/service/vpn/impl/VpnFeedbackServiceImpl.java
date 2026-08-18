package com.ruoyi.yianlian.service.vpn.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.api.domain.VpnFeedbackCreateDTO;
import com.ruoyi.yianlian.domain.VpnFeedback;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.mapper.VpnFeedbackMapper;
import com.ruoyi.yianlian.service.vpn.FeedbackRules;
import com.ruoyi.yianlian.service.vpn.IVpnFeedbackService;
import com.ruoyi.yianlian.service.vpn.IVpnUserService;

/**
 * VPN 问题反馈 服务实现
 *
 * @author ruoyi
 */
@Service
public class VpnFeedbackServiceImpl implements IVpnFeedbackService
{
    private static final String EMPTY_IMAGE_URLS = "[]";

    private static final int MAX_IMAGE_COUNT = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VpnFeedbackMapper feedbackMapper;

    @Autowired
    private IVpnUserService userService;

    @Override
    public List<VpnFeedback> selectFeedbackList(VpnFeedback feedback)
    {
        return feedbackMapper.selectFeedbackList(feedback);
    }

    @Override
    public VpnFeedback selectFeedbackById(Long id)
    {
        return feedbackMapper.selectFeedbackById(id);
    }

    @Override
    public int updateStatus(Long id, String status)
    {
        if (!FeedbackRules.isValidStatus(status))
        {
            throw new ServiceException("状态不正确");
        }
        return feedbackMapper.updateStatus(id, status);
    }

    @Override
    public Long create(VpnFeedbackCreateDTO dto)
    {
        if (dto == null || StringUtils.isBlank(dto.getUserName()))
        {
            throw new ServiceException("请填写账号");
        }
        Long userId = dto.getUserId();
        if (userId == null)
        {
            // 同名可出现在多条线路；只有唯一对上才回填，对不上也落库
            List<VpnUser> matched = userService.selectUserListByUserName(dto.getUserName().trim());
            if (matched != null && matched.size() == 1)
            {
                userId = matched.get(0).getUserId();
            }
        }
        FeedbackRules.assertTitle(dto.getTitle());
        FeedbackRules.assertContent(dto.getContent());

        VpnFeedback feedback = new VpnFeedback();
        feedback.setTitle(dto.getTitle());
        feedback.setContent(dto.getContent());
        feedback.setCategory(FeedbackRules.normalizeCategory(dto.getCategory()));
        feedback.setStatus("0");
        feedback.setUserId(userId);
        feedback.setUserName(dto.getUserName().trim());
        feedback.setClientVersion(dto.getClientVersion());
        feedback.setClientPlatform(dto.getClientPlatform());
        feedback.setIpaddr(dto.getIpaddr());
        feedback.setImageUrls(normalizeImageUrls(dto.getImageUrls()));
        feedback.setCreateTime(DateUtils.getNowDate());
        feedbackMapper.insertFeedback(feedback);
        return feedback.getId();
    }

    /**
     * null/空白 → []；否则必须是 JSON 数组且长度 ≤3。
     */
    private String normalizeImageUrls(String imageUrls)
    {
        if (StringUtils.isBlank(imageUrls))
        {
            return EMPTY_IMAGE_URLS;
        }
        try
        {
            JsonNode node = objectMapper.readTree(imageUrls.trim());
            if (!node.isArray() || node.size() > MAX_IMAGE_COUNT)
            {
                throw new ServiceException("截图最多 3 张");
            }
            return objectMapper.writeValueAsString(node);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("截图最多 3 张");
        }
    }
}
