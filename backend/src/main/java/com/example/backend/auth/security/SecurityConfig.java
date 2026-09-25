package com.example.backend.auth.security;

import com.example.backend.auth.service.UsersServices;
import com.example.backend.auth.jwt.JWTFilter;
import lombok.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;
    private final UsersServices userService;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final com.example.backend.auth.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
    private final com.example.backend.auth.security.oauth2.CustomOAuth2UserService customOAuth2UserService;
    private final com.example.backend.auth.security.oauth2.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final com.example.backend.auth.security.oauth2.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) {
       return http
                // V06 Fix: Remove narrow securityMatcher("/api/**") so all application routes default to secure
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // V01 Fix: Replace overly permissive wildcard "/api/v1/auth/**" with explicit public endpoints
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/oauth2/**",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        // V01 & V06 Fix: Restrict development and test utilities to ADMIN only
                        .requestMatchers("/api/v1/auth/dev/**", "/test-email", "/test-email/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
               .build();
    }

    // Authentication provider for JWT login
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    // Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // Authentication manager
    @Bean
    public AuthenticationManager authenticationManager()  {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
