package com.ruoyi.yianlian.service.vpn;

import com.ruoyi.common.core.exception.ServiceException;
import org.junit.Test;

import static org.junit.Assert.*;

public class FeedbackRulesTest
{
    @Test
    public void normalizeCategory_illegalBecomesEmpty()
    {
        assertEquals("connect", FeedbackRules.normalizeCategory("connect"));
        assertEquals("", FeedbackRules.normalizeCategory("bug"));
        assertEquals("", FeedbackRules.normalizeCategory(null));
    }

    @Test
    public void isValidStatus_onlyZeroOneTwo()
    {
        assertTrue(FeedbackRules.isValidStatus("0"));
        assertTrue(FeedbackRules.isValidStatus("1"));
        assertTrue(FeedbackRules.isValidStatus("2"));
        assertFalse(FeedbackRules.isValidStatus("3"));
        assertFalse(FeedbackRules.isValidStatus(""));
        assertFalse(FeedbackRules.isValidStatus(null));
    }

    @Test(expected = ServiceException.class)
    public void assertTitle_blankThrows()
    {
        FeedbackRules.assertTitle(null);
    }

    @Test(expected = ServiceException.class)
    public void assertTitle_tooLongThrows()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 81; i++)
        {
            sb.append('a');
        }
        FeedbackRules.assertTitle(sb.toString());
    }

    @Test(expected = ServiceException.class)
    public void assertContent_blankThrows()
    {
        FeedbackRules.assertContent("   ");
    }

    @Test(expected = ServiceException.class)
    public void assertContent_tooLongThrows()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2001; i++)
        {
            sb.append('a');
        }
        FeedbackRules.assertContent(sb.toString());
    }

    @Test
    public void image_jpegHeaderOkPngHeaderOk()
    {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertEquals("image/jpeg", FeedbackImageRules.detectContentType(jpeg));
        assertEquals("image/png", FeedbackImageRules.detectContentType(png));
        assertNull(FeedbackImageRules.detectContentType(new byte[] {0x00}));
    }

    @Test(expected = ServiceException.class)
    public void assertUpload_overMaxBytesThrows()
    {
        byte[] tooLarge = new byte[FeedbackImageRules.MAX_BYTES + 1];
        tooLarge[0] = (byte) 0xFF;
        tooLarge[1] = (byte) 0xD8;
        tooLarge[2] = (byte) 0xFF;
        FeedbackImageRules.assertUpload(tooLarge);
    }
}
