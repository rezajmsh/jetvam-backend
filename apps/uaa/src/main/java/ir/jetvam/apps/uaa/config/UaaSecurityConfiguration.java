package ir.jetvam.apps.uaa.config;

import ir.jetvam.infra.security.JetvamJwtAuthenticationConverter;
import ir.jetvam.infra.security.web.JetvamAccessDeniedHandler;
import ir.jetvam.infra.security.web.JetvamAuthenticationEntryPoint;
import ir.jetvam.apps.uaa.security.OtpGrantAuthenticationConverter;
import ir.jetvam.apps.uaa.security.OtpGrantAuthenticationProvider;
import ir.jetvam.apps.uaa.security.PasswordGrantAuthenticationConverter;
import ir.jetvam.apps.uaa.security.PasswordGrantAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Combines OAuth token endpoints and secured UAA APIs.
 * All login flows are explicit grants and the server remains stateless.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Configuration(proxyBeanMethods = false)
public class UaaSecurityConfiguration {

    @Bean
    SecurityFilterChain uaaSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JetvamJwtAuthenticationConverter authenticationConverter,
            JetvamAuthenticationEntryPoint authenticationEntryPoint,
            JetvamAccessDeniedHandler accessDeniedHandler,
            OtpGrantAuthenticationProvider otpGrantAuthenticationProvider,
            PasswordGrantAuthenticationProvider passwordGrantAuthenticationProvider
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/customer/registrations/otp",
                                "/api/v1/customer/registrations/verify",
                                "/api/v1/customer/auth/otp",
                                "/api/v1/password-users/auth/prepare").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/api/**")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2AuthorizationServer(server -> server
                        .tokenEndpoint(endpoint -> endpoint
                                .accessTokenRequestConverter(new OtpGrantAuthenticationConverter())
                                .accessTokenRequestConverter(new PasswordGrantAuthenticationConverter())
                                .authenticationProvider(otpGrantAuthenticationProvider)
                                .authenticationProvider(passwordGrantAuthenticationProvider))
                        .oidc(Customizer.withDefaults()))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                authenticationEntryPoint,
                                PathPatternRequestMatcher.pathPattern("/api/**")
                        ));
        return http.build();
    }
}
