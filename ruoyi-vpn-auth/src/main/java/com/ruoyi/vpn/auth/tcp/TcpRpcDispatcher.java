package com.ruoyi.vpn.auth.tcp;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.CaptchaException;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.auth.AuthUtil;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.vpn.auth.service.VpnCaptchaService;
import com.ruoyi.vpn.auth.service.VpnLineVerifyService;
import com.ruoyi.vpn.auth.service.VpnLoginService;
import com.ruoyi.vpn.auth.utils.AesUtils;
import com.ruoyi.vpn.protocol.ChangePasswordRequest;
import com.ruoyi.vpn.protocol.ChangePasswordResponse;
import com.ruoyi.vpn.protocol.ConfirmLineVerifyRequest;
import com.ruoyi.vpn.protocol.ConfirmLineVerifyResponse;
import com.ruoyi.vpn.protocol.Envelope;
import com.ruoyi.vpn.protocol.GetAuthorizedLinesRequest;
import com.ruoyi.vpn.protocol.GetAuthorizedLinesResponse;
import com.ruoyi.vpn.protocol.GetCaptchaRequest;
import com.ruoyi.vpn.protocol.GetCaptchaResponse;
import com.ruoyi.vpn.protocol.GetUserCredentialsRequest;
import com.ruoyi.vpn.protocol.GetUserCredentialsResponse;
import com.ruoyi.vpn.protocol.ListPublicLinesRequest;
import com.ruoyi.vpn.protocol.ListPublicLinesResponse;
import com.ruoyi.vpn.protocol.LoginRequest;
import com.ruoyi.vpn.protocol.LoginResponse;
import com.ruoyi.vpn.protocol.LogoutRequest;
import com.ruoyi.vpn.protocol.LogoutResponse;
import com.ruoyi.vpn.protocol.MessageType;
import com.ruoyi.vpn.protocol.RefreshTokenRequest;
import com.ruoyi.vpn.protocol.RefreshTokenResponse;
import com.ruoyi.vpn.protocol.RpcResponse;
import com.ruoyi.vpn.protocol.SendLineVerifyRequest;
import com.ruoyi.vpn.protocol.SendLineVerifyResponse;
import com.ruoyi.yianlian.api.RemoteVpnUserService;
import com.ruoyi.yianlian.api.model.VpnLoginUser;

/**
 * TCP RPC 分发器
 */
