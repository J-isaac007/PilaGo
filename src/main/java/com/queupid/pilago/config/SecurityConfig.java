package com.queupid.pilago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.storage.StaffRepository;

import jakarta.servlet.DispatcherType;

/**
 * Who may open which page.
 *
 * <ul>
 *   <li>Residents and the TV board need no login: {@code /}, {@code /request/**}, {@code /ticket/**}, {@code /display}.</li>
 *   <li>{@code /staff/**} is for the {@code SECRETARY} role, {@code /captain/**} for the {@code CAPTAIN} role.</li>
 *   <li>Staff log in at {@code /login} with their Staff ID. The role is {@link StaffMember#getRole()}, so a
 *       {@code BarangayCaptain} and a {@code BarangaySecretary} get different pages (polymorphism).</li>
 * </ul>
 * Passwords are checked with the BCrypt {@code PasswordEncoder} from {@link PasswordConfig}.
 */
@Configuration
public class SecurityConfig {

    public static final String SECRETARY = "SECRETARY";
    public static final String CAPTAIN = "CAPTAIN";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // error pages and internal forwards must render for everyone
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/staff/**").hasRole(SECRETARY)
                        .requestMatchers("/captain/**").hasRole(CAPTAIN)
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("staffId")
                        .passwordParameter("password")
                        .successHandler(homeForRole())
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console in dev
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        return http.build();
    }

    /** Staff accounts come from the database; the Staff ID is the username. */
    @Bean
    public UserDetailsService userDetailsService(StaffRepository staff) {
        return staffId -> {
            StaffMember member = staff.findByStaffIdIgnoreCase(staffId)
                    .orElseThrow(() -> new UsernameNotFoundException("No staff with ID " + staffId));
            return User.withUsername(member.getStaffId())
                    .password(member.getPasswordHash())
                    .roles(member.getRole())
                    .build();
        };
    }

    /** After login the captain goes to the signing list; the secretary to the dashboard. */
    private static AuthenticationSuccessHandler homeForRole() {
        return (request, response, authentication) -> {
            boolean captain = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(("ROLE_" + CAPTAIN)::equals);
            response.sendRedirect(request.getContextPath() + (captain ? "/captain" : "/staff"));
        };
    }
}
