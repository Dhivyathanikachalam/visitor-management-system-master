package com.java.vms.config;

import com.java.vms.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.AbstractSecurityBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/***
 *
 * @project Visitor-Management-System
 * @author anvunnam on 06-02-2025.
 ***/
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request ->
                    request
                        .requestMatchers("/resident/register", "/gt/register").permitAll()
                        .requestMatchers("/admin/**").hasAuthority(Role.ADMIN.getAuthority())
                        .requestMatchers("/gt/**").hasAnyAuthority(Role.ADMIN.getAuthority(), Role.GATEKEEPER.getAuthority())
                        .requestMatchers("/resident/**").hasAnyAuthority(Role.ADMIN.getAuthority(), Role.RESIDENT.getAuthority())
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(12);
    }
}
