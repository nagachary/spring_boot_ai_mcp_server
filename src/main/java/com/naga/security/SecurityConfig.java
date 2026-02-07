package com.naga.security;

import com.naga.filter.JwtSlidingExpirationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtSlidingExpirationFilter jwtSlidingExpirationFilter;

    public SecurityConfig(JwtSlidingExpirationFilter jwtSlidingExpirationFilter) {
        this.jwtSlidingExpirationFilter = jwtSlidingExpirationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/mcp/auth/token", "/auth/**", "/actuator/**").permitAll()  // ← Add /mcp/auth/token
                        .requestMatchers("/mcp/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtSlidingExpirationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
