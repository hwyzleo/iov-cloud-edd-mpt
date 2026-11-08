package net.hwyz.iov.cloud.edd.mpt.service.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * 服务网关错误解码器：保留 SGW 的错误码与 HTTP 状态。
 *
 * @author hwyz_leo
 */
@Slf4j
public class SgwRemoteErrorDecoder implements ErrorDecoder {

    private static final String SGW_API_PREFIX = "/api/service/sgw/";

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    private final ObjectMapper objectMapper;

    public SgwRemoteErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String url = response.request() == null ? "" : response.request().url();
        if (!methodKey.contains("Sgw") && !url.contains(SGW_API_PREFIX)) {
            return defaultDecoder.decode(methodKey, response);
        }
        HttpStatus status = HttpStatus.resolve(response.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String code = "SGW-503001";
        String message = "服务网关调用失败";
        if (response.body() != null) {
            try (java.io.InputStream inputStream = response.body().asInputStream()) {
                byte[] bytes = Util.toByteArray(inputStream);
                JsonNode body = objectMapper.readTree(bytes);
                if (body != null && body.hasNonNull("code")) {
                    code = body.get("code").asText(code);
                }
                if (body != null && body.hasNonNull("message")) {
                    message = body.get("message").asText(message);
                }
            } catch (Exception e) {
                log.warn("解析服务网关错误响应失败: {}", e.getMessage());
            }
        }
        return new SgwRemoteException(code, message, status);
    }

}
