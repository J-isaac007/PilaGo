package com.queupid.pilago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PLACEHOLDER, owned by Precious (Part 3). It lets every page open without a login so Parts 2 and 3
 * can start building. Replace it in week 3 with: form login at /login using Staff ID
 * ({@code StaffRepository.findByStaffIdIgnoreCase}), roles from {@code StaffMember.getRole()},
 * {@code /staff/**} for SECRETARY, {@code /captain/**} for CAPTAIN, everything else public.
 * Use the {@code PasswordEncoder} bean from {@link PasswordConfig}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        return http.build();
    }
}
