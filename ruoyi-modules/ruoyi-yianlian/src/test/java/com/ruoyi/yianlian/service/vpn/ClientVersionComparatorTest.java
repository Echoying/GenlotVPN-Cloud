package com.ruoyi.yianlian.service.vpn;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClientVersionComparatorTest
{
    @Test
    public void isValid_acceptsSemver()
    {
        assertTrue(ClientVersionComparator.isValid("1.2.0"));
        assertFalse(ClientVersionComparator.isValid("1.2"));
        assertFalse(ClientVersionComparator.isValid(""));
        assertFalse(ClientVersionComparator.isValid(null));
    }

    @Test
    public void compare_numericSegments()
    {
        assertTrue(ClientVersionComparator.compare("1.2.0", "1.2.0") == 0);
        assertTrue(ClientVersionComparator.compare("1.2.0", "1.10.0") < 0);
        assertTrue(ClientVersionComparator.isLower("1.1.9", "1.2.0"));
        assertFalse(ClientVersionComparator.isLower("1.2.0", "1.2.0"));
    }
}
