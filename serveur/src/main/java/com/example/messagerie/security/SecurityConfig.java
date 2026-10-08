package com.example.messagerie.security;

import com.example.messagerie.exception.ErreurResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Configuration de securite (etape 3) :
 * - seuls POST /api/comptes et POST /api/auth/login sont publics ;
 * - la documentation Swagger (/swagger-ui.html, /v3/api-docs) est publique (etape 6) ;
 * - tout le reste exige un jeton JWT valide, sinon 401 au format JSON du contrat ;
 * - /ws/** est laisse passer ici : le jeton y est verifie par JwtHandshakeInterceptor ;
 * - CORS autorise les pages des deux clients (en local et sur le reseau Wi-Fi de la demo).
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    /** Sert a hacher les mots de passe avant de les stocker. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comptes", "/api/auth/login").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Documentation Swagger (etape 6) : page et description de l'API accessibles sans jeton
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(this::refuserSansJeton))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Reponse 401 au meme format JSON que les autres erreurs : {status, erreur, message, date}. */
    private void refuserSansJeton(HttpServletRequest requete,
                                  HttpServletResponse reponse,
                                  AuthenticationException exception) throws IOException {
        HttpStatus statut = HttpStatus.UNAUTHORIZED;
        ErreurResponse corps = new ErreurResponse(
                statut.value(),
                statut.getReasonPhrase(),
                "Jeton absent, invalide ou expire",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        reponse.setStatus(statut.value());
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(reponse.getWriter(), corps);
    }

    /**
     * CORS : autorise les pages web des clients a appeler l'API.
     * Origines acceptees : localhost / 127.0.0.1 (tests sur un seul PC)
     * et les adresses de reseau local (Wi-Fi ou partage de connexion le jour de la demo), sur tous les ports.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://192.168.*:[*]",
                "http://172.*:[*]",
                "http://10.*:[*]"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}