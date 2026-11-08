package net.hwyz.iov.cloud.edd.mpt.service.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.RequestLine;
import feign.Target;
import net.hwyz.iov.cloud.edd.mpt.service.config.SgwInternalProperties;
import net.hwyz.iov.cloud.edd.mpt.service.exception.SgwRemoteErrorDecoder;
import net.hwyz.iov.cloud.edd.mpt.service.exception.SgwRemoteException;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 服务网关 Feign 签名与错误映射集成测试（本地模拟 SGW 服务端）。
 *
 * @author hwyz_leo
 */
class SgwFeignSigningIT {

    private static final String SECRET = "it-secret";

    private static final Map<String, String> LAST_REQUEST = new ConcurrentHashMap<>();

    private static final AtomicInteger CONFLICT_HITS = new AtomicInteger();

    private static HttpServer server;

    private static String baseUrl;

    private static SgwTestClient client;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
            LAST_REQUEST.put("method", exchange.getRequestMethod());
            LAST_REQUEST.put("path", path);
            LAST_REQUEST.put("body", body);
            for (String header : new String[]{"X-Internal-Client", "X-Internal-Timestamp",
                    "X-Internal-Nonce", "X-Internal-Signature"}) {
                String value = exchange.getRequestHeaders().getFirst(header);
                if (value != null) {
                    LAST_REQUEST.put(header, value);
                }
            }
            if (path.endsWith("/conflict")) {
                CONFLICT_HITS.incrementAndGet();
                byte[] error = ("{\"code\":\"SGW-409001\",\"message\":\"配置版本冲突\","
                        + "\"timestamp\":" + System.currentTimeMillis() + "}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(409, error.length);
                exchange.getResponseBody().write(error);
                exchange.close();
                return;
            }
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        SgwInternalProperties properties = new SgwInternalProperties();
        properties.setClientId("mpt");
        properties.setSecret(SECRET);
        client = Feign.builder()
                .requestInterceptor(new SgwFeignSignatureInterceptor(properties))
                .errorDecoder(new SgwRemoteErrorDecoder(new ObjectMapper()))
                .target(new Target.HardCodedTarget<>(SgwTestClient.class, ServiceNameConstants.EDD_SGW, baseUrl));
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("POST 调用的签名可被按 SGW 规范复算")
    void postSignatureCompatible() throws Exception {
        String payload = "{\"routeCode\":\"vehicle-profile\"}";
        assertThat(client.create(payload)).isEqualTo("ok");
        assertThat(LAST_REQUEST.get("method")).isEqualTo("POST");
        assertThat(LAST_REQUEST.get("path")).isEqualTo("/api/service/sgw/v1/routes");
        assertThat(LAST_REQUEST.get("body")).isEqualTo(payload);
        assertThat(LAST_REQUEST.get("X-Internal-Client")).isEqualTo("mpt");
        String timestamp = LAST_REQUEST.get("X-Internal-Timestamp");
        String nonce = LAST_REQUEST.get("X-Internal-Nonce");
        String expected = hmacSha256(SECRET, "POST\n/api/service/sgw/v1/routes\n"
                + sha256(payload) + "\n" + timestamp + "\n" + nonce + "\nmpt");
        assertThat(LAST_REQUEST.get("X-Internal-Signature")).isEqualTo(expected);
    }

    @Test
    @DisplayName("重复调用使用不同 nonce，GET 使用空请求体摘要")
    void nonceUniqueAndGetBodyHash() throws Exception {
        client.workspace();
        String firstNonce = LAST_REQUEST.get("X-Internal-Nonce");
        client.workspace();
        String secondNonce = LAST_REQUEST.get("X-Internal-Nonce");
        assertThat(firstNonce).isNotEqualTo(secondNonce);
        String expected = hmacSha256(SECRET, "GET\n/api/service/sgw/v1/workspace\n"
                + sha256("") + "\n" + LAST_REQUEST.get("X-Internal-Timestamp") + "\n"
                + secondNonce + "\nmpt");
        assertThat(LAST_REQUEST.get("X-Internal-Signature")).isEqualTo(expected);
    }

    @Test
    @DisplayName("SGW 业务错误保留错误码与 HTTP 状态")
    void errorDecoderPreservesCode() {
        assertThatThrownBy(() -> client.conflict("{}"))
                .isInstanceOfSatisfying(SgwRemoteException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SGW-409001");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("配置版本冲突");
                });
        assertThat(CONFLICT_HITS.get()).isGreaterThan(0);
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

    private static byte[] readAll(InputStream inputStream) throws java.io.IOException {
        return inputStream.readAllBytes();
    }

    /**
     * 本地测试用 SGW 客户端契约。
     */
    interface SgwTestClient {

        @RequestLine("POST /api/service/sgw/v1/routes")
        String create(String body);

        @RequestLine("GET /api/service/sgw/v1/workspace")
        String workspace();

        @RequestLine("POST /api/service/sgw/v1/routes/conflict")
        String conflict(String body);

    }

}
