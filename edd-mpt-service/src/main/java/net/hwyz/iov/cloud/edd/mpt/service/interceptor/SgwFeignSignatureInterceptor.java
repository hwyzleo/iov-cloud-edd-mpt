package net.hwyz.iov.cloud.edd.mpt.service.interceptor;

import feign.RequestTemplate;
import feign.RequestInterceptor;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mpt.service.config.SgwInternalProperties;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotatedElementUtils;

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
        String canonical = canonical(template.method(), path, body, timestamp, nonce, clientId);
        String signature = hmacSha256Hex(secret, canonical);
        if (log.isInfoEnabled()) {
            log.info("服务网关内部签名(调用方): client={}, method={}, path={}, bodyHash={}, timestamp={}, nonce={},\n"
                            + "  计算签名={}, \n  密钥指纹={},\n  签名串=[{}]",
                    clientId, template.method(), path, sha256(body), timestamp, nonce, signature,
                    fingerprint(secret), canonical.replace("\n", "|"));
        }

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
        return hmacSha256Hex(secret, canonical(method, path, body, timestamp, nonce, clientId));
    }

    /**
     * 构造签名串（必须与服务网关 InternalHmacVerifier 完全一致）
     *
     * @param method    HTTP 方法
     * @param path      请求路径（不含查询参数）
     * @param body      请求体文本
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param clientId  内部客户端ID
     * @return 签名串
     */
    public static String canonical(String method, String path, String body,
                                   String timestamp, String nonce, String clientId) {
        return method + "\n"
                + path + "\n"
                + sha256(body) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + clientId;
    }

    /**
     * HMAC-SHA256
     *
     * @param secret    密钥
     * @param canonical 签名串
     * @return 小写十六进制签名
     */
    public static String hmacSha256Hex(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("服务网关内部签名计算失败", e);
        }
    }

    /**
     * 密钥指纹（SHA-256 前 8 位十六进制）。
     * <p>
     * 仅用于两端比对密钥是否一致，不泄露密钥本身；排查完成后建议移除相关日志。
     *
     * @param secret 密钥
     * @return 指纹
     */
    public static String fingerprint(String secret) {
        if (secret == null || secret.isBlank()) {
            return "-empty-";
        }
        return sha256(secret).substring(0, 8);
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
        if (url.isEmpty()) {
            url = "/";
        }
        return withClientPath(template, url);
    }

    /**
     * 补全 {@code @FeignClient(path = ...)} 前缀。
     * <p>
     * Feign 在 {@code RequestInterceptor} 执行完之后才通过 {@code target.apply()} 把
     * {@code @FeignClient} 的 {@code path} 拼到 URL 上，因此拦截器内 {@code template.url()} 只有方法级路径
     * （如 {@code /policies/auth}）。服务网关校验的是真实入站 {@code rawPath}
     * （如 {@code /api/service/sgw/v1/policies/auth}），不补全会必然验签失败。
     *
     * @param template 请求模板
     * @param path     方法级路径
     * @return 完整签名路径
     */
    private String withClientPath(RequestTemplate template, String path) {
        String prefix = clientPath(template);
        if (prefix == null || prefix.isBlank()) {
            return path;
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (path.equals(normalized) || path.startsWith(normalized + "/")) {
            return path;
        }
        return normalized + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * 读取 Feign 客户端声明的路径前缀，取不到时退回配置项
     *
     * @param template 请求模板
     * @return 路径前缀，可能为空
     */
    private String clientPath(RequestTemplate template) {
        Target<?> target = template.feignTarget();
        if (target != null) {
            FeignClient annotation = AnnotatedElementUtils.findMergedAnnotation(target.type(), FeignClient.class);
            if (annotation != null && annotation.path() != null && !annotation.path().isBlank()) {
                return annotation.path();
            }
        }
        return properties.getPathPrefix();
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
