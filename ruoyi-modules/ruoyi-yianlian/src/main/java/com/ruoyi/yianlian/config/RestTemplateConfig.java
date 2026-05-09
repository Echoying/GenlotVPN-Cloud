package com.ruoyi.yianlian.config;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * RestTemplate 配置 - 支持 HTTPS 自签名证书
 */
@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplateBuilder restTemplateBuilder()
    {
        return new RestTemplateBuilder()
                .requestFactory(this::httpRequestFactory);
    }

    private HttpComponentsClientHttpRequestFactory httpRequestFactory()
    {
        try
        {
            // 创建信任所有证书的 SSLContext
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();

            // 创建 SSLConnectionSocketFactory，跳过主机名验证
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            // 创建 HttpClient
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            // 创建请求工厂
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);
            factory.setConnectTimeout(10000); // 连接超时 10 秒
            factory.setReadTimeout(30000);    // 读取超时 30 秒

            return factory;
        }
        catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e)
        {
            throw new RuntimeException("创建 RestTemplate 失败", e);
        }
    }
}
