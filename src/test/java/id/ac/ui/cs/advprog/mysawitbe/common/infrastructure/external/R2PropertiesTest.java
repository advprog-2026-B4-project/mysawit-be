package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class R2PropertiesTest {

    private final R2Properties properties = new R2Properties() {{
        setEndpoint("https://test.r2.cloudflarestorage.com");
        setBucket("test-bucket");
        setAccessKey("test-access");
        setSecretKey("test-secret");
        setPublicUrl("https://pub-test.r2.dev");
    }};

    @Test
    void r2Properties_bindsAllFields() {
        assertThat(properties.getEndpoint()).isEqualTo("https://test.r2.cloudflarestorage.com");
        assertThat(properties.getBucket()).isEqualTo("test-bucket");
        assertThat(properties.getAccessKey()).isEqualTo("test-access");
        assertThat(properties.getSecretKey()).isEqualTo("test-secret");
        assertThat(properties.getPublicUrl()).isEqualTo("https://pub-test.r2.dev");
    }

    @Test
    void r2Properties_isDataClassWithGetters() {
        assertThat(properties).isNotNull();
        assertThat(properties.getEndpoint()).isNotNull();
    }
}
