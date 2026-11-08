package net.hwyz.iov.cloud.edd.mpt.service.config;

import jakarta.annotation.PostConstruct;
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

    /**
     * 启动期校验，避免密钥缺失时应用启动成功、直到调用服务网关才报错。
     */
    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("服务网关内部客户端ID未配置：请设置 mpt.sgw.internal.client-id"
                    + "（或环境变量 SGW_INTERNAL_CLIENT），且需与服务网关 sgw.security.internal.clients 中登记的键一致");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("服务网关内部签名密钥未配置：请在 Nacos edd-mpt.yaml 设置"
                    + " mpt.sgw.internal.secret，或设置环境变量 SGW_INTERNAL_SECRET"
                    + "（须与 edd-sgw 的 sgw.security.internal.clients.mpt 完全一致）；"
                    + "若本服务无需调用服务网关，可将 mpt.sgw.internal.enabled 设为 false");
        }
    }

}
