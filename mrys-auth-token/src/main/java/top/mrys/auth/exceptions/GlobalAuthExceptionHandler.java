package top.mrys.auth.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.mrys.auth.properties.AuthProperties;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局认证异常处理器
 * 统一处理认证相关异常，提供标准的错误响应格式
 *
 * @author mrys
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "mrys.auth.exception", name = "enable-global-handler", havingValue = "true", matchIfMissing = true)
public class GlobalAuthExceptionHandler {

    private final AuthProperties authProperties;

    public GlobalAuthExceptionHandler(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 处理 Token 异常
     */
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<Map<String, Object>> handleTokenException(TokenException e) {
        log.warn("Token 异常: {}", e.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                determineHttpStatus(e),
                e.getMessage(),
                "TOKEN_ERROR",
                e
        );

        return ResponseEntity.status(determineHttpStatus(e)).body(errorResponse);
    }

    /**
     * 处理通用认证异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException e) {
        log.warn("安全异常: {}", e.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.FORBIDDEN,
                e.getMessage(),
                "SECURITY_ERROR",
                e
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(HttpStatus status, String message, String errorCode, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("success", false);
        errorResponse.put("code", status.value());
        errorResponse.put("message", getErrorMessage(message));
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", getCurrentRequestPath());

        // 根据配置决定是否包含详细错误信息
        if (authProperties.getException().isIncludeErrorDetails()) {
            errorResponse.put("details", e.getClass().getSimpleName());
            if (e instanceof TokenException tokenException && tokenException.getToken() != null) {
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("from", getTokenFrom(tokenException.getToken()));
                tokenInfo.put("valid", tokenException.getToken().isValid());
                errorResponse.put("tokenInfo", tokenInfo);
            }
        }

        return errorResponse;
    }

    /**
     * 确定 HTTP 状态码
     */
    private HttpStatus determineHttpStatus(TokenException e) {
        if (e.getToken() == null) {
            // Token 为空，认证失败
            return HttpStatus.valueOf(authProperties.getException().getAuthFailureStatus());
        } else {
            // Token 存在但无效，权限不足
            return HttpStatus.valueOf(authProperties.getException().getAccessDeniedStatus());
        }
    }

    /**
     * 获取错误消息
     */
    private String getErrorMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return authProperties.getException().getDefaultErrorMessage();
        }
        return originalMessage;
    }

    /**
     * 获取当前请求路径
     */
    private String getCurrentRequestPath() {
        try {
            // 尝试从 RequestContextHolder 获取当前请求路径
            var requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletRequestAttributes) {
                return servletRequestAttributes.getRequest().getRequestURI();
            }
        } catch (Exception ex) {
            log.debug("无法获取当前请求路径", ex);
        }
        return "unknown";
    }

    /**
     * 获取 Token 来源信息
     */
    private String getTokenFrom(top.mrys.auth.token.Token token) {
        try {
            // 尝试获取 Token 的来源信息
            if (token instanceof top.mrys.auth.token.SimpleToken simpleToken) {
                return simpleToken.getFrom();
            }
        } catch (Exception ex) {
            log.debug("无法获取 Token 来源信息", ex);
        }
        return "unknown";
    }
}