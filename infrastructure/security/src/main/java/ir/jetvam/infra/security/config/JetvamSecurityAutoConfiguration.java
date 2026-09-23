package ir.jetvam.infra.security.config;

import ir.jetvam.infra.security.JetvamJwtAuthenticationConverter;
import ir.jetvam.infra.security.web.JetvamAccessDeniedHandler;
import ir.jetvam.infra.security.web.JetvamAuthenticationEntryPoint;
import ir.jetvam.infra.security.web.SecurityErrorWriter;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-configures JWT validation, principal mapping and stateless API protection.
 * Applications activate it by providing standard Spring resource-server settings.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@AutoConfiguration
@EnableMethodSecurity
@ConditionalOnClass({HttpSecurity.class, JwtDecoder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(JetvamSecurityProperties.class)
public class JetvamSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JetvamJwtAuthenticationConverter jetvamJwtAuthenticationConverter() {
        return new JetvamJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    SecurityErrorWriter jetvamSecurityErrorWriter(
            ObjectMapper objectMapper,
            ApiResponseFactory responseFactory
    ) {
        return new SecurityErrorWriter(objectMapper, responseFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    JetvamAuthenticationEntryPoint jetvamAuthenticationEntryPoint(SecurityErrorWriter writer) {
        return new JetvamAuthenticationEntryPoint(writer);
    }

    @Bean
    @ConditionalOnMissingBean
    JetvamAccessDeniedHandler jetvamAccessDeniedHandler(SecurityErrorWriter writer) {
        return new JetvamAccessDeniedHandler(writer);
    }

    @Bean
    @ConditionalOnBean(JwtDecoder.class)
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(
            prefix = "jetvam.security.resource-server",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    SecurityFilterChain jetvamResourceServerSecurityFilterChain(
            HttpSecurity http,
            JetvamSecurityProperties properties,
            JetvamJwtAuthenticationConverter authenticationConverter,
            JetvamAuthenticationEntryPoint authenticationEntryPoint,
            JetvamAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        String[] publicPaths = properties.getPublicPaths().toArray(String[]::new);
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicPaths).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
