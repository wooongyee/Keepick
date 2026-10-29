package com.ssafy.keepick.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 요청 시작 시 시간 저장
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 캐싱 래퍼로 감싸야 바디 로깅 가능
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        String requestId = UUID.randomUUID().toString();
        request.setAttribute("REQUEST_ID", requestId);

        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        String logMessage = "\n====================  Incoming Request  ====================\n" +
                " REQUEST ID  : " + requestId + "\n" +
                " Time        : " + LocalDateTime.now().format(formatter) + "\n" +
                " Method      : " + request.getMethod() + "\n" +
                " URI         : " + request.getRequestURI() + "\n" +
                " Query Params: " + (request.getQueryString() == null ? "" : "[redacted]") + "\n" +
                " Remote IP   : " + request.getRemoteAddr() + "\n" +
                " User-Agent  : " + request.getHeader("User-Agent") + "\n" +
                " Headers     : " + getHeaders(request) + "\n" +
                "=============================================================\n";

        log.info(logMessage);

        return true;
    }

    // 응답 완료 시 시간 측정
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestId = (String) request.getAttribute("REQUEST_ID");
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = (startTime != null) ? System.currentTimeMillis() - startTime : -1;

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n====================  Response Info  ====================\n")
                .append(" REQUEST ID   : ").append(requestId).append("\n")
                .append(" Status      : ").append(response.getStatus()).append("\n")
                .append(" Duration    : ").append(duration).append(" ms\n")
                .append("=========================================================");


        if (ex != null) {
            logMessage.append("⚠️ Exception   : ").append(ex.getMessage()).append("\n");
            log.error(logMessage.toString(), ex);
        } else {
            log.info(logMessage.toString());
        }
    }

    private String getHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            for (String headerName : Collections.list(headerNames)) {
                boolean sensitive = java.util.Set.of("authorization", "cookie", "set-cookie", "referer").contains(headerName.toLowerCase(java.util.Locale.ROOT));
                headers.append(headerName).append("=").append(sensitive ? "[redacted]" : request.getHeader(headerName)).append("; ");
            }
        }
        return headers.toString();
    }

    // (선택) 요청 바디 로깅
    private String getRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] buf = request.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf, 0, buf.length, request.getCharacterEncoding());
            }
        } catch (UnsupportedEncodingException ignored) {}
        return "";
    }
}
