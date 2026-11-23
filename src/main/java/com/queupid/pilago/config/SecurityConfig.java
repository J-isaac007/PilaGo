package com.queupid.pilago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.security.AccountLockedException;
import com.queupid.pilago.security.LoginAttempts;
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
    public SecurityFilterChain filterChain(HttpSecurity http, LoginAttempts attempts) throws Exception {
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
                        .successHandler(homeForRole(attempts))
                        .failureHandler(failureHandler(attempts))
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
    public UserDetailsService userDetailsService(StaffRepository staff, LoginAttempts attempts) {
        return staffId -> {
            StaffMember member = staff.findByStaffIdIgnoreCase(staffId)
                    .orElseThrow(() -> new UsernameNotFoundException("No staff with ID " + staffId));
            return User.withUsername(member.getStaffId())
                    .password(member.getPasswordHash())
                    .roles(member.getRole())
                    .accountLocked(attempts.isLocked(member.getStaffId())) // locked: even the right password is refused
                    .build();
        };
    }

    /** A staff member's home page: the captain's signing list, or the secretary's dashboard. */
    public static String homeFor(Authentication authentication) {
        boolean captain = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + CAPTAIN)::equals);
        return captain ? "/captain" : "/staff";
    }

    /** After login, go to the home page for the role. */
    private static AuthenticationSuccessHandler homeForRole(LoginAttempts attempts) {
        return (request, response, authentication) -> {
            attempts.recordSuccess(authentication.getName());
            response.sendRedirect(request.getContextPath() + homeFor(authentication));
        };
    }

    /**
     * A failed login. A wrong password counts toward the lock; if the Staff ID is now locked (this was the
     * 5th wrong try, or it was already locked) the page shows the "too many tries" message instead.
     */
    private static AuthenticationFailureHandler failureHandler(LoginAttempts attempts) {
        return (request, response, exception) -> {
            String staffId = request.getParameter("staffId");
            if (exception instanceof BadCredentialsException) {
                attempts.recordFailure(staffId);
            }
            String target = "/login?error";
            try {
                attempts.requireUnlocked(staffId);
            } catch (AccountLockedException locked) {
                target = "/login?locked";
            }
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}
