package net.hwyz.iov.cloud.edd.mpt.service.config;

import feign.codec.ErrorDecoder;
import net.hwyz.iov.cloud.edd.mpt.service.exception.SgwRemoteErrorDecoder;
import net.hwyz.iov.cloud.edd.mpt.service.interceptor.SgwFeignSignatureInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 服务网关 Feign 调用配置。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableConfigurationProperties(SgwInternalProperties.class)
public class SgwFeignConfiguration {

    /**
     * 服务网关签名拦截器
     *
     * @param properties 签名配置
     * @return 拦截器
     */
    @Bean
    public SgwFeignSignatureInterceptor sgwFeignSignatureInterceptor(SgwInternalProperties properties) {
        return new SgwFeignSignatureInterceptor(properties);
    }

    /**
     * 服务网关错误解码器
     *
     * @param objectMapper JSON 解析器
     * @return 错误解码器
     */
    @Bean
    public ErrorDecoder sgwRemoteErrorDecoder(ObjectMapper objectMapper) {
        return new SgwRemoteErrorDecoder(objectMapper);
    }

}
