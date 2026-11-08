package net.hwyz.iov.cloud.edd.mpt.service.interceptor;

import feign.RequestTemplate;
import feign.Target;
import net.hwyz.iov.cloud.edd.mpt.service.config.SgwInternalProperties;
import net.hwyz.iov.cloud.edd.sgw.api.service.SgwRouteAdminService;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 服务网关 Feign 签名拦截器单元测试。
 *
 * @author hwyz_leo
 */
class SgwFeignSignatureInterceptorTest {

    private static final String SECRET = "unit-secret";

    @Test
    @DisplayName("为 SGW 调用追加完整签名头并与规范签名一致")
    void signSgwRequest() throws Exception {
        SgwFeignSignatureInterceptor interceptor = new SgwFeignSignatureInterceptor(properties(SECRET, true));
        RequestTemplate template = template(ServiceNameConstants.EDD_SGW, "POST",
                "/api/service/sgw/v1/routes", "{\"routeCode\":\"r1\"}");

        interceptor.apply(template);

        String timestamp = header(template, SgwFeignSignatureInterceptor.HEADER_TIMESTAMP);
        String nonce = header(template, SgwFeignSignatureInterceptor.HEADER_NONCE);
        String signature = header(template, SgwFeignSignatureInterceptor.HEADER_SIGNATURE);
        assertThat(header(template, SgwFeignSignatureInterceptor.HEADER_CLIENT)).isEqualTo("mpt");
        assertThat(timestamp).isNotBlank();
        assertThat(nonce).isNotBlank();
        String expected = hmacSha256(SECRET, "POST\n/api/service/sgw/v1/routes\n"
                + sha256("{\"routeCode\":\"r1\"}") + "\n" + timestamp + "\n" + nonce + "\nmpt");
        assertThat(signature).isEqualTo(expected);
    }

    @Test
    @DisplayName("GET 请求使用空请求体摘要")
    void getUsesEmptyBodyHash() throws Exception {
        SgwFeignSignatureInterceptor interceptor = new SgwFeignSignatureInterceptor(properties(SECRET, true));
        RequestTemplate template = template(ServiceNameConstants.EDD_SGW, "GET",
                "/api/service/sgw/v1/workspace", null);

        interceptor.apply(template);

        String timestamp = header(template, SgwFeignSignatureInterceptor.HEADER_TIMESTAMP);
        String nonce = header(template, SgwFeignSignatureInterceptor.HEADER_NONCE);
        String expected = hmacSha256(SECRET, "GET\n/api/service/sgw/v1/workspace\n"
                + sha256("") + "\n" + timestamp + "\n" + nonce + "\nmpt");
        assertThat(header(template, SgwFeignSignatureInterceptor.HEADER_SIGNATURE)).isEqualTo(expected);
    }

    @Test
    @DisplayName("非 SGW 目标不追加签名头")
    void nonSgwTargetUntouched() {
        SgwFeignSignatureInterceptor interceptor = new SgwFeignSignatureInterceptor(properties(SECRET, true));
        RequestTemplate template = template("edd-vmd", "POST", "/api/service/part/v1", "{}");

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(SgwFeignSignatureInterceptor.HEADER_SIGNATURE);
    }

    @Test
    @DisplayName("未配置密钥时失败关闭")
    void missingSecretFailsClosed() {
        SgwFeignSignatureInterceptor interceptor = new SgwFeignSignatureInterceptor(properties("", true));
        RequestTemplate template = template(ServiceNameConstants.EDD_SGW, "GET",
                "/api/service/sgw/v1/routes", null);
        assertThatThrownBy(() -> interceptor.apply(template))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("签名密钥未配置");
    }

    @Test
    @DisplayName("禁用时跳过签名")
    void disabledSkips() {
        SgwFeignSignatureInterceptor interceptor = new SgwFeignSignatureInterceptor(properties(SECRET, false));
        RequestTemplate template = template(ServiceNameConstants.EDD_SGW, "GET",
                "/api/service/sgw/v1/routes", null);
        interceptor.apply(template);
        assertThat(template.headers()).doesNotContainKey(SgwFeignSignatureInterceptor.HEADER_SIGNATURE);
    }

    private SgwInternalProperties properties(String secret, boolean enabled) {
        SgwInternalProperties properties = new SgwInternalProperties();
        properties.setClientId("mpt");
        properties.setSecret(secret);
        properties.setEnabled(enabled);
        return properties;
    }

    private RequestTemplate template(String targetName, String method, String path, String body) {
        RequestTemplate template = new RequestTemplate();
        template.method(method);
        template.target("https://" + targetName);
        template.uri(path);
        template.feignTarget(new Target.HardCodedTarget<>(SgwRouteAdminService.class, targetName,
                "https://" + targetName));
        if (body != null) {
            template.body(body);
        }
        return template;
    }

    private String header(RequestTemplate template, String name) {
        Collection<String> values = template.headers().get(name);
        assertThat(values).isNotNull();
        return values.iterator().next();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hmacSha256(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

}
