package com.tibet.tourism.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final String[] API_DOCS_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/webjars/**"
    };

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private CsrfCookieFilter csrfCookieFilter;

    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;

    @Value("${app.security.public-metrics-enabled:false}")
    private boolean publicMetricsEnabled;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
   
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            // Browser cookie authentication is protected by CsrfCookieFilter with signed double-submit tokens.
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (publicDocsEnabled) {
                    auth.requestMatchers(API_DOCS_PATHS).permitAll();
                }
                if (publicMetricsEnabled) {
                    auth.requestMatchers("/actuator/prometheus").permitAll();
                } else {
                    auth.requestMatchers("/actuator/prometheus").hasRole("ADMIN");
                }
                auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.TRACE, "/**").denyAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/info").hasRole("ADMIN")
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/register").permitAll()
                    .requestMatchers("/api/auth/logout").permitAll()
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/auth/me/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/payments/callbacks/**").permitAll()
                    .requestMatchers("/api/orders/**").authenticated()
                    .requestMatchers("/api/spots/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/spots/recommendations/**").authenticated()
                    .requestMatchers("/api/spots/companion-type").authenticated()
                    .requestMatchers("/api/spots/user/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/spots/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/spots").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/spots/search").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/spots/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/spots/*/similar").permitAll()
                    .requestMatchers("/api/news/**").permitAll()
                    .requestMatchers("/api/heritage/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tibet-specialty/culture-tips").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tibet-specialty/phrasebook").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tibet-specialty/sustainable-options").permitAll()
                    .requestMatchers("/api/tibet-specialty/**").authenticated()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/api/itineraries/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/routes/generate").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/routes/generate/**").authenticated()
                    // 分享路线相关的GET请求允许匿名访问（必须在 /api/routes/** 之前）
                    .requestMatchers(HttpMethod.GET, "/api/routes/shared").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/routes/shared/**").permitAll()
                    // 分享路线相关的POST/DELETE请求需要认证
                    .requestMatchers(HttpMethod.POST, "/api/routes/share").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/routes/shared/*/like").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/routes/shared/*/like").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/routes/shared/*/like-status").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/routes/shared/*/comments").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/routes/shared/*").authenticated()
                    // 其他路线操作需要认证
                    .requestMatchers("/api/routes/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/carousels").permitAll()
                    .requestMatchers("/api/hotel-bookings/hotels/**").permitAll()
                    .requestMatchers("/api/hotel-bookings/room-types/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/comments/spot/**").permitAll()
                    .requestMatchers("/api/comments/**").authenticated()
                    .requestMatchers("/api/test/**").authenticated()
                    // 社区问答 — GET 请求公开，写操作需认证
                    .requestMatchers(HttpMethod.GET, "/api/community/questions").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/community/questions/**").permitAll()
                    .requestMatchers("/api/community/questions/**").authenticated()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated();
            });
        
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "img-src 'self' data: blob:; " +
                        "object-src 'none'; " +
                        "base-uri 'self'; " +
                        "frame-ancestors 'self'"))
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin"))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000)));

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(csrfCookieFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
