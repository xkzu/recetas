package cl.duoc.app.recetas.controller;

import cl.duoc.app.recetas.model.Receta;
import cl.duoc.app.recetas.model.Banner;
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
import java.util.List;
import java.util.Map;

@Controller
public class RecetaController {

    private final RestTemplate restTemplate;

    private static final String BACKEND_MAIN_URL = "http://localhost:8080";

    private static final String ERROR = "error";

    private static final String USERNAME = "username";

    private static final String TOKEN = "token";

    @Autowired
    public RecetaController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpSession session) {
        String backendPopularesUrl = BACKEND_MAIN_URL + "/recetas/populares/5";
        String backendRecientesUrl = BACKEND_MAIN_URL + "/recetas/recientes/5";
        String backendBannersUrl = BACKEND_MAIN_URL + "/banners/all";

        try {
            ResponseEntity<List<Receta>> popularesResponse = restTemplate.exchange(
                    backendPopularesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            ResponseEntity<List<Receta>> recientesResponse = restTemplate.exchange(
                    backendRecientesUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            ResponseEntity<List<Banner>> bannersResponse = restTemplate.exchange(
                    backendBannersUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Banner>>() {}
            );

            model.addAttribute("recetasPopulares", popularesResponse.getBody());
            model.addAttribute("recetasRecientes", recientesResponse.getBody());
            model.addAttribute("banners", bannersResponse.getBody());
        } catch (RestClientException e) {
            model.addAttribute("recetasPopulares", Collections.emptyList());
            model.addAttribute("recetasRecientes", Collections.emptyList());
            model.addAttribute("banners", Collections.emptyList());
            model.addAttribute(ERROR, "No se pueden cargar algunos datos en este momento. Intente nuevamente más tarde.");
        }

        String nombreUsuario = (String) session.getAttribute(USERNAME);
        boolean isAuthenticated = (nombreUsuario != null);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute(USERNAME, nombreUsuario);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(@RequestBody Map<String, String> credentials, Model model, HttpSession session) {

        String loginUrl = BACKEND_MAIN_URL + "/usuario/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            String token = (String) response.getBody().get(TOKEN);
            Map<String, Object> usuario = (Map<String, Object>) response.getBody().get("usuario");
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
            return "redirect:/login";
        }
        String backendUrl = BACKEND_MAIN_URL + "/recetas/detalle/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Receta> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.GET,
                    request,
                    Receta.class
            );

            model.addAttribute("receta", response.getBody());
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

        String backendUrl = String.format("%s/recetas/buscar/%s/%s/%s/%s/%s",
                BACKEND_MAIN_URL,
                encodeParam(nombre),
                encodeParam(tipoCocina),
                encodeParam(ingredientes),
                encodeParam(paisOrigen),
                encodeParam(dificultad)
        );

        try {
            ResponseEntity<List<Receta>> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Receta>>() {}
            );
            model.addAttribute("recetas", response.getBody());
        } catch (RestClientException e) {
            model.addAttribute("recetas", Collections.emptyList());
            model.addAttribute(ERROR, "No se pudieron cargar las recetas según los criterios de búsqueda.");
        }

        return "buscar_recetas";
    }

    @PostMapping("/recetas/nueva")
    public String registrarNuevaReceta(@ModelAttribute Receta nuevaReceta, HttpSession session, Model model) {
        // Recupera el token de la sesión
        String token = (String) session.getAttribute(TOKEN);
        if (token == null || token.isEmpty()) {
            return "redirect:/login"; // Redirige al login si el token no está presente
        }

        // Recupera el usuario de la sesión y extrae el idUsuario
        @SuppressWarnings("unchecked")
        Map<String, Object> usuario = (Map<String, Object>) session.getAttribute("usuario");
        if (usuario == null || usuario.get("id") == null) {
            return "redirect:/login"; // Redirige al login si el usuario no está presente o no tiene un id válido
        }
        // Asigna el idUsuario a la receta
        nuevaReceta.setIdUsuario(((Number) usuario.get("id")).longValue());

        // URL del backend para registrar la receta
        String backendUrl = BACKEND_MAIN_URL + "/recetas/register";

        // Configura los encabezados HTTP con el token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Crea la entidad de solicitud con la receta y los encabezados
        HttpEntity<Receta> request = new HttpEntity<>(nuevaReceta, headers);

        try {
            // Llama al backend para registrar la receta
            ResponseEntity<Receta> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.POST,
                    request,
                    Receta.class
            );

            // Maneja respuestas exitosas
            if (response.getStatusCode().is2xxSuccessful()) {
                return "redirect:/inicio";
            }
            // Redirige al login si la respuesta es no autorizada
            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
                return "redirect:/login";
            }

            // Manejo de errores generales de respuesta
            model.addAttribute(ERROR, "Error al registrar la receta. Respuesta del servidor: " + response.getStatusCode());
            return "crear_receta";

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Manejo específico de errores del cliente o servidor, incluyendo redirección al login si es necesario
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return "redirect:/login";
            }
            model.addAttribute(ERROR, "Error al registrar la receta. Respuesta del servidor: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return "crear_receta";
        } catch (RestClientException e) {
            // Manejo de errores de conexión
            model.addAttribute(ERROR, "Error de conexión. Intente nuevamente.");
            return "crear_receta";
        } catch (Exception e) {
            // Manejo de excepciones generales
            model.addAttribute(ERROR, "Ocurrió un error inesperado. Intente nuevamente.");
            return "crear_receta";
        }
    }



    @GetMapping("/recetas/nueva")
    public String mostrarFormularioCrearReceta(HttpSession session, Model model) {
        String token = (String) session.getAttribute(TOKEN);
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        return "crear_receta";
    }

    private String encodeParam(String param) {
        return param.replace(" ", "%20");
    }
}
