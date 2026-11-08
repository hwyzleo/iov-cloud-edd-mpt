package net.hwyz.iov.cloud.edd.mpt.service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SgwInternalProperties} 启动期校验测试。
 *
 * @author hwyz_leo
 */
class SgwInternalPropertiesTest {

    @Test
    void shouldFailFastWhenSecretMissing() {
        SgwInternalProperties properties = new SgwInternalProperties();
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mpt.sgw.internal.secret");
    }

    @Test
    void shouldFailFastWhenSecretBlank() {
        SgwInternalProperties properties = new SgwInternalProperties();
        properties.setSecret("   ");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SGW_INTERNAL_SECRET");
    }

    @Test
    void shouldPassWhenSecretConfigured() {
        SgwInternalProperties properties = new SgwInternalProperties();
        properties.setSecret("test-secret");
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldSkipValidationWhenDisabled() {
        SgwInternalProperties properties = new SgwInternalProperties();
        properties.setEnabled(false);
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

}
