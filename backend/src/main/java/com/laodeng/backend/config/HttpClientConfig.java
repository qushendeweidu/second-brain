package com.laodeng.backend.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laodeng.backend.config.properties.GitHubOAuthAppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

import static java.net.Proxy.Type.HTTP;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/10/13 00:00
 * @description Http请求客户端配置类
 */

@Configuration
public class HttpClientConfig {

    @Bean("githubRestClient")   // 起名字，避免和你项目里其他 RestClient 冲突
    public RestClient githubRestClient(GitHubOAuthAppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));   // 连接超时，防止线程被卡死
        factory.setReadTimeout(Duration.ofSeconds(10));  // 读取超时，防止线程卡死
        if (props.getProxyHost() != null && props.getProxyPort() != null) {
            factory.setProxy(new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(props.getProxyHost(), Integer.parseInt(props.getProxyPort()))));
        }
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, "laodeng-backend")   // GitHub API 要求必须带
                .build();
    }


}
