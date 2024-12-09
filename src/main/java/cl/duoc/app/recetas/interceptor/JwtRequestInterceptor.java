package cl.duoc.app.recetas.interceptor;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class JwtRequestInterceptor implements ClientHttpRequestInterceptor {

    private final HttpSession session;

    public JwtRequestInterceptor(HttpSession session) {
        this.session = session;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        try {
            String token = (String) session.getAttribute("token");
            if (token != null && !token.trim().isEmpty()) {
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        } catch (Exception e) {
            // Manejo de excepción: registrar el error sin interrumpir la ejecución
            System.err.println("Error al obtener el token de la sesión: " + e.getMessage());
        }
        return execution.execute(request, body);
    }
}
