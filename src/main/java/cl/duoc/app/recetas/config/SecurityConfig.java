package cl.duoc.app.recetas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/login", "/usuario/login"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/inicio", "/buscar", "/login", "/logout", "/css/**", "/images/**").permitAll()
                        .requestMatchers("/receta/**").permitAll()
                        .requestMatchers("/comentarios", "/comentarios/aprobar/**", "/recetas/nueva").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin().disable()
                .logout(logout -> logout
                        .logoutSuccessUrl("/inicio")
                        .permitAll()
                );
        return http.build();
    }

}
