package cl.duoc.app.recetas.config;

import cl.duoc.app.recetas.interceptor.JwtRequestInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    private final HttpSession session;

    public RestTemplateConfig(HttpSession session) {
        this.session = session;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configurar el interceptor para agregar el token JWT
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new JwtRequestInterceptor(session));
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }
}
