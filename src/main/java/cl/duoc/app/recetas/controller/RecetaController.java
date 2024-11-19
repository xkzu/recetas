package cl.duoc.app.recetas.controller;

import cl.duoc.app.recetas.config.UrlConfiguration;
import cl.duoc.app.recetas.model.Receta;
import cl.duoc.app.recetas.model.Banner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class RecetaController {

    private final RestTemplate restTemplate;

    private final String backendMainUrl;

    private final String frontendMainUrl;

    private static final String ERROR = "error";

    private static final String USERNAME = "username";

    private static final String TOKEN = "token";

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private static final String CREAR_RECETA = "crear_receta";

    private String backendUrl = null;

    private int idUsuario = 0;

    @Autowired
    public RecetaController(RestTemplate restTemplate, UrlConfiguration urlConfiguration) {
        this.restTemplate = restTemplate;
        this.backendMainUrl = urlConfiguration.getBackendUrl();
        this.frontendMainUrl = urlConfiguration.getFrontendUrl();
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpSession session) {
        String backendPopularesUrl = backendMainUrl + "/recetas/populares/5";
        log.info("Backend populares url: " + backendPopularesUrl);

        String backendRecientesUrl = backendMainUrl + "/recetas/recientes/5";
        log.info("Backend recientes url: " + backendRecientesUrl);

        String backendBannersUrl = backendMainUrl + "/banners/all";
        log.info("Backend banners url: " + backendBannersUrl);

        try {
            ResponseEntity<List<Receta>> popularesResponse = restTemplate.exchange(
                    backendPopularesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            log.info("Recetas populadas: {}", popularesResponse.getBody());

            ResponseEntity<List<Receta>> recientesResponse = restTemplate.exchange(
                    backendRecientesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            log.info("Recetas recientes: {}", recientesResponse.getBody());

            ResponseEntity<List<Banner>> bannersResponse = restTemplate.exchange(
                    backendBannersUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Banner>>() {}
            );
            log.info("Recetas banners: {}", bannersResponse.getBody());

            model.addAttribute("recetasPopulares", popularesResponse.getBody());
            model.addAttribute("recetasRecientes", recientesResponse.getBody());
            model.addAttribute("banners", bannersResponse.getBody());
        } catch (RestClientException e) {
            model.addAttribute("recetasPopulares", Collections.emptyList());
            model.addAttribute("recetasRecientes", Collections.emptyList());
            model.addAttribute("banners", Collections.emptyList());
            model.addAttribute(ERROR, "No se pueden cargar algunos datos en este momento. Intente nuevamente más tarde.");
        }

        String nombreUsuarioSession = (String) session.getAttribute(USERNAME);
        boolean isAuthenticated = (nombreUsuarioSession != null);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute(USERNAME, nombreUsuarioSession);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(@RequestBody Map<String, String> credentials, Model model, HttpSession session) {

        String loginUrl = backendMainUrl + "/usuario/login";
        log.info("Recetas login url: {}", loginUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);
        log.info("Recetas login request: {}", request);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            log.info("Recetas login response: {}", response);

            String token = (String) response.getBody().get(TOKEN);
            Map<String, Object> usuario = (Map<String, Object>) response.getBody().get("usuario");
            idUsuario = (Integer) usuario.get("id");
            String nombreUsuario = (String) usuario.get("nombreUsuario");

            session.setAttribute(TOKEN, token);
            session.setAttribute(USERNAME, nombreUsuario);

            return "redirect:/inicio";

        } catch (RestClientException e) {
            model.addAttribute(ERROR, "Usuario o contraseña incorrectos.");
            return "login";
        }
    }

    @GetMapping("/receta/{id}")
    public String verReceta(@PathVariable Long id, Model model, HttpSession session) {
        String token = (String) session.getAttribute(TOKEN);
        if (token == null || token.isEmpty()) {
            session.setAttribute("redirectAfterLogin", "/receta/" + id);
            return REDIRECT_LOGIN;
        }

        backendUrl = backendMainUrl + "/recetas/detalle/" + id;
        log.info("Recetas ver url: " + backendUrl);

        String frontendUrl = frontendMainUrl + "/receta/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        log.info("Recetas ver request: {}", request);

        try {
            ResponseEntity<Receta> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.GET,
                    request,
                    Receta.class
            );
            log.info("Recetas ver response: {}", response);

            model.addAttribute("receta", response.getBody());
            model.addAttribute("backendUrl", backendUrl);
            model.addAttribute("frontendUrl", frontendUrl);
            return "detalle_receta";

        } catch (RestClientException e) {
            model.addAttribute(ERROR, "No se pudo cargar la receta. Intente nuevamente más tarde.");
            return ERROR;
        }
    }


    @GetMapping("/buscar")
    public String buscarRecetas(
            @RequestParam(defaultValue = "any") String nombre,
            @RequestParam(defaultValue = "any") String tipoCocina,
            @RequestParam(defaultValue = "any") String ingredientes,
            @RequestParam(defaultValue = "any") String paisOrigen,
            @RequestParam(defaultValue = "any") String dificultad,
            Model model) {

        backendUrl = String.format("%s/recetas/buscar/%s/%s/%s/%s/%s",
                backendMainUrl,
                encodeParam(nombre),
                encodeParam(tipoCocina),
                encodeParam(ingredientes),
                encodeParam(paisOrigen),
                encodeParam(dificultad)
        );
        log.info("Recetas buscar url: {}", backendUrl);

        try {
            ResponseEntity<List<Receta>> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            log.info("Recetas buscar response: {}", response);

            model.addAttribute("recetas", response.getBody());
        } catch (RestClientException e) {
            model.addAttribute("recetas", Collections.emptyList());
            model.addAttribute(ERROR, "No se pudieron cargar las recetas según los criterios de búsqueda.");
        }

        return "buscar_recetas";
    }

    @PostMapping("/recetas/nueva")
    public String registrarNuevaReceta(@ModelAttribute Receta nuevaReceta, HttpSession session, Model model) {

        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            log.info("Atributo de sesión: {} = {}", attributeName, attributeValue);
        }

        log.info("session: {}", session);
        // Recupera el token de la sesión
        String token = (String) session.getAttribute(TOKEN);
        log.info("Recetas registrar token: {}", token);

        if (token == null || token.isEmpty()) {
            return REDIRECT_LOGIN; // Redirige al login si el token no está presente
        }

        // Recupera el usuario de la sesión y extrae el idUsuario
        String usuario = (String) session.getAttribute(USERNAME);
        log.info("Recetas registrar username: {}", usuario);

        if (usuario == null || usuario.isEmpty()) {
            return REDIRECT_LOGIN; // Redirige al login si el usuario no está presente
        }

        // Asigna el idUsuario a la receta
        nuevaReceta.setIdUsuario((long) idUsuario);

        // URL del backend para registrar la receta
        backendUrl = backendMainUrl + "/recetas/register";
        log.info("Recetas registrar url: {}", backendUrl);

        // Configura los encabezados HTTP con el token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Crea la entidad de solicitud con la receta y los encabezados
        HttpEntity<Receta> request = new HttpEntity<>(nuevaReceta, headers);
        log.info("Recetas registrar request: {}", request);

        try {
            // Llama al backend para registrar la receta
            ResponseEntity<Receta> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.POST,
                    request,
                    Receta.class
            );
            log.info("Recetas registrar response: {}", response);

            // Maneja respuestas exitosas
            if (response.getStatusCode().is2xxSuccessful()) {
                return "redirect:/inicio";
            }
            // Redirige al login si la respuesta es no autorizada
            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
                return REDIRECT_LOGIN;
            }

            // Manejo de errores generales de respuesta
            model.addAttribute(ERROR, "Error al registrar la receta. Respuesta del servidor: " + response.getStatusCode());
            return CREAR_RECETA;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Manejo específico de errores del cliente o servidor, incluyendo redirección al login si es necesario
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return REDIRECT_LOGIN;
            }
            model.addAttribute(ERROR, "Error al registrar la receta. Respuesta del servidor: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return CREAR_RECETA;
        } catch (RestClientException e) {
            // Manejo de errores de conexión
            model.addAttribute(ERROR, "Error de conexión. Intente nuevamente.");
            return CREAR_RECETA;
        } catch (Exception e) {
            // Manejo de excepciones generales
            model.addAttribute(ERROR, "Ocurrió un error inesperado. Intente nuevamente.");
            return CREAR_RECETA;
        }
    }

    @GetMapping("/recetas/nueva")
    public String mostrarFormularioCrearReceta(HttpSession session, Model model) {
        String token = (String) session.getAttribute(TOKEN);
        log.info("Recetas mostrar token: {}", token);

        if (token == null || token.isEmpty()) {
            return REDIRECT_LOGIN;
        }
        return CREAR_RECETA;
    }

    private String encodeParam(String param) {
        return param.replace(" ", "%20");
    }
}
