package com.ssafy.keepick.global.security.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ssafy.keepick.auth.application.dto.CustomOAuth2Member;
import com.ssafy.keepick.global.security.util.JWTUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Value("${app.redirect.defaultBase}")
    private String defaultBase;

    // 쉼표로 구분된 문자열을 List로 주입
    @Value("#{'${app.redirect.allowed}'.split('\\s*,\\s*')}")
    private List<String> allowedOrigins;

    @Value("${app.security.stateSecret:keepick-state-secret-key}")
    private String stateSecret;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        CustomOAuth2Member user = (CustomOAuth2Member) authentication.getPrincipal();
        String username = user.getUsername();
        Long memberId = user.getMemberId();

        log.info("🔐 OAuth2 인증 시작 - 누가: {}(ID:{}) | 언제: {} | 어디서: {} | 무엇을: {} {} | 어떻게: {} | 왜: OAuth2 로그인 성공",
                username, memberId, java.time.LocalDateTime.now(), clientIp, method, requestUri, userAgent);

        String token = jwtUtil.createToken(memberId, username);

        // state 파라미터에서 원본 Origin 추출 시도 (우선)
        String rawState = request.getParameter("state");
        String baseRedirect;
        
        if (rawState != null && rawState.contains(".")) {
            // HMAC 검증 및 파싱
            String[] parts = rawState.split("\\.");
            if (parts.length == 2) {
                String encoded = parts[0];
                String sig = parts[1];
                
                String expected = hmacSha256B64Url(encoded, stateSecret);
                if (expected.equals(sig)) {
                    try {
                        String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(json);
                        
                        if (node.has("origin")) {
                            String candidate = normalize(node.get("origin").asText());
                            if (isAllowed(candidate)) {
                                baseRedirect = candidate;
                                log.info("✅ state HMAC 검증 성공 → redirect base: {}", baseRedirect);
                            } else {
                                baseRedirect = normalize(defaultBase);
                                log.warn("🚫 state에서 추출한 Origin이 허용되지 않음: {} → defaultBase 사용", candidate);
                            }
                        } else {
                            baseRedirect = normalize(defaultBase);
                            log.info("ℹ️ state에 Origin 정보 없음 → defaultBase 사용");
                        }
                    } catch (Exception e) {
                        log.warn("🚫 state JSON 파싱 실패 → defaultBase 사용", e);
                        baseRedirect = normalize(defaultBase);
                    }
                } else {
                    log.warn("🚫 state HMAC 불일치 → defaultBase 사용");
                    baseRedirect = normalize(defaultBase);
                }
            } else {
                log.warn("🚫 state 형식 불일치 → defaultBase 사용");
                baseRedirect = normalize(defaultBase);
            }
        } else if (rawState != null) {
            // 기존 방식 (단순 origin= 형식) - 하위 호환성
            String originalOrigin = parseOriginFromState(rawState);
            if (originalOrigin != null && isAllowed(originalOrigin)) {
                baseRedirect = normalize(originalOrigin);
                log.info("✅ 기존 방식 state 파라미터에서 원본 Origin 복원: {} → {}", originalOrigin, baseRedirect);
            } else {
                baseRedirect = normalize(defaultBase);
                log.info("ℹ️ 기존 방식 state 파라미터 있지만 Origin 추출 실패 또는 허용되지 않음 - defaultBase 사용: {}", baseRedirect);
            }
        } else {
            // state 파라미터가 없으면 기존 방식으로 Origin 추정
            String resolvedOrigin = resolveOrigin(request);
            baseRedirect = pickRedirectBase(resolvedOrigin);
            log.info("ℹ️ state 파라미터 없음 - 헤더에서 Origin 추정: {} → {}", resolvedOrigin, baseRedirect);
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString(baseRedirect)
                .path("/")
                .queryParam("token", token)  // 쿼리스트링으로 전달
                .build()
                .toUriString();

        log.info("🔄 OAuth2 리다이렉트 완료 - 누가: {}(ID:{}) | 언제: {} | 어디서: {} | 무엇을: {}로 리다이렉트 | 어떻게: Origin:{} → {} | 왜: 인증 성공 후 프론트엔드 전달",
                username, memberId, java.time.LocalDateTime.now(), clientIp, baseRedirect, rawState != null ? "state파싱" : "헤더추정", baseRedirect);

        response.sendRedirect(redirectUrl);
    }

    /** state 파라미터에서 원본 Origin 추출 */
    private String parseOriginFromState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        
        try {
            // state 파라미터 디코딩
            String decodedState = URLDecoder.decode(state, StandardCharsets.UTF_8);
            
            // state 형식: "origin=https://localhost:3000" 또는 "origin=https://fe-keepick.vercel.app"
            if (decodedState.startsWith("origin=")) {
                String origin = decodedState.substring("origin=".length());
                log.info("🔍 state 파라미터에서 Origin 추출: {} → {}", state, origin);
                return normalize(origin);
            }
        } catch (Exception e) {
            log.warn("🚫 state 파라미터 파싱 실패: {}", state, e);
        }
        
        return null;
    }

    /** 화이트리스트 매칭 */
    private boolean isAllowed(String origin) {
        if (origin == null) return false;
        
        boolean allowed = allowedOrigins.stream()
                .map(this::normalize)
                .anyMatch(allow -> allow.equalsIgnoreCase(origin));
        
        if (allowed) {
            log.info("✅ 허용된 Origin 확인: {}", origin);
        } else {
            log.warn("🚫 차단된 Origin 감지: {}", origin);
        }
        
        return allowed;
    }

    /** 요청의 출발 Origin 추정: Origin → Referer(origin만) → X-Forwarded-Proto/Host */
    private String resolveOrigin(HttpServletRequest request) {
        // OAuth2 콜백에서는 Origin이 OAuth 제공자 도메인이므로 Referer를 우선 확인
        String referer = headerOrNull(request, "Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                if (uri.getScheme() != null && uri.getHost() != null) {
                    // OAuth 제공자 도메인이 아닌 경우만 사용
                    String host = uri.getHost();
                    if (!host.contains("naver.com") && !host.contains("kakao.com") && !host.contains("google.com")) {
                        String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
                        log.info("🔍 Referer에서 Origin 추정: {} → {}", referer, base);
                        return normalize(base);
                    }
                }
            } catch (Exception ignored) {}
        }

        String origin = headerOrNull(request, "Origin");
        if (origin != null) {
            // OAuth 제공자 도메인이 아닌 경우만 사용
            if (!origin.contains("naver.com") && !origin.contains("kakao.com") && !origin.contains("google.com")) {
                log.info("🔍 Origin 헤더에서 추정: {}", origin);
                return normalize(origin);
            }
        }

        String xfProto = headerOrNull(request, "X-Forwarded-Proto");
        String xfHost  = headerOrNull(request, "X-Forwarded-Host");
        if (xfProto != null && xfHost != null) {
            String base = xfProto + "://" + xfHost;
            log.info("🔍 X-Forwarded 헤더에서 Origin 추정: {}://{}", xfProto, xfHost);
            return normalize(base);
        }
        
        log.info("🔍 Origin 추정 실패 - 모든 헤더 확인 불가");
        return null;
    }

    /** 화이트리스트에 있을 때만 사용, 아니면 defaultBase 사용 */
    private String pickRedirectBase(String origin) {
        if (origin != null) {
            boolean allowed = allowedOrigins.stream()
                    .map(this::normalize)
                    .anyMatch(allow -> allow.equalsIgnoreCase(origin));
            if (allowed) {
                log.info("✅ 허용된 Origin 확인 - 누가: 시스템 | 언제: {} | 어디서: {} | 무엇을: {} 사용 | 어떻게: 화이트리스트 매칭 | 왜: 안전한 리다이렉트",
                        java.time.LocalDateTime.now(), origin, origin);
                return origin;
            }
            log.warn("🚫 차단된 Origin 감지 - 누가: 시스템 | 언제: {} | 어디서: {} | 무엇을: {} 차단 | 어떻게: 화이트리스트 미매칭 | 왜: 보안상 defaultBase로 폴백",
                    java.time.LocalDateTime.now(), origin, origin);
        } else {
            log.info("ℹ️ Origin 미감지 - 누가: 시스템 | 언제: {} | 어디서: 시스템 | 무엇을: defaultBase 사용 | 어떻게: Origin 헤더 없음 | 왜: 기본 리다이렉트",
                    java.time.LocalDateTime.now());
        }
        return normalize(defaultBase);
    }

    private String headerOrNull(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 끝의 슬래시 제거 */
    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    /** HMAC-SHA256 서명 (Base64 URL 인코딩) */
    private String hmacSha256B64Url(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("🚫 HMAC 서명 검증 실패", e);
            throw new RuntimeException(e);
        }
    }

    /** 클라이언트 IP 주소 추출 (프록시 환경 고려) */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
