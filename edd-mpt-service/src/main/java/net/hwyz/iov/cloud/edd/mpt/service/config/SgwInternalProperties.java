package net.hwyz.iov.cloud.edd.mpt.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务网关内部请求签名配置。
 *
 * @author hwyz_leo
 */
@Data
@ConfigurationProperties(prefix = "mpt.sgw.internal")
public class SgwInternalProperties {

    /** 是否启用对服务网关的签名调用 */
    private boolean enabled = true;

    /** 内部客户端ID，必须与服务网关登记值一致 */
    private String clientId = "mpt";

    /** HMAC 密钥，必须与服务网关配置一致 */
    private String secret;

}
