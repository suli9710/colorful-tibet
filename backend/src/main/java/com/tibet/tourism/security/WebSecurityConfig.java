package com.tibet.tourism.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
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
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/register").permitAll()
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/auth/me/**").authenticated()
                    .requestMatchers("/api/spots/**").permitAll()
                    .requestMatchers("/api/news/**").permitAll()
                    .requestMatchers("/api/heritage/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/api/routes/generate").permitAll() // AI生成公开
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
                    .requestMatchers("/api/comments/**").permitAll()
                    .requestMatchers("/api/test/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/api/admin/**").authenticated()
                    .anyRequest().authenticated()
            );
        
        // Fix for H2 console
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
