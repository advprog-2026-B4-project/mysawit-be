package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

@SpringBootTest(classes = {CorsConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000,https://mysawit.com"
})
class CorsConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Nested
    class BeanWiring {

        @Test
        void corsConfig_beanIsCreated() {
            assertThat(corsConfigurationSource).isNotNull();
        }

        @Test
        void corsConfig_returnsConfigurationForApiPaths() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");

            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config).isNotNull();
        }

        @Test
        void corsConfig_returnsNullForNonApiPaths() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/other/path");

            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config).isNull();
        }
    }

    @Nested
    class ConfigurationValues {

        @Test
        void corsConfig_allowedOrigins_containsConfiguredOrigins() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");
            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config.getAllowedOrigins())
                    .contains("http://localhost:3000", "https://mysawit.com");
        }

        @Test
        void corsConfig_allowedMethods_containsStandardMethods() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");
            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config.getAllowedMethods())
                    .contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        }

        @Test
        void corsConfig_allowedHeaders_allowsAll() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");
            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config.getAllowedHeaders()).contains("*");
        }

        @Test
        void corsConfig_allowCredentials_isFalse() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");
            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config.getAllowCredentials()).isFalse();
        }

        @Test
        void corsConfig_maxAge_is3600() {
            HttpServletRequest request = new MockHttpServletRequest("GET", "/api/panen");
            CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

            assertThat(config.getMaxAge()).isEqualTo(3600L);
        }
    }
}
