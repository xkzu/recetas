package cl.duoc.app.recetas.controller;

import cl.duoc.app.recetas.config.UrlConfiguration;
import cl.duoc.app.recetas.model.Banner;
import cl.duoc.app.recetas.model.Comentario;
import cl.duoc.app.recetas.model.Receta;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecetaControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UrlConfiguration urlConfiguration;

    @Mock
    private Model model;

    @Mock
    private HttpSession session;

    private RecetaController recetaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mockear las URLs
        when(urlConfiguration.getBackendUrl()).thenReturn("http://localhost:8080");
        when(urlConfiguration.getFrontendUrl()).thenReturn("http://localhost:3000");

        // Inicializar el controlador con URLs mockeadas
        recetaController = new RecetaController(restTemplate, urlConfiguration);
    }

    @Test
    void testLoginPost_SuccessfulLogin() {
        // Mock de respuesta del backend
        Map<String, Object> usuarioMock = new HashMap<>();
        usuarioMock.put("id", 1);
        usuarioMock.put("nombreUsuario", "testUser");
        usuarioMock.put("rol", true);

        Map<String, Object> responseBodyMock = new HashMap<>();
        responseBodyMock.put("token", "testToken");
        responseBodyMock.put("usuario", usuarioMock);

        ResponseEntity<Map> mockResponse = ResponseEntity.ok(responseBodyMock);

        // Configuración del RestTemplate mock
        when(restTemplate.exchange(
                eq("http://localhost:8080/usuario/login"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        // Ejecutar método
        String result = recetaController.loginPost("testUser", "testPassword", model, session);

        // Verificar interacciones y resultados
        verify(session).setAttribute("token", "testToken");
        verify(session).setAttribute("username", "testUser");
        verify(session).setAttribute("rol", true);
        assertEquals("redirect:/inicio", result);
    }

    @Test
    void testLoginPost_InvalidCredentials() {
        // Configurar RestTemplate para lanzar una excepción
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RestClientException("Invalid credentials"));

        // Ejecutar método
        String result = recetaController.loginPost("invalidUser", "invalidPassword", model, session);

        // Verificar interacciones y resultados
        verify(model).addAttribute("error", "Usuario o contraseña incorrectos.");
        assertEquals("login", result);
    }

    @Test
    void testLogin() {
        // Ejecutar el método
        String viewName = recetaController.login();

        // Verificar el resultado
        assertEquals("login", viewName, "Debe retornar el nombre de la vista 'login'");
    }

    @Test
    void testInicio_SuccessfulResponses() {
        // Datos simulados
        List<Receta> mockPopulares = List.of(new Receta(1L, "Receta Popular", null, null, null, null, 0, null, 0, 0, null, null, null, null));
        List<Receta> mockRecientes = List.of(new Receta(2L, "Receta Reciente", null, null, null, null, 0, null, 0, 0, null, null, null, null));
        List<Banner> mockBanners = List.of(new Banner(1L, "Empresa 1", "Mensaje 1", null, null, null));

        // Configurar mocks de RestTemplate
        when(restTemplate.exchange(
                eq("http://localhost:8080/recetas/populares/5"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(mockPopulares));

        when(restTemplate.exchange(
                eq("http://localhost:8080/recetas/recientes/5"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(mockRecientes));

        when(restTemplate.exchange(
                eq("http://localhost:8080/banners/all"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(mockBanners));

        // Simular sesión
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("rol")).thenReturn(true);

        // Ejecutar método
        String viewName = recetaController.inicio(model, session);

        // Verificar resultados
        assertEquals("index", viewName);
        verify(model).addAttribute("recetasPopulares", mockPopulares);
        verify(model).addAttribute("recetasRecientes", mockRecientes);
        verify(model).addAttribute("banners", mockBanners);
        verify(model).addAttribute("isAuthenticated", true);
        verify(model).addAttribute("username", "testUser");
        verify(model).addAttribute("rol", true);
    }

    @Test
    void testInicio_RestClientException() {
        // Configurar RestTemplate para lanzar excepción
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Error al consumir servicio"));

        // Simular sesión
        when(session.getAttribute("username")).thenReturn(null);
        when(session.getAttribute("rol")).thenReturn(null);

        // Ejecutar método
        String viewName = recetaController.inicio(model, session);

        // Verificar resultados
        assertEquals("index", viewName);
        verify(model).addAttribute("recetasPopulares", Collections.emptyList());
        verify(model).addAttribute("recetasRecientes", Collections.emptyList());
        verify(model).addAttribute("banners", Collections.emptyList());
        verify(model).addAttribute("error", "No se pueden cargar algunos datos en este momento. Intente nuevamente más tarde.");
        verify(model).addAttribute("isAuthenticated", false);
        verify(model).addAttribute("username", null);
        verify(model).addAttribute("rol", false);
    }

    @Test
    void testLogout() {
        // Ejecutar el método
        String viewName = recetaController.logout(session);

        // Verificar que la sesión se invalide
        verify(session).invalidate();

        // Verificar que se redirige correctamente
        assertEquals("redirect:/inicio", viewName);
    }

    @Test
    void testVerReceta_SuccessfulResponse() {
        // Configurar sesión simulada
        when(session.getAttribute("token")).thenReturn("validToken");

        // Mock de respuesta del backend
        Receta mockReceta = new Receta(1L, "Receta Ejemplo", null, null, null, null, 0, null, 0, 0, null, null, null, null);
        ResponseEntity<Receta> mockResponse = ResponseEntity.ok(mockReceta);

        when(restTemplate.exchange(
                eq("http://localhost:8080/recetas/detalle/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Receta.class)
        )).thenReturn(mockResponse);

        // Ejecutar el método
        String viewName = recetaController.verReceta(1L, model, session);

        // Verificar resultados
        assertEquals("detalle_receta", viewName);
        verify(model).addAttribute("receta", mockReceta);
        verify(model).addAttribute("backendUrl", "http://localhost:8080/recetas/detalle/1");
        verify(model).addAttribute("frontendUrl", "http://localhost:3000/receta/1");
    }

    @Test
    void testVerReceta_NoToken() {
        // Configurar sesión simulada sin token
        when(session.getAttribute("token")).thenReturn(null);

        // Ejecutar el método
        String viewName = recetaController.verReceta(1L, model, session);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verify(session).setAttribute("redirectAfterLogin", "/receta/1");
    }

    @Test
    void testVerReceta_RestClientException() {
        // Configurar sesión simulada
        when(session.getAttribute("token")).thenReturn("validToken");

        // Configurar RestTemplate para lanzar excepción
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Receta.class)
        )).thenThrow(new RestClientException("Error al consumir servicio"));

        // Ejecutar el método
        String viewName = recetaController.verReceta(1L, model, session);

        // Verificar resultados
        assertEquals("error", viewName);
        verify(model).addAttribute("error", "No se pudo cargar la receta. Intente nuevamente más tarde.");
    }

    @Test
    void testBuscarRecetas_SuccessfulResponse() {
        // Datos simulados
        List<Receta> mockRecetas = List.of(
                new Receta(1L, "Receta 1", null, null, null, null, 0, null, 0, 0, null, null, null, null),
                new Receta(2L, "Receta 2", null, null, null, null, 0, null, 0, 0, null, null, null, null)
        );

        // Mock de respuesta del backend
        ResponseEntity<List<Receta>> mockResponse = ResponseEntity.ok(mockRecetas);

        when(restTemplate.exchange(
                eq("http://localhost:8080/recetas/buscar/any/any/any/any/any"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        // Ejecutar el método
        String viewName = recetaController.buscarRecetas("any", "any", "any", "any", "any", model);

        // Verificar resultados
        assertEquals("buscar_recetas", viewName);
        verify(model).addAttribute("recetas", mockRecetas);
    }

    @Test
    void testBuscarRecetas_RestClientException() {
        // Configurar RestTemplate para lanzar excepción
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Error al consumir servicio"));

        // Ejecutar el método
        String viewName = recetaController.buscarRecetas("any", "any", "any", "any", "any", model);

        // Verificar resultados
        assertEquals("buscar_recetas", viewName);
        verify(model).addAttribute("recetas", Collections.emptyList());
        verify(model).addAttribute("error", "No se pudieron cargar las recetas según los criterios de búsqueda.");
    }

    @Test
    void testMostrarFormularioCrearReceta_TokenPresente() {
        // Configurar sesión con token válido
        when(session.getAttribute("token")).thenReturn("validToken");

        // Ejecutar método
        String viewName = recetaController.mostrarFormularioCrearReceta(session, model);

        // Verificar resultados
        assertEquals("crear_receta", viewName);
        verify(session).getAttribute("token");
    }

    @Test
    void testMostrarFormularioCrearReceta_TokenAusente() {
        // Configurar sesión sin token
        when(session.getAttribute("token")).thenReturn(null);

        // Ejecutar método
        String viewName = recetaController.mostrarFormularioCrearReceta(session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verify(session).getAttribute("token");
    }

    @Test
    void testMostrarFormularioCrearReceta_TokenVacio() {
        // Configurar sesión con token vacío
        when(session.getAttribute("token")).thenReturn("");

        // Ejecutar método
        String viewName = recetaController.mostrarFormularioCrearReceta(session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verify(session).getAttribute("token");
    }

    @Test
    void testRegistrarNuevaReceta_TokenAndUserPresent() {
        // Configurar sesión con token y usuario
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn("testUser");

        // Simular enumeración de atributos
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        // Crear receta de prueba
        Receta nuevaReceta = new Receta();

        // Configurar RestTemplate para retornar una respuesta exitosa
        ResponseEntity<Receta> mockResponse = ResponseEntity.ok(new Receta());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Receta.class)))
                .thenReturn(mockResponse);

        // Ejecutar método
        String viewName = recetaController.registrarNuevaReceta(nuevaReceta, session, model);

        // Verificar resultados
        assertEquals("redirect:/inicio", viewName);
        verify(model, never()).addAttribute(eq("error"), anyString());
    }


    @Test
    void testRegistrarNuevaReceta_TokenMissing() {
        // Configurar sesión sin token
        when(session.getAttribute("token")).thenReturn(null);

        // Simular enumeración vacía para getAttributeNames()
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        // Crear receta de prueba
        Receta nuevaReceta = new Receta();

        // Ejecutar método
        String viewName = recetaController.registrarNuevaReceta(nuevaReceta, session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(restTemplate);
    }


    @Test
    void testRegistrarNuevaReceta_UserMissing() {
        // Configurar sesión con token pero sin usuario
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn(null);

        // Simular enumeración vacía para getAttributeNames()
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        // Crear receta de prueba
        Receta nuevaReceta = new Receta();

        // Ejecutar método
        String viewName = recetaController.registrarNuevaReceta(nuevaReceta, session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(restTemplate);
    }


    @Test
    void testRegistrarNuevaReceta_RestClientException() {
        // Configurar sesión con token y usuario
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn("testUser");

        // Simular enumeración vacía para getAttributeNames()
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        // Crear receta de prueba
        Receta nuevaReceta = new Receta();

        // Configurar RestTemplate para lanzar excepción
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Receta.class)))
                .thenThrow(new RestClientException("Error de conexión"));

        // Ejecutar método
        String viewName = recetaController.registrarNuevaReceta(nuevaReceta, session, model);

        // Verificar resultados
        assertEquals("crear_receta", viewName);
        verify(model).addAttribute("error", "Error de conexión. Intente nuevamente.");
    }


    @Test
    void testRegistrarNuevaReceta_HttpClientErrorException() {
        // Configurar sesión con token y usuario
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn("testUser");

        // Simular enumeración vacía para evitar el NullPointerException
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        // Crear receta de prueba
        Receta nuevaReceta = new Receta();

        // Configurar RestTemplate para lanzar HttpClientErrorException
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Receta.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, // Cambiar a UNAUTHORIZED para simular acceso no autorizado
                        "Unauthorized",
                        new HttpHeaders(),
                        null,
                        null
                ));

        // Ejecutar método
        String viewName = recetaController.registrarNuevaReceta(nuevaReceta, session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verify(model, never()).addAttribute(eq("error"), anyString()); // Asegurarse de que no se agrega un error al modelo
    }

    @Test
    void testMostrarComentarios_TokenPresent() {
        // Configurar sesión con token
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn("testUser");

        // Crear lista de comentarios de prueba
        List<Comentario> comentarios = List.of(
                new Comentario(1L, 101L, 201L, "Comentario 1", LocalDateTime.now(), true),
                new Comentario(2L, 102L, 202L, "Comentario 2", LocalDateTime.now(), true)
        );

        // Simular respuesta del backend
        when(restTemplate.exchange(
                eq("http://localhost:8080/comentario/all"), // Asegúrate de que la URL sea correcta
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class) // Cambiar eq por any para evitar problemas
        )).thenReturn(new ResponseEntity<>(comentarios, HttpStatus.OK));

        // Ejecutar método
        String viewName = recetaController.mostrarComentarios(session, model);

        // Verificar resultados
        assertEquals("comentarios", viewName);
        verify(model).addAttribute("comentarios", comentarios);
        verify(model).addAttribute("isAuthenticated", true);
        verify(model).addAttribute("username", "testUser");
    }


    @Test
    void testMostrarComentarios_TokenMissing() {
        // Configurar sesión sin token
        when(session.getAttribute("token")).thenReturn(null);

        // Ejecutar método
        String viewName = recetaController.mostrarComentarios(session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(restTemplate);
        verifyNoInteractions(model);
    }

    @Test
    void testMostrarComentarios_RestClientException() {
        // Configurar sesión con token
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("username")).thenReturn("testUser");

        // Simular excepción de RestTemplate
        when(restTemplate.exchange(
                eq("http://localhost:8080/comentario/all"), // Asegúrate de que la URL sea correcta
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class) // Cambiar eq por any para evitar problemas
        )).thenThrow(new RestClientException("Error de conexión"));

        // Ejecutar método
        String viewName = recetaController.mostrarComentarios(session, model);

        // Verificar resultados
        assertEquals("comentarios", viewName);
        verify(model).addAttribute("comentarios", Collections.emptyList());
        verify(model).addAttribute("error", "No se pudieron cargar los comentarios. Intente nuevamente más tarde.");
        verify(model).addAttribute("isAuthenticated", true);
        verify(model).addAttribute("username", "testUser");
    }






    @Test
    void testAprobarComentario_Success() {
        // Configurar sesión con token y rol válido
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("rol")).thenReturn(true);

        // Simular respuesta exitosa del backend
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://localhost:8080/comentario/aprobar/1"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(responseEntity);

        // Ejecutar método
        String viewName = recetaController.aprobarComentario(1L, session, model);

        // Verificar resultados
        assertEquals("redirect:/comentarios", viewName);
        verifyNoInteractions(model); // No se debería agregar nada al modelo en este caso
    }

    @Test
    void testAprobarComentario_NoToken() {
        // Configurar sesión sin token
        when(session.getAttribute("token")).thenReturn(null);

        // Ejecutar método
        String viewName = recetaController.aprobarComentario(1L, session, model);

        // Verificar resultados
        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(model); // No se debería agregar nada al modelo
    }

    @Test
    void testAprobarComentario_NoPermission() {
        // Configurar sesión sin permisos
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("rol")).thenReturn(false);

        // Ejecutar método
        String viewName = recetaController.aprobarComentario(1L, session, model);

        // Verificar resultados
        assertEquals("redirect:/comentarios", viewName);
        verify(model).addAttribute("error", "No tienes permisos para aprobar comentarios.");
    }

    @Test
    void testAprobarComentario_Forbidden() {
        // Configurar sesión con token y rol válido
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("rol")).thenReturn(true);

        // Simular respuesta 403 Forbidden
        when(restTemplate.exchange(
                eq("http://localhost:8080/comentario/aprobar/1"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                null,
                null
        ));

        // Ejecutar método
        String viewName = recetaController.aprobarComentario(1L, session, model);

        // Verificar resultados
        assertEquals("redirect:/comentarios", viewName);
        verify(model).addAttribute("error", "No tienes permisos para aprobar este comentario.");
    }

    @Test
    void testAprobarComentario_Exception() {
        // Configurar sesión con token y rol válido
        when(session.getAttribute("token")).thenReturn("validToken");
        when(session.getAttribute("rol")).thenReturn(true);

        // Simular excepción inesperada
        when(restTemplate.exchange(
                eq("http://localhost:8080/comentario/aprobar/1"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(new RestClientException("Error inesperado"));

        // Ejecutar método
        String viewName = recetaController.aprobarComentario(1L, session, model);

        // Verificar resultados
        assertEquals("redirect:/comentarios", viewName);
        verify(model).addAttribute("error", "Ocurrió un error inesperado. Intenta nuevamente.");
    }

}
