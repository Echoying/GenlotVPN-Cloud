package com.ruoyi.vpn.auth.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import static com.google.code.kaptcha.Constants.KAPTCHA_BORDER;
import static com.google.code.kaptcha.Constants.KAPTCHA_BORDER_COLOR;
import static com.google.code.kaptcha.Constants.KAPTCHA_IMAGE_HEIGHT;
import static com.google.code.kaptcha.Constants.KAPTCHA_IMAGE_WIDTH;
import static com.google.code.kaptcha.Constants.KAPTCHA_NOISE_COLOR;
import static com.google.code.kaptcha.Constants.KAPTCHA_NOISE_IMPL;
import static com.google.code.kaptcha.Constants.KAPTCHA_OBSCURIFICATOR_IMPL;
import static com.google.code.kaptcha.Constants.KAPTCHA_SESSION_CONFIG_KEY;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_CHAR_LENGTH;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_CHAR_SPACE;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_FONT_COLOR;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_FONT_NAMES;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE;
import static com.google.code.kaptcha.Constants.KAPTCHA_TEXTPRODUCER_IMPL;

/**
 * TCP 通道验证码 Bean（彩色样式，与 ruoyi-vpn-gateway 对齐）
 */
@Configuration
public class VpnCaptchaConfig
{
    /** 字符验证码：蓝字 + 绿色边框 + 水纹 */
    @Bean(name = "vpnCaptchaProducer")
    public DefaultKaptcha vpnCaptchaProducer()
    {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty(KAPTCHA_BORDER, "yes");
        properties.setProperty(KAPTCHA_BORDER_COLOR, "105,179,90");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "blue");
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "38");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "vpnCaptchaCode");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Courier");
        properties.setProperty(KAPTCHA_NOISE_COLOR, "105,179,90");
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.WaterRipple");
        kaptcha.setConfig(new Config(properties));
        return kaptcha;
    }

    /** 数学算式验证码：与 Web 端默认一致 */
    @Bean(name = "vpnCaptchaProducerMath")
    public DefaultKaptcha vpnCaptchaProducerMath()
    {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty(KAPTCHA_BORDER, "yes");
        properties.setProperty(KAPTCHA_BORDER_COLOR, "105,179,90");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "blue");
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "35");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "vpnCaptchaCodeMath");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_IMPL, "com.ruoyi.vpn.auth.config.VpnKaptchaTextCreator");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "3");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "6");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Courier");
        properties.setProperty(KAPTCHA_NOISE_COLOR, "white");
        properties.setProperty(KAPTCHA_NOISE_IMPL, "com.google.code.kaptcha.impl.NoNoise");
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.ShadowGimpy");
        kaptcha.setConfig(new Config(properties));
        return kaptcha;
    }
}
