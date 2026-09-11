package com.lezztto.LmfBank.config;

import com.lezztto.LmfBank.auth.filter.JwtAuthenticationFilter;
import com.lezztto.LmfBank.auth.security.CustomAuthenticationEntryPoint;
import com.lezztto.LmfBank.exception.handler.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("java:S4502") // CSRF desabilitado de propósito — ver comentário abaixo
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // CSRF protection is not needed: this is a stateless API whose only
                // credential is a signed JWT sent in the `Authorization: Bearer` header.
                // Browsers never attach that header automatically to cross-site requests,
                // and the backend reads no session cookie or HTTP Basic credential, so
                // there is no ambient authority for a CSRF attack to ride on. The
                // httpOnly cookie in ADR 0007 lives only between the browser and the Next
                // BFF; the backend still only ever sees a Bearer header. This MUST be
                // revisited if cookie- or session-based authentication is ever added.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/actuator/**"
                        ).permitAll()
                        // Estático — só decide por padrão de URL. A checagem por conta
                        // (ADR 0010) é dinâmica e vive no AccountAccessGuard.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}