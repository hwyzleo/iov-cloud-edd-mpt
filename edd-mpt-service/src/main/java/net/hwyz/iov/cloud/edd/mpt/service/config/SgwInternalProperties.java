package net.hwyz.iov.cloud.edd.mpt.service.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 服务网关内部请求签名配置。
 *
 * @author hwyz_leo
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "mpt.sgw.internal")
public class SgwInternalProperties {

    /** 是否启用对服务网关的签名调用 */
    private boolean enabled = true;

    /** 内部客户端ID，必须与服务网关登记值一致 */
    private String clientId = "mpt";

    /** HMAC 密钥，必须与服务网关配置一致 */
    private String secret;

    /** Feign 客户端未声明 path 时使用的兜底路径前缀（用于拼装待签名路径） */
    private String pathPrefix;

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
        log.info("服务网关内部签名配置已加载: enabled={}, clientId={}, 密钥指纹={}, 密钥长度={}",
                enabled, clientId, fingerprint(secret), secret.length());
    }

    /**
     * 密钥指纹（SHA-256 前 8 位十六进制），用于与 edd-sgw 启动日志中的指纹比对，不泄露密钥本身。
     * <p>
     * 必须与服务网关 {@code InternalHmacVerifier.fingerprint} 算法完全一致。
     *
     * @param secret 密钥
     * @return 指纹
     */
    public static String fingerprint(String secret) {
        if (secret == null || secret.isBlank()) {
            return "-empty-";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8))).substring(0, 8);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

}
