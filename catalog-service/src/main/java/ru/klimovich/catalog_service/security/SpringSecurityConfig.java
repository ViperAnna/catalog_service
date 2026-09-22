package ru.klimovich.catalog_service.security;


import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import ru.klimovich.catalog_service.util.converter.KCRoleConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KCRoleConverter());

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                .requestMatchers("/actuator/health").permitAll()

                                .requestMatchers(HttpMethod.GET, "/categories", "/categories/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/products", "/products/*", "/products/productByName/*").permitAll()
                                .requestMatchers(HttpMethod.POST, "/products/by-ids").permitAll()

                                .requestMatchers("/categories/**").hasRole("ADMIN")

                                .requestMatchers(HttpMethod.GET, "/products/stores/*/products").hasRole("SELLER")
                                .requestMatchers(HttpMethod.POST, "/products").hasRole("SELLER")
                                .requestMatchers(HttpMethod.PUT, "/products/*").hasRole("SELLER")
                                .requestMatchers(HttpMethod.DELETE, "/products/*").hasRole("SELLER")

                                .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)));


        return http.build();
    }
}
