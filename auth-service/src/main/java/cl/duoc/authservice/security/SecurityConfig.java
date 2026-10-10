package cl.duoc.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${internal.api-key}") String internalApiKey) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        // Endpoints internos: protegidos con X-Internal-Api-Key
                        // (los consume notification-service).
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                // El filtro corre antes de la autorización: si no llega la
                // cabecera X-Internal-Api-Key, responde 401 y corta.
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
}
