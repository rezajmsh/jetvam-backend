package ir.jetvam.apps.uaa.config;

import ir.jetvam.infra.security.JetvamJwtAuthenticationConverter;
import ir.jetvam.infra.security.web.JetvamAccessDeniedHandler;
import ir.jetvam.infra.security.web.JetvamAuthenticationEntryPoint;
import ir.jetvam.apps.uaa.security.OtpGrantAuthenticationConverter;
import ir.jetvam.apps.uaa.security.OtpGrantAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Combines OAuth authorization endpoints, interactive login and secured UAA APIs.
 * Browser login uses sessions while API bearer-token authentication remains stateless.
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
            OtpGrantAuthenticationProvider otpGrantAuthenticationProvider
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/login", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/customer/registrations/otp",
                                "/api/v1/customer/registrations/verify",
                                "/api/v1/customer/auth/otp").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/api/**")))
                .formLogin(Customizer.withDefaults())
                .oauth2AuthorizationServer(server -> server
                        .tokenEndpoint(endpoint -> endpoint
                                .accessTokenRequestConverter(new OtpGrantAuthenticationConverter())
                                .authenticationProvider(otpGrantAuthenticationProvider))
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
