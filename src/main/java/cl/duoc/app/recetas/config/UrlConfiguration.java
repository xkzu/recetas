package cl.duoc.app.recetas.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class UrlConfiguration {


    private String backendUrl;

    private String frontendUrl;

    @Value("${backend.port}") //se toma el puerto del backend desde el properties
    private String backendPort;

    @Value("${server.port}")
    private String frontendPort;

    @PostConstruct
    public void init() {
        try {
            // Obtiene el hostname automáticamente
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String bport = backendPort;
            String fport = frontendPort;

            // Construye la URL del backend con el protocolo HTTP y la dirección obtenida
            backendUrl = "http://" + hostAddress + ":" + bport;
            frontendUrl = "http://" + hostAddress + ":" + fport;
        } catch (UnknownHostException e) {
            throw new RuntimeException("No se pudo obtener la dirección del host", e);
        }
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }
}