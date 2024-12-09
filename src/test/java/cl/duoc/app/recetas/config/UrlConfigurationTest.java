package cl.duoc.app.recetas.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class UrlConfigurationTest {

    private UrlConfiguration urlConfiguration;

    @BeforeEach
    void setUp() {
        urlConfiguration = new UrlConfiguration();
        ReflectionTestUtils.setField(urlConfiguration, "backendPort", "8081");
        ReflectionTestUtils.setField(urlConfiguration, "frontendPort", "8080");
    }

    @Test
    void testInit_Success() {
        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            // Arrange
            String mockHostAddress = "127.0.0.1";
            mockedInetAddress.when(InetAddress::getLocalHost)
                    .thenReturn(Mockito.mock(InetAddress.class));
            mockedInetAddress.when(() -> InetAddress.getLocalHost().getHostAddress())
                    .thenReturn(mockHostAddress);

            // Act
            urlConfiguration.init();

            // Assert
            String expectedBackendUrl = "http://" + mockHostAddress + ":8081";
            String expectedFrontendUrl = "http://" + mockHostAddress + ":8080";

            assertEquals(expectedBackendUrl, urlConfiguration.getBackendUrl(), "La URL del backend no es correcta.");
            assertEquals(expectedFrontendUrl, urlConfiguration.getFrontendUrl(), "La URL del frontend no es correcta.");
        }
    }

    @Test
    void testInit_UnknownHostException() {
        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            // Arrange
            mockedInetAddress.when(InetAddress::getLocalHost)
                    .thenThrow(new UnknownHostException("Host desconocido"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, urlConfiguration::init, "Debería lanzarse una excepción.");
            assertTrue(exception.getMessage().contains("No se pudo obtener la dirección del host"), "El mensaje de la excepción no es el esperado.");
        }
    }
}
