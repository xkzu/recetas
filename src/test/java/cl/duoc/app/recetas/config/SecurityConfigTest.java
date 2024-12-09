//package cl.duoc.app.recetas.config;
//
//import cl.duoc.app.recetas.config.JwtAuthenticationFilter;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest
//class SecurityConfigTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @BeforeEach
//    void setUp() {
//        // Puedes agregar configuraciones iniciales aquí si es necesario
//    }
//
//    @Test
//    void testPublicEndpointsAccessibleWithoutAuthentication() throws Exception {
//        mockMvc.perform(get("/inicio"))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(get("/login"))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(get("/buscar"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    @WithMockUser
//    void testAuthenticatedEndpointsWithAuthentication() throws Exception {
//        mockMvc.perform(get("/recetas/nueva").with(csrf()))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testProtectedEndpointsWithoutAuthentication() throws Exception {
//        mockMvc.perform(get("/comentarios"))
//                .andExpect(status().isForbidden());
//    }
//}
