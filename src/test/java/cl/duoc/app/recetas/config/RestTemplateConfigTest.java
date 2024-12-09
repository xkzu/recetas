package cl.duoc.app.recetas.config;

import cl.duoc.app.recetas.interceptor.JwtRequestInterceptor;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RestTemplateConfigTest {

    @Mock
    private HttpSession session;

    private RestTemplateConfig restTemplateConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restTemplateConfig = new RestTemplateConfig(session);
    }

    @Test
    void testRestTemplate_NotNull() {
        // Act
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // Assert
        assertNotNull(restTemplate, "El RestTemplate no debe ser nulo.");
        assertEquals(1, restTemplate.getInterceptors().size(), "Debe haber exactamente un interceptor configurado.");
        assertTrue(
                restTemplate.getInterceptors().get(0) instanceof JwtRequestInterceptor,
                "El interceptor debe ser una instancia de JwtRequestInterceptor."
        );
    }

    @Test
    void testRestTemplate_InterceptorConfiguredCorrectly() {
        // Arrange
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // Act
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        // Assert
        assertNotNull(interceptor, "El interceptor configurado no debe ser nulo.");
        assertTrue(interceptor instanceof JwtRequestInterceptor, "El interceptor debe ser del tipo JwtRequestInterceptor.");
    }
}
