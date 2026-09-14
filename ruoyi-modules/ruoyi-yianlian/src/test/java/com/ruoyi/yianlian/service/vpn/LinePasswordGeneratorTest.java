package com.ruoyi.yianlian.service.vpn;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LinePasswordGeneratorTest
{
    private static final String ALLOWED_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    @Test
    public void generate_meetsPasswordRules()
    {
        LinePasswordGenerator generator = new LinePasswordGenerator();

        for (int i = 0; i < 1000; i++)
        {
            String password = generator.generate();

            assertEquals(8, password.length());
            assertTrue(password.matches(".*[A-Z].*"));
            assertTrue(password.matches(".*[a-z].*"));
            assertTrue(password.matches(".*[0-9].*"));
            assertTrue(password.matches(".*[!@#$%^&*].*"));
            assertTrue(password.matches("[" + ALLOWED_CHARACTERS + "]+"));
        }
    }
}
