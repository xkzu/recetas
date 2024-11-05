package cl.duoc.app.recetas.controller;

import cl.duoc.app.recetas.model.Receta;
import cl.duoc.app.recetas.model.Banner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class RecetaController {

    private final RestTemplate restTemplate;
    private final String backendMainUrl = "http://localhost:8080";

    @Autowired
    public RecetaController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpSession session) {
        String backendPopularesUrl = backendMainUrl + "/recetas/populares/5";
        String backendRecientesUrl = backendMainUrl + "/recetas/recientes/5";
        String backendBannersUrl = backendMainUrl + "/banners/all";

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
            model.addAttribute("error", "No se pueden cargar algunos datos en este momento. Intente nuevamente más tarde.");
        }

        String nombreUsuario = (String) session.getAttribute("username");
        boolean isAuthenticated = (nombreUsuario != null);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("username", nombreUsuario);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPost(@RequestBody Map<String, String> credentials, Model model, HttpSession session) {

        String loginUrl = backendMainUrl + "/usuario/login";

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

            String token = (String) response.getBody().get("token");
            Map<String, Object> usuario = (Map<String, Object>) response.getBody().get("usuario");
            String nombreUsuario = (String) usuario.get("nombreUsuario");

            session.setAttribute("token", token);
            session.setAttribute("username", nombreUsuario);

            return "redirect:/inicio";

        } catch (RestClientException e) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "login";
        }
    }

    @GetMapping("/receta/{id}")
    public String verReceta(@PathVariable Long id, Model model, HttpSession session) {
        String token = (String) session.getAttribute("token");
        if (token == null || token.isEmpty()) {
            session.setAttribute("redirectAfterLogin", "/receta/" + id);
            return "redirect:/login";
        }
        String backendUrl = backendMainUrl + "/recetas/detalle/" + id;

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
            model.addAttribute("error", "No se pudo cargar la receta. Intente nuevamente más tarde.");
            return "error";
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
                backendMainUrl,
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
            model.addAttribute("error", "No se pudieron cargar las recetas según los criterios de búsqueda.");
        }

        return "buscar_recetas";
    }

    private String encodeParam(String param) {
        return param.replace(" ", "%20");
    }
}
