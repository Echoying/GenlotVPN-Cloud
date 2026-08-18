package com.ruoyi.vpn.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import com.ruoyi.vpn.auth.tcp.TcpRpcDispatcher;
import com.ruoyi.vpn.auth.tcp.VpnTcpRateLimitService;
import com.ruoyi.vpn.auth.util.FeedbackImageRules;
import com.ruoyi.vpn.auth.util.InMemoryMultipartFile;
import com.ruoyi.vpn.protocol.SubmitFeedbackRequest;
import com.ruoyi.vpn.protocol.SubmitFeedbackResponse;
import com.ruoyi.vpn.protocol.UploadFeedbackImageRequest;
import com.ruoyi.vpn.protocol.UploadFeedbackImageResponse;
import com.ruoyi.yianlian.api.RemoteVpnFeedbackService;
import com.ruoyi.yianlian.api.domain.VpnFeedbackCreateDTO;

/**
 * VPN 问题反馈 TCP RPC：上传截图、提交工单。
 */
@Service
public class VpnFeedbackRpcService
{
    private static final Logger log = LoggerFactory.getLogger(VpnFeedbackRpcService.class);

    private static final String REDIS_IMG_PREFIX = "vpn_feedback_img:";

    private static final long IMAGE_TTL_SECONDS = 7200L;

    private static final int MAX_IMAGE_IDS = 3;

    private static final int MAX_TITLE_LENGTH = 80;

    private static final int MAX_CONTENT_LENGTH = 2000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private VpnTcpRateLimitService vpnTcpRateLimitService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Autowired
    private RemoteVpnFeedbackService remoteVpnFeedbackService;

    @Autowired
    private RedisService redisService;

    public TcpRpcDispatcher.RpcResult upload(UploadFeedbackImageRequest req, String clientIp)
    {
        if (!vpnTcpRateLimitService.tryAcquireFeedbackUpload(clientIp))
        {
            return TcpRpcDispatcher.RpcResult.fail("上传过于频繁，请稍后再试");
        }
        byte[] image = req.getImage() == null ? new byte[0] : req.getImage().toByteArray();
        FeedbackImageRules.assertUpload(image);
        String detected = FeedbackImageRules.detectContentType(image);
        boolean png = "image/png".equals(detected);
        String filename = png ? "feedback.png" : "feedback.jpg";
        InMemoryMultipartFile file = new InMemoryMultipartFile(filename, detected, image);
        R<SysFile> result;
        try
        {
            result = remoteFileService.upload(file);
        }
        catch (Exception e)
        {
            log.warn("反馈截图上传失败: {}", e.getMessage());
            throw new ServiceException("截图上传失败，请重试");
        }
        if (result == null || R.FAIL == result.getCode() || result.getData() == null
                || StringUtils.isEmpty(result.getData().getUrl()))
        {
            throw new ServiceException("截图上传失败，请重试");
        }
        String imageId = UUID.randomUUID().toString();
        redisService.setCacheObject(REDIS_IMG_PREFIX + imageId, result.getData().getUrl(),
                IMAGE_TTL_SECONDS, TimeUnit.SECONDS);
        return TcpRpcDispatcher.RpcResult.ok(UploadFeedbackImageResponse.newBuilder()
                .setImageId(imageId)
                .build());
    }

    /**
     * @param userId 已登录为 token 用户 id；游客为 null
     * @param userName 已登录为 token 用户名（忽略请求体账号）；游客为请求体 user_name
     */
    public TcpRpcDispatcher.RpcResult submit(SubmitFeedbackRequest req, Long userId, String userName,
            String clientIp)
    {
        if (!vpnTcpRateLimitService.tryAcquireFeedbackSubmit(clientIp))
        {
            return TcpRpcDispatcher.RpcResult.fail("提交过于频繁，请稍后再试");
        }
        if (StringUtils.isEmpty(userName))
        {
            throw new ServiceException("请填写账号");
        }
        assertTitle(req.getTitle());
        assertContent(req.getContent());
        if (req.getImageIdsCount() > MAX_IMAGE_IDS)
        {
            throw new ServiceException("截图最多 3 张");
        }
        List<String> urls = new ArrayList<String>(req.getImageIdsCount());
        for (String imageId : req.getImageIdsList())
        {
            String url = redisService.getCacheObject(REDIS_IMG_PREFIX + imageId);
            if (StringUtils.isEmpty(url))
            {
                throw new ServiceException("截图已失效，请重新添加");
            }
            urls.add(url);
        }
        VpnFeedbackCreateDTO dto = new VpnFeedbackCreateDTO();
        dto.setTitle(req.getTitle());
        dto.setContent(req.getContent());
        dto.setCategory(req.getCategory());
        dto.setUserId(userId);
        dto.setUserName(userName);
        dto.setClientVersion(req.getClientVersion());
        dto.setClientPlatform(req.getClientPlatform());
        dto.setIpaddr(clientIp);
        dto.setImageUrls(toJsonArray(urls));
        R<Long> result;
        try
        {
            result = remoteVpnFeedbackService.create(dto, SecurityConstants.INNER);
        }
        catch (Exception e)
        {
            log.warn("反馈提交 Feign 失败: {}", e.getMessage());
            throw new ServiceException(StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "提交失败");
        }
        if (result == null || R.FAIL == result.getCode() || result.getData() == null)
        {
            String msg = result != null && StringUtils.isNotEmpty(result.getMsg()) ? result.getMsg() : "提交失败";
            throw new ServiceException(msg);
        }
        return TcpRpcDispatcher.RpcResult.ok(SubmitFeedbackResponse.newBuilder()
                .setFeedbackId(result.getData().longValue())
                .build());
    }

    private static void assertTitle(String title)
    {
        if (title == null || title.trim().isEmpty())
        {
            throw new ServiceException("请填写标题");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH)
        {
            throw new ServiceException("标题过长");
        }
    }

    private static void assertContent(String content)
    {
        if (content == null || content.trim().isEmpty())
        {
            throw new ServiceException("请填写描述");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH)
        {
            throw new ServiceException("描述过长");
        }
    }

    private static String toJsonArray(List<String> urls)
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(urls);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("截图数据无效");
        }
    }
}
