package com.ruoyi.yianlian.service.vpn;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 线路密码生成器。
 */
@Component
public class LinePasswordGenerator
{
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*";
    private static final String ALL_CHARACTERS =
        UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARACTERS;
    private static final int PASSWORD_LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate()
    {
        char[] password = new char[PASSWORD_LENGTH];
        password[0] = randomCharacter(UPPER_CASE);
        password[1] = randomCharacter(LOWER_CASE);
        password[2] = randomCharacter(DIGITS);
        password[3] = randomCharacter(SPECIAL_CHARACTERS);
        for (int i = 4; i < PASSWORD_LENGTH; i++)
        {
            password[i] = randomCharacter(ALL_CHARACTERS);
        }
        shuffle(password);
        return new String(password);
    }

    private char randomCharacter(String characters)
    {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }

    private void shuffle(char[] characters)
    {
        for (int i = characters.length - 1; i > 0; i--)
        {
            int swapIndex = secureRandom.nextInt(i + 1);
            char current = characters[i];
            characters[i] = characters[swapIndex];
            characters[swapIndex] = current;
        }
    }
}
