package cl.duoc.app.recetas.interceptor;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestInterceptorTest {

    @Mock
    private HttpSession session;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private ClientHttpRequestExecution execution;

    private JwtRequestInterceptor jwtRequestInterceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtRequestInterceptor = new JwtRequestInterceptor(session);
    }

    @Test
    void testIntercept_AddsAuthorizationHeader_WhenTokenIsPresent() throws IOException {
        // Arrange
        String testToken = "testToken123";
        when(session.getAttribute("token")).thenReturn(testToken);
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);

        // Act
        jwtRequestInterceptor.intercept(httpRequest, new byte[0], execution);

        // Assert
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION), "El encabezado 'Authorization' debería estar presente.");
        assertEquals("Bearer " + testToken, headers.getFirst(HttpHeaders.AUTHORIZATION), "El token debería estar correctamente formateado.");
    }

    @Test
    void testIntercept_DoesNotAddAuthorizationHeader_WhenTokenIsAbsent() throws IOException {
        // Arrange
        when(session.getAttribute("token")).thenReturn(null);
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);

        // Act
        jwtRequestInterceptor.intercept(httpRequest, new byte[0], execution);

        // Assert
        assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION), "El encabezado 'Authorization' no debería estar presente.");
    }

    @Test
    void testIntercept_DoesNotAddAuthorizationHeader_WhenTokenIsEmpty() throws IOException {
        // Arrange
        when(session.getAttribute("token")).thenReturn(" ");
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);

        // Act
        jwtRequestInterceptor.intercept(httpRequest, new byte[0], execution);

        // Assert
        assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION), "El encabezado 'Authorization' no debería estar presente para un token vacío.");
    }

    @Test
    void testIntercept_ExecutesRequest() throws IOException {
        // Arrange
        when(session.getAttribute("token")).thenReturn(null);
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);

        // Act
        jwtRequestInterceptor.intercept(httpRequest, new byte[0], execution);

        // Assert
        verify(execution, times(1)).execute(httpRequest, new byte[0]);
    }

    @Test
    void testIntercept_HandlesException_WhenRetrievingToken() throws IOException {
        // Arrange
        when(session.getAttribute("token")).thenThrow(new RuntimeException("Error al obtener el token"));
        HttpHeaders headers = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(headers);

        // Act & Assert
        assertDoesNotThrow(() -> jwtRequestInterceptor.intercept(httpRequest, new byte[0], execution), "No debería lanzarse una excepción.");
        verify(execution, times(1)).execute(httpRequest, new byte[0]);
    }
}
