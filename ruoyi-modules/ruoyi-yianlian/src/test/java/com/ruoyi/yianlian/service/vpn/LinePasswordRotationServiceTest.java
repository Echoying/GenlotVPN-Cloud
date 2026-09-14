package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.yianlian.domain.VpnUser;
import com.ruoyi.yianlian.mapper.VpnUserMapper;
import com.ruoyi.yianlian.utils.AesUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LinePasswordRotationServiceTest
{
    private VpnUserMapper userMapper;
    private IVpnLocalUserService localUserService;
    private IVpnUserService userService;
    private AesUtils aesUtils;
    private LinePasswordGenerator generator;
    private LinePasswordRotationGuard rotationGuard;
    private LinePasswordRotationService service;

    @Before
    public void setUp()
    {
        userMapper = mock(VpnUserMapper.class);
        localUserService = mock(IVpnLocalUserService.class);
        userService = mock(IVpnUserService.class);
        aesUtils = mock(AesUtils.class);
        generator = mock(LinePasswordGenerator.class);
        rotationGuard = mock(LinePasswordRotationGuard.class);
        when(rotationGuard.tryAcquire(any(), any(), org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn("lock-token");
        service = new LinePasswordRotationService(
            userMapper, localUserService, userService, aesUtils, generator, rotationGuard);
    }

    @Test
    public void rotate_updatesOnlyAuthorizedActiveLineUser()
    {
        VpnUser lineUser = activeLineUser();
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);
        when(aesUtils.decrypt("encrypted-old")).thenReturn("Old1!abc");
        when(generator.generate()).thenReturn("New2@xyz");
        when(aesUtils.encrypt("New2@xyz")).thenReturn("encrypted-new");
        when(userService.resetPwdWithSync(any(), eq("New2@xyz"))).thenReturn(1);

        service.rotate(1L, "line-a");

        ArgumentCaptor<VpnUser> captor = ArgumentCaptor.forClass(VpnUser.class);
        verify(userService).resetPwdWithSync(captor.capture(), eq("New2@xyz"));
        VpnUser passwordUpdate = captor.getValue();
        assertEquals(Long.valueOf(10L), passwordUpdate.getUserId());
        assertEquals("encrypted-new", passwordUpdate.getEncryptedPwd());
        assertTrue(SecurityUtils.matchesPassword("New2@xyz", passwordUpdate.getPassword()));
        // 宽更新字段必须缺省，避免把查询实体上的旧值写回
        assertNull(passwordUpdate.getPwdUpdateDate());
        assertNull(passwordUpdate.getStatus());
        assertNull(passwordUpdate.getLoginDate());
        assertNull(passwordUpdate.getLoginIp());
        assertNull(passwordUpdate.getNickName());
        assertNull(passwordUpdate.getRemark());
        assertNull(passwordUpdate.getYianlianId());
        assertEquals("encrypted-old", lineUser.getEncryptedPwd());
        verify(rotationGuard).markRotated(1L, "line-a");
        verify(rotationGuard).release(1L, "line-a", "lock-token");
    }

    @Test
    public void rotate_skipsWhenRecentlyRotated()
    {
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(rotationGuard.isRotatedRecently(1L, "line-a")).thenReturn(true);

        service.rotate(1L, "line-a");

        // 调用方超时重试时复用上一次结果，不得再打易安联
        verify(rotationGuard, never()).tryAcquire(any(), any(), org.mockito.ArgumentMatchers.anyLong());
        verify(userService, never()).resetPwdWithSync(any(), any());
    }

    @Test
    public void rotate_skipsWhenPreviousRotationFinishedDuringLockWait()
    {
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(rotationGuard.isRotatedRecently(1L, "line-a")).thenReturn(false, true);

        service.rotate(1L, "line-a");

        verify(userService, never()).resetPwdWithSync(any(), any());
        verify(rotationGuard, never()).markRotated(any(), any());
        verify(rotationGuard).release(1L, "line-a", "lock-token");
    }

    @Test
    public void rotate_rejectsWhenLockUnavailable()
    {
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(rotationGuard.tryAcquire(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(null);

        assertServiceException("线路密码正在更新，请稍后重试",
            () -> service.rotate(1L, "line-a"));

        verify(userService, never()).resetPwdWithSync(any(), any());
    }

    @Test
    public void rotate_doesNotMarkRotatedWhenSyncFails()
    {
        VpnUser lineUser = activeLineUser();
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);
        when(aesUtils.decrypt("encrypted-old")).thenReturn("Old1!abc");
        when(generator.generate()).thenReturn("New2@xyz");
        when(aesUtils.encrypt("New2@xyz")).thenReturn("encrypted-new");
        when(userService.resetPwdWithSync(any(), eq("New2@xyz"))).thenReturn(0);

        assertServiceException("更新线路密码失败，请重试",
            () -> service.rotate(1L, "line-a"));

        verify(rotationGuard, never()).markRotated(any(), any());
        verify(rotationGuard).release(1L, "line-a", "lock-token");
    }

    @Test
    public void rotate_rejectsWhenResetReturnsZero()
    {
        VpnUser lineUser = activeLineUser();
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);
        when(aesUtils.decrypt("encrypted-old")).thenReturn("Old1!abc");
        when(generator.generate()).thenReturn("New2@xyz");
        when(aesUtils.encrypt("New2@xyz")).thenReturn("encrypted-new");
        when(userService.resetPwdWithSync(any(), eq("New2@xyz"))).thenReturn(0);

        assertServiceException("更新线路密码失败，请重试",
            () -> service.rotate(1L, "line-a"));
    }

    @Test
    public void rotate_rejectsUnauthorizedLine()
    {
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(false);

        assertServiceException("您无权访问所选线路，请联系管理员",
            () -> service.rotate(1L, "line-a"));

        verify(userMapper, never()).selectUserByLocalUserIdAndAppId(any(), any());
    }

    @Test
    public void rotate_rejectsUnavailableLineUser()
    {
        VpnUser lineUser = activeLineUser();
        lineUser.setStatus("1");
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);

        assertServiceException("您无权访问所选线路，请联系管理员",
            () -> service.rotate(1L, "line-a"));

        verify(userService, never()).resetPwdWithSync(any(), any());
    }

    @Test
    public void rotate_rejectsMissingPassword()
    {
        VpnUser lineUser = activeLineUser();
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);
        when(aesUtils.decrypt("encrypted-old")).thenThrow(new RuntimeException("decrypt failed"));

        assertServiceException("无法获取线路用户密码，请在线路用户管理中重置密码",
            () -> service.rotate(1L, "line-a"));
    }

    @Test
    public void rotate_rejectsEightPasswordCollisions()
    {
        VpnUser lineUser = activeLineUser();
        when(localUserService.isAuthorizedForLine(1L, "line-a")).thenReturn(true);
        when(userMapper.selectUserByLocalUserIdAndAppId(1L, "line-a")).thenReturn(lineUser);
        when(aesUtils.decrypt("encrypted-old")).thenReturn("Old1!abc");
        when(generator.generate()).thenReturn("Old1!abc");

        assertServiceException("生成线路密码失败，请重试",
            () -> service.rotate(1L, "line-a"));

        verify(generator, org.mockito.Mockito.times(8)).generate();
        verify(userService, never()).resetPwdWithSync(any(), any());
    }

    private VpnUser activeLineUser()
    {
        VpnUser user = new VpnUser(10L);
        user.setAppId("line-a");
        user.setStatus("0");
        user.setDelFlag("0");
        user.setEncryptedPwd("encrypted-old");
        user.setNickName("线路用户");
        user.setLoginIp("10.0.0.1");
        user.setLoginDate(new Date());
        user.setPwdUpdateDate(new Date());
        user.setRemark("旧备注");
        user.setYianlianId("remote-1");
        return user;
    }

    private void assertServiceException(String expectedMessage, Runnable action)
    {
        try
        {
            action.run();
        }
        catch (ServiceException e)
        {
            if (expectedMessage != null)
            {
                assertEquals(expectedMessage, e.getMessage());
            }
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }
}
