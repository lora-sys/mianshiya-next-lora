package com.lora.mianshihou.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * elastic search 配置客户端
 */
@Configuration
public class ElasticSearchConfig {
    @Value("${spring.elasticsearch.uris}")
    private String[] uris;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient() {
        HttpHost[] hosts = new HttpHost[uris.length];
        for(int i = 0; i < uris.length; i++){
            try {
                URI uri = new URI(uris[i]);
                String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
                String host = uri.getHost();
                int port = uri.getPort();
                if (port == -1) {
                    // 如果没有指定端口，使用默认端口
                    port = "https".equals(scheme) ? 443 : 9200;
                }
                hosts[i] = new HttpHost(host, port, scheme);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid Elasticsearch URI: " + uris[i], e);
            }
        }
        return new RestHighLevelClient(RestClient.builder(hosts));
    }

    //这个配置类是手动配置 RestHighLevelClient 的一个有效且常见的方式。它绕过了 Spring Data Elasticsearch 的自动配置，提供了更多的手动控制权。
    //需要注意的一点： RestHighLevelClient 在较新的 Elasticsearch 版本（8.x 及以上）和 Spring Boot 版本（3.x 及以上）中已经被标记为过时（deprecated），并被新的 ElasticsearchClient (来自 co.elastic.clients:elasticsearch-java 依赖) 所取代。
}