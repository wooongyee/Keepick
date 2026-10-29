package com.ssafy.keepick.global.config;

import com.ssafy.keepick.auth.application.CustomOAuth2MemberService;
import com.ssafy.keepick.global.security.filter.JWTFilter;
import com.ssafy.keepick.global.security.filter.OAuth2StateFilter;
import com.ssafy.keepick.global.security.handler.CustomAuthenticationEntryPoint;
import com.ssafy.keepick.global.security.handler.CustomSuccessHandler;
import com.ssafy.keepick.global.security.resolver.OriginAwareAuthorizationRequestResolver;
import com.ssafy.keepick.global.security.util.JWTUtil;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2MemberService customOAuth2MemberService;
    private final CustomSuccessHandler customSuccessHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JWTUtil jwtUtil;
    private final OAuth2StateFilter oauth2StateFilter;
    private final OriginAwareAuthorizationRequestResolver originAwareAuthorizationRequestResolver;

    @Value("#{'${app.redirect.allowed}'.split('\\s*,\\s*')}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 기본 보안 설정 해제 (REST API 스타일)
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // OAuth2 state 파라미터 필터 등록 (OAuth2 인증 리다이렉트 필터보다 앞에 배치)
        // 주의: OriginAwareAuthorizationRequestResolver가 state를 완전히 제어하므로 필터는 로깅용으로만 사용
        // http.addFilterBefore(oauth2StateFilter, 
        //         org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.class);
        
        // JWT 인증 필터 등록 (OAuth2 인증 필터 이후에 실행)
        http.addFilterAfter(new JWTFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class);

        // CORS 설정
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // OAuth2 소셜 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(config -> config
                        .baseUri("/api/oauth2/authorization")        // 프론트와 일치
                        .authorizationRequestResolver(originAwareAuthorizationRequestResolver)  // ★ 커스텀 Resolver로 교체
                )
                .redirectionEndpoint(config -> config.baseUri("/api/login/oauth2/code/*"))
                .userInfoEndpoint(config -> config.userService(customOAuth2MemberService))
                .successHandler(customSuccessHandler) // 로그인 성공 시 JWT 발급 등 처리
        );

        // 경로별 권한 설정
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(
                        // OAuth2 관련 경로
                        "/api/oauth2/**",
                        "/api/local-files/**",
                        "/api/local-health",
                        "/api/login/oauth2/**",
                        // 모바일 인증 관련 경로
                        "/api/auth/login",
                        // Swagger 문서 관련 경로
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs",
                        "/.well-known/assetlinks.json",
                        "/api/groups/*/photos/analysis/status/*")
                .permitAll()
                .anyRequest().authenticated());

        // 세션 사용 안함 (JWT 방식)
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 인증 예외 처리: 로그인 안 된 사용자에게 401 JSON 응답
        http.exceptionHandling(handler -> handler
                .authenticationEntryPoint(customAuthenticationEntryPoint));

        return http.build();
    }

    // CORS 설정 분리
    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();

            // 허용된 origin 목록 사용
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 허용할 HTTP 메서드
            config.setAllowedHeaders(Arrays.asList("*")); // 모든 헤더 허용
            config.setAllowCredentials(true); // 쿠키 전송 허용
            config.setMaxAge(3600L); // pre-flight 요청 캐싱 시간
            config.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization")); // 클라이언트에서 접근 가능한 헤더

            return config;
        };
    }
}
