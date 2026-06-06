package com.luckzll.demo.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String serverUrl = "https://openapi.alipay.com/gateway.do";
    private String appId = "";
    private String privateKey = "";
    private String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApPYbS9l4BWqjNuIFg5Q5CLAfGuPY+rfLmGxtlhs+m/gjlWY7yau+r+CsmQEiJZaXkFQHsLvSmdIlA03HcKBt+un1xx2D9jHbOZ9mV1+/Kxtmvn+uNu7AHbhL3PBEKPiUiTtHUjl8S0by5qC+W006l8L7ePT9j5ELCD9Mjoto2GeWtCqwz1GyECfu6IKeYaWOZmbOeUPkLGxXn4F4oNxi5RGTsGZp85VAJzJLVvtrFKTLe7XrxAuRbSUC3RLxmzNb7m9u1VAs8ALu9gggg8q4/VsP3A80x3Zjzv6BZ3RiTZPF9MNb7Z29vrAMcdiTcY5IRL8jLqWLKERcxuiSVO0l1wIDAQAB";
    private String format = "json";
    private String charset = "UTF-8";
    private String signType = "RSA2";

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
            serverUrl,
            appId,
            privateKey,
            format,
            charset,
            alipayPublicKey,
            signType
        );
    }
}
