package cl.duoc.app.recetas.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateToken() {
        String username = "testUser";
        String token = jwtUtil.generateToken(username);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        String username = "testUser";
        String token = jwtUtil.generateToken(username);
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    void testExtractExpiration() {
        String username = "testUser";
        String token = jwtUtil.generateToken(username);
        assertNotNull(jwtUtil.extractExpiration(token));
    }

    @Test
    void testValidateToken_ValidToken() {
        String username = "testUser";
        String token = jwtUtil.generateToken(username);
        assertTrue(jwtUtil.validateToken(token, username));
    }

    @Test
    void testValidateToken_InvalidUsername() {
        String username = "testUser";
        String token = jwtUtil.generateToken(username);
        assertFalse(jwtUtil.validateToken(token, "invalidUser"));
    }

    @Test
    void testValidateToken_ExpiredToken() throws InterruptedException {
        String username = "testUser";
        String token = jwtUtil.generateToken(username, 1000); // Expira en 1 segundo
        Thread.sleep(2000); // Espera 2 segundos
        assertFalse(jwtUtil.validateToken(token, username));
    }

    @Test
    void testExtractClaim_ExpiredToken() {
        // Arrange: Genera un token con una expiración pasada
        String expiredToken = Jwts.builder()
                .setSubject(null)
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1 segundo en el pasado
                .signWith(jwtUtil.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Act: Extraer un claim del token expirado
        String extractedUsername = jwtUtil.extractClaim(expiredToken, Claims::getSubject);

        // Assert: Verifica que el resultado sea null
        assertNull(extractedUsername, "El resultado debe ser null para un token expirado.");
    }


}
