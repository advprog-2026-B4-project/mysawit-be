package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = R2PropertiesTest.TestConfig.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@TestPropertySource(properties = {
        "app.storage.r2.endpoint=https://test.r2.cloudflarestorage.com",
        "app.storage.r2.bucket=test-bucket",
        "app.storage.r2.access-key=test-access",
        "app.storage.r2.secret-key=test-secret",
        "app.storage.r2.public-url=https://pub-test.r2.dev"
})
class R2PropertiesTest {

    @EnableConfigurationProperties(R2Properties.class)
    static class TestConfig {}

    @Autowired
    private R2Properties properties;

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