@Component
public class TcpRpcDispatcher
{
    private static final Logger log = LoggerFactory.getLogger(TcpRpcDispatcher.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private VpnLoginService vpnLoginService;

    @Autowired
    private VpnCaptchaService vpnCaptchaService;

    @Autowired
    private VpnLineVerifyService vpnLineVerifyService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RemoteVpnUserService remoteVpnUserService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private AesUtils aesUtils;

    public RpcResult dispatch(Envelope envelope, TcpSessionContext session)
    {
        MessageType type = envelope.getType();
        if (type == null || type == MessageType.MESSAGE_TYPE_UNSPECIFIED)
        {
            return RpcResult.fail("未知消息类型");
        }
        try
        {
            switch (type)
            {
                case GET_CAPTCHA:
                    return handleGetCaptcha(envelope);
                case LIST_PUBLIC_LINES:
                    return handleListPublicLines(envelope);
                case LOGIN:
                    return handleLogin(envelope, session);
                case CHANGE_PASSWORD:
                    return handleChangePassword(envelope);
                case LOGOUT:
                    return handleLogout(envelope, session);
                case REFRESH_TOKEN:
                    return handleRefreshToken(envelope, session);
                case GET_AUTHORIZED_LINES:
                    return handleGetAuthorizedLines(envelope, session);
                case SEND_LINE_VERIFY:
                    return handleSendLineVerify(envelope, session);
                case CONFIRM_LINE_VERIFY:
                    return handleConfirmLineVerify(envelope, session);
                case GET_USER_CREDENTIALS:
                    return handleGetUserCredentials(envelope, session);
                default:
                    return RpcResult.fail("不支持的消息类型");
            }
        }
        catch (ServiceException e)
        {
            return RpcResult.fail(e.getMessage());
        }
        catch (CaptchaException e)
        {
            return RpcResult.fail(e.getMessage());
        }
        catch (InvalidProtocolBufferException e)
        {
            log.warn("Protobuf 解析失败", e);
            return RpcResult.fail("请求格式错误");
        }
        catch (java.io.IOException e)
        {
            log.warn("验证码生成失败", e);
            return RpcResult.fail("验证码生成失败");
        }
        catch (Exception e)
        {
            log.error("RPC 处理异常 type={}", type, e);
            return RpcResult.fail("服务内部错误");
        }
    }

    private RpcResult handleGetCaptcha(Envelope envelope) throws InvalidProtocolBufferException, java.io.IOException
    {
        GetCaptchaRequest.parseFrom(envelope.getPayload());
        Map<String, Object> captcha = vpnCaptchaService.createCaptcha();
        GetCaptchaResponse.Builder builder = GetCaptchaResponse.newBuilder()
                .setCaptchaEnabled(Boolean.TRUE.equals(captcha.get("captchaEnabled")));
        if (captcha.get("uuid") != null)
        {
            builder.setUuid(String.valueOf(captcha.get("uuid")));
        }
        if (captcha.get("img") != null)
        {
            builder.setImgBase64(String.valueOf(captcha.get("img")));
        }
        return RpcResult.ok(builder.build());
    }

    private RpcResult handleListPublicLines(Envelope envelope) throws InvalidProtocolBufferException
    {
        ListPublicLinesRequest.parseFrom(envelope.getPayload());
        List<Map<String, Object>> lines = vpnLoginService.listPublicLines();
        ListPublicLinesResponse.Builder builder = ListPublicLinesResponse.newBuilder();
        TcpLineMapper.addAllLines(builder, lines);
        return RpcResult.ok(builder.build());
    }

    private RpcResult handleLogin(Envelope envelope, TcpSessionContext session) throws InvalidProtocolBufferException
    {
        LoginRequest req = LoginRequest.parseFrom(envelope.getPayload());
        vpnCaptchaService.checkCaptcha(req.getCode(), req.getUuid());
        VpnLoginUser userInfo = vpnLoginService.login(req.getUsername(), req.getPassword(), req.getAppId());
        Map<String, Object> tokenMap = tokenService.createToken(userInfo);

        byte[] sessionKey = new byte[32];
        SECURE_RANDOM.nextBytes(sessionKey);

        session.setSessionKey(sessionKey);
        session.setAccessToken(String.valueOf(tokenMap.get("access_token")));
        session.setUserId(userInfo.getVpnUser().getUserId());
        session.setUsername(userInfo.getVpnUser().getUserName());
        session.setAuthenticated(true);

        LoginResponse response = LoginResponse.newBuilder()
                .setAccessToken(session.getAccessToken())
                .setExpiresIn(((Number) tokenMap.get("expires_in")).longValue())
                .setSessionKey(ByteString.copyFrom(sessionKey))
                .build();
        return RpcResult.ok(response);
    }

    private RpcResult handleChangePassword(Envelope envelope) throws InvalidProtocolBufferException
    {
        ChangePasswordRequest req = ChangePasswordRequest.parseFrom(envelope.getPayload());
        vpnLoginService.changePassword(req.getUsername(), req.getOldPassword(), req.getNewPassword(), req.getAppId());
        return RpcResult.ok(ChangePasswordResponse.newBuilder().build());
    }

    private RpcResult handleLogout(Envelope envelope, TcpSessionContext session) throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        LogoutRequest.parseFrom(envelope.getPayload());
        String token = resolveToken(envelope, session);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            AuthUtil.logoutByToken(token);
            vpnLoginService.logout(username);
        }
        session.clearSecrets();
        session.setAuthenticated(false);
        return RpcResult.ok(LogoutResponse.newBuilder().build());
    }

    private RpcResult handleRefreshToken(Envelope envelope, TcpSessionContext session) throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        RefreshTokenRequest.parseFrom(envelope.getPayload());
        String token = resolveToken(envelope, session);
        com.ruoyi.system.api.model.LoginUser loginUser = tokenService.getLoginUser(token);
        if (loginUser == null)
        {
            return RpcResult.fail("登录状态已过期");
        }
        tokenService.refreshToken(loginUser);
        return RpcResult.ok(RefreshTokenResponse.newBuilder().build());
    }

    private RpcResult handleGetAuthorizedLines(Envelope envelope, TcpSessionContext session)
            throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        GetAuthorizedLinesRequest.parseFrom(envelope.getPayload());
        Long userId = resolveUserId(envelope, session);
        R<List<Map<String, Object>>> result = remoteVpnUserService.getAuthorizedLines(userId, SecurityConstants.INNER);
        if (R.FAIL == result.getCode() || result.getData() == null)
        {
            return RpcResult.fail(StringUtils.isNotEmpty(result.getMsg()) ? result.getMsg() : "获取授权线路失败");
        }
        GetAuthorizedLinesResponse.Builder builder = GetAuthorizedLinesResponse.newBuilder();
        TcpLineMapper.addAllLines(builder, result.getData());
        return RpcResult.ok(builder.build());
    }

    private RpcResult handleSendLineVerify(Envelope envelope, TcpSessionContext session)
            throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        SendLineVerifyRequest req = SendLineVerifyRequest.parseFrom(envelope.getPayload());
        Long userId = resolveUserId(envelope, session);
        String username = resolveUsername(envelope, session);
        Map<String, String> result = vpnLineVerifyService.sendCode(userId, username, req.getAppId(), req.getLineName());
        SendLineVerifyResponse.Builder builder = SendLineVerifyResponse.newBuilder();
        if (result.get("expireAt") != null)
        {
            builder.setExpireAt(result.get("expireAt"));
        }
        return RpcResult.ok(builder.build());
    }

    private RpcResult handleConfirmLineVerify(Envelope envelope, TcpSessionContext session)
            throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        ConfirmLineVerifyRequest req = ConfirmLineVerifyRequest.parseFrom(envelope.getPayload());
        Long userId = resolveUserId(envelope, session);
        vpnLineVerifyService.confirmCode(userId, req.getAppId(), req.getCode());
        return RpcResult.ok(ConfirmLineVerifyResponse.newBuilder().build());
    }

    private RpcResult handleGetUserCredentials(Envelope envelope, TcpSessionContext session)
            throws InvalidProtocolBufferException
    {
        requireAuth(session, envelope);
        GetUserCredentialsRequest req = GetUserCredentialsRequest.parseFrom(envelope.getPayload());
        Long userId = resolveUserId(envelope, session);
        String username = resolveUsername(envelope, session);

        vpnLineVerifyService.consumePassed(userId, req.getAppId());

        String plainPassword = redisService.getCacheObject("vpn_plain_pwd:" + userId);
        if (StringUtils.isEmpty(plainPassword))
        {
            return RpcResult.fail("凭证已过期，请重新登录");
        }
        String encryptedPassword = aesUtils.encrypt(plainPassword);
        GetUserCredentialsResponse response = GetUserCredentialsResponse.newBuilder()
                .setUsername(username)
                .setPassword(encryptedPassword)
                .build();
        return RpcResult.ok(response);
    }

    private void requireAuth(TcpSessionContext session, Envelope envelope)
    {
        if (session.isAuthenticated())
        {
            return;
        }
        String token = resolveToken(envelope, session);
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("未登录或会话已失效");
        }
        try
        {
            session.setAccessToken(token);
            session.setUserId(Long.parseLong(JwtUtils.getUserId(token)));
            session.setUsername(JwtUtils.getUserName(token));
            session.setAuthenticated(true);
        }
        catch (Exception e)
        {
            throw new ServiceException("登录状态已过期");
        }
    }

    private String resolveToken(Envelope envelope, TcpSessionContext session)
    {
        if (StringUtils.isNotEmpty(envelope.getAccessToken()))
        {
            return envelope.getAccessToken();
        }
        return session.getAccessToken();
    }

    private Long resolveUserId(Envelope envelope, TcpSessionContext session)
    {
        String token = resolveToken(envelope, session);
        if (StringUtils.isNotEmpty(token))
        {
            return Long.parseLong(JwtUtils.getUserId(token));
        }
        return session.getUserId();
    }

    private String resolveUsername(Envelope envelope, TcpSessionContext session)
    {
        String token = resolveToken(envelope, session);
        if (StringUtils.isNotEmpty(token))
        {
            return JwtUtils.getUserName(token);
        }
        return session.getUsername();
    }

    /**
     * RPC 处理结果
     */
    public static final class RpcResult
    {
        private final RpcResponse response;

        private RpcResult(RpcResponse response)
        {
            this.response = response;
        }

        public static RpcResult ok(com.google.protobuf.Message data)
        {
            RpcResponse.Builder builder = RpcResponse.newBuilder().setCode(200);
            if (data != null)
            {
                builder.setData(data.toByteString());
            }
            return new RpcResult(builder.build());
        }

        public static RpcResult fail(String msg)
        {
            return new RpcResult(RpcResponse.newBuilder().setCode(500).setMsg(msg).build());
        }

        public RpcResponse getResponse()
        {
            return response;
        }
    }
}
