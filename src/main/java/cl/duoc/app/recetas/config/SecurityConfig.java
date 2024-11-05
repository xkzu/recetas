package cl.duoc.app.recetas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/inicio", "/buscar", "/login", "/css/**", "/images/**", "/usuario/login").permitAll()
                        .requestMatchers("/receta/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf().disable()
                .formLogin().disable()
                .logout((logout) -> logout
                        .logoutSuccessUrl("/inicio")
                        .permitAll()
                );

        return http.build();
    }
}
