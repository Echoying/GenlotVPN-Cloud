package com.ruoyi.yianlian.service.vpn.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.yianlian.domain.LineApp;
import com.ruoyi.yianlian.domain.VpnLocalUser;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginDatPayload;
import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginExportRequest;
import com.ruoyi.yianlian.domain.vo.VpnOfflineLoginValidity;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.service.vpn.IVpnLineAppService;
import com.ruoyi.yianlian.service.vpn.IVpnLineAuthService;
import com.ruoyi.yianlian.service.vpn.IVpnLocalUserService;
import com.ruoyi.yianlian.service.vpn.IVpnOfflineLoginExportService;
import com.ruoyi.yianlian.utils.AesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 离线登录导出服务
 */
@Service
public class VpnOfflineLoginExportServiceImpl implements IVpnOfflineLoginExportService
{
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter EXPIRE_TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private IVpnLocalUserService localUserService;

    @Autowired
    private VpnUserMapper userMapper;

    @Autowired
    private IVpnLineAuthService lineAuthService;

    @Autowired
    private IVpnLineAppService lineAppService;

    @Autowired
    private AesUtils aesUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listSelectableLines(Long localUserId)
    {
        validateLocalUser(localUserId);
        Set<String> selectableAppIds = resolveSelectableAppIds(localUserId);
        if (selectableAppIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<LineApp> allLines = lineAppService.selectLineAppList(new LineApp());
        return allLines.stream()
            .filter(line -> selectableAppIds.contains(line.getAppId()) && "0".equals(line.getStatus()))
            .map(this::toSelectableLineVo)
            .collect(Collectors.toList());
    }

    @Override
    public void exportZip(HttpServletResponse response, VpnOfflineLoginExportRequest request)
    {
        VpnLocalUser localUser = validateLocalUser(request.getLocalUserId());
        VpnOfflineLoginValidity validity = VpnOfflineLoginValidity.fromCode(request.getValidity());
        Set<String> selectableAppIds = resolveSelectableAppIds(request.getLocalUserId());
        List<String> appIds = request.getAppIds();
        if (appIds == null || appIds.isEmpty())
        {
            throw new ServiceException("请至少选择一条线路");
        }
        for (String appId : appIds)
        {
            if (!selectableAppIds.contains(appId))
            {
                throw new ServiceException("线路「" + appId + "」不在可导出范围内");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String batchTs = now.format(FILE_TS);
        String expireAt = validity.resolveExpireAt(now).format(EXPIRE_TS);
        Map<String, LineApp> lineMap = loadEnabledLineMap();

        response.setContentType("application/zip");
        String zipName = "offline-login-" + sanitizeFileName(localUser.getUserName()) + "-" + batchTs + ".zip";
        try
        {
            String encodedName = URLEncoder.encode(zipName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"; filename*=UTF-8''" + encodedName);
            ZipOutputStream zip = new ZipOutputStream(response.getOutputStream());
            for (String appId : appIds)
            {
                LineApp line = lineMap.get(appId);
                if (line == null)
                {
                    throw new ServiceException("线路不存在或已停用: " + appId);
                }
                VpnUser lineUser = userMapper.selectUserByLocalUserIdAndAppId(request.getLocalUserId(), appId);
                if (lineUser == null || !"0".equals(lineUser.getStatus()))
                {
                    throw new ServiceException("线路用户不存在或已停用: " + line.getAppName());
                }
                VpnOfflineLoginDatPayload payload = buildPayload(line, lineUser, expireAt);
                String json = objectMapper.writeValueAsString(payload);
                String entryName = sanitizeFileName(line.getAppName()) + "-"
                    + sanitizeFileName(lineUser.getUserName()) + "-" + batchTs + ".dat";
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(json.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            zip.flush();
        }
        catch (IOException e)
        {
            throw new ServiceException("导出离线登录文件失败: " + e.getMessage());
        }
    }

    private VpnLocalUser validateLocalUser(Long localUserId)
    {
        if (localUserId == null)
        {
            throw new ServiceException("本地用户ID不能为空");
        }
        VpnLocalUser localUser = localUserService.selectLocalUserById(localUserId);
        if (localUser == null)
        {
            throw new ServiceException("本地用户不存在");
        }
        if (UserStatus.DELETED.getCode().equals(localUser.getDelFlag()))
        {
            throw new ServiceException("本地用户已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(localUser.getStatus()))
        {
            throw new ServiceException("本地用户已停用");
        }
        return localUser;
    }

    private Set<String> resolveSelectableAppIds(Long localUserId)
    {
        List<VpnUser> lineUsers = userMapper.selectUsersByLocalUserId(localUserId);
        if (lineUsers == null || lineUsers.isEmpty())
        {
            return new LinkedHashSet<>();
        }
        Set<String> selectableAppIds = new LinkedHashSet<>();
        for (VpnUser lineUser : lineUsers)
        {
            if (!"0".equals(lineUser.getStatus()))
            {
                continue;
            }
            Set<String> authorizedLineIds = lineAuthService.resolveAuthorizedLineIds(lineUser);
            for (String lineId : authorizedLineIds)
            {
                VpnUser onLine = userMapper.selectUserByLocalUserIdAndAppId(localUserId, lineId);
                if (onLine != null && "0".equals(onLine.getStatus()))
                {
                    selectableAppIds.add(lineId);
                }
            }
        }
        return selectableAppIds;
    }

    private Map<String, LineApp> loadEnabledLineMap()
    {
        List<LineApp> allLines = lineAppService.selectLineAppList(new LineApp());
        Map<String, LineApp> lineMap = new HashMap<>();
        for (LineApp line : allLines)
        {
            if ("0".equals(line.getStatus()))
            {
                lineMap.put(line.getAppId(), line);
            }
        }
        return lineMap;
    }

    private VpnOfflineLoginDatPayload buildPayload(LineApp line, VpnUser lineUser, String expireAt)
    {
        String plainPassword = decryptPassword(lineUser.getEncryptedPwd());
        if (StringUtils.isEmpty(plainPassword))
        {
            throw new ServiceException("线路「" + line.getAppName() + "」无法获取用户密码，请在线路用户管理中重置密码");
        }
        VpnOfflineLoginDatPayload payload = new VpnOfflineLoginDatPayload();
        payload.setAppId(line.getAppId());
        payload.setAppName(line.getAppName());
        payload.setHost(line.getHost());
        payload.setSrvPort(line.getSrvPort());
        payload.setSpaPort(line.getSpaPort());
        String spaKey = line.getSpaKey();
        if (spaKey != null && !spaKey.isEmpty())
        {
            spaKey = AesUtils.md5(aesUtils.decrypt(spaKey));
        }
        payload.setSpaKey(spaKey);
        payload.setUserName(lineUser.getUserName());
        payload.setPassword(aesUtils.encrypt(plainPassword));
        payload.setExpireAt(expireAt);
        return payload;
    }

    private String decryptPassword(String encryptedPwd)
    {
        if (StringUtils.isEmpty(encryptedPwd))
        {
            return null;
        }
        try
        {
            return aesUtils.decrypt(encryptedPwd);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private Map<String, Object> toSelectableLineVo(LineApp line)
    {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("appId", line.getAppId());
        vo.put("appName", line.getAppName());
        vo.put("host", line.getHost());
        vo.put("srvPort", line.getSrvPort());
        vo.put("spaPort", line.getSpaPort());
        return vo;
    }

    private String sanitizeFileName(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            return "unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
