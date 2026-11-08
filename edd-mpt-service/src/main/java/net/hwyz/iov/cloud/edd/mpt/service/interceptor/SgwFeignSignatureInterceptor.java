package net.hwyz.iov.cloud.edd.mpt.service.interceptor;

import feign.RequestTemplate;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mpt.service.config.SgwInternalProperties;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 服务网关内部请求签名拦截器。
 * <p>
 * 仅对 {@code edd-sgw} 的 Feign 调用追加 HMAC 签名头，签名串与 SGW 校验逻辑一致：
 * {@code method\path\bodySha256\timestamp\nonce\nclient}。
 *
 * @author hwyz_leo
 */
@Slf4j
public class SgwFeignSignatureInterceptor implements RequestInterceptor {

    /** 内部客户端头 */
    public static final String HEADER_CLIENT = "X-Internal-Client";

    /** 时间戳头 */
    public static final String HEADER_TIMESTAMP = "X-Internal-Timestamp";

    /** 随机串头 */
    public static final String HEADER_NONCE = "X-Internal-Nonce";

    /** 签名头 */
    public static final String HEADER_SIGNATURE = "X-Internal-Signature";

    /** 操作者头 */
    public static final String HEADER_ACTOR = "X-Internal-Actor";

    private final SgwInternalProperties properties;

    public SgwFeignSignatureInterceptor(SgwInternalProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!properties.isEnabled() || !isSgwTarget(template)) {
            return;
        }
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("服务网关内部签名密钥未配置（mpt.sgw.internal.secret）");
        }
        String clientId = properties.getClientId();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String body = bodyText(template);
        String path = rawPath(template);
        String signature = sign(secret, template.method(), path, body, timestamp, nonce, clientId);

        putHeader(template, HEADER_CLIENT, clientId);
        putHeader(template, HEADER_TIMESTAMP, timestamp);
        putHeader(template, HEADER_NONCE, nonce);
        putHeader(template, HEADER_SIGNATURE, signature);
        String actor = actor(template);
        if (actor != null && !actor.isBlank()) {
            putHeader(template, HEADER_ACTOR, actor);
        }
    }

    /**
     * 计算签名
     *
     * @param secret    密钥
     * @param method    HTTP 方法
     * @param path      请求路径（不含查询参数）
     * @param body      请求体文本
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param clientId  内部客户端ID
     * @return 小写十六进制签名
     */
    public static String sign(String secret, String method, String path, String body,
                              String timestamp, String nonce, String clientId) {
        String canonical = method + "\n"
                + path + "\n"
                + sha256(body) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + clientId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("服务网关内部签名计算失败", e);
        }
    }

    /**
     * SHA-256 摘要
     *
     * @param value 文本
     * @return 小写十六进制摘要
     */
    public static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private boolean isSgwTarget(RequestTemplate template) {
        return template.feignTarget() != null
                && ServiceNameConstants.EDD_SGW.equals(template.feignTarget().name());
    }

    private String bodyText(RequestTemplate template) {
        byte[] body = template.body();
        if (body != null) {
            return new String(body, StandardCharsets.UTF_8);
        }
        String bodyTemplate = template.bodyTemplate();
        return bodyTemplate == null ? "" : bodyTemplate;
    }

    private String rawPath(RequestTemplate template) {
        String url = template.url();
        int query = url.indexOf('?');
        if (query >= 0) {
            url = url.substring(0, query);
        }
        int scheme = url.indexOf("://");
        if (scheme >= 0) {
            int slash = url.indexOf('/', scheme + 3);
            url = slash >= 0 ? url.substring(slash) : "/";
        }
        return url.isEmpty() ? "/" : url;
    }

    private void putHeader(RequestTemplate template, String name, String value) {
        Map<String, Collection<String>> headers = template.headers();
        headers.remove(name);
        template.header(name, value);
    }

    private String actor(RequestTemplate template) {
        Collection<String> usernames = template.headers().get("username");
        if (usernames != null && !usernames.isEmpty()) {
            return usernames.iterator().next();
        }
        try {
            String username = SecurityUtils.getUsername();
            return username == null || username.isBlank() ? null : username;
        } catch (RuntimeException e) {
            return null;
        }
    }

}
