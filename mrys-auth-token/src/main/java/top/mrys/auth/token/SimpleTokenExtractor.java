package top.mrys.auth.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

/**
 * 简单的token 提取器
 * 从header、query、cookie 中提取token
 */
public class SimpleTokenExtractor implements TokenExtractor {

    @Override
    public Optional<Token> extract() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "header", "Authorization"));
        }
        token = request.getHeader("X-Authorization");
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "header", "X-Authorization"));
        }
        token = request.getHeader("X-Token");
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "header", "X-Token"));
        }
        token = request.getHeader("token");
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "header", "token"));
        }
        token = request.getParameter("token");
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "query", "token"));
        }
        token = request.getCookies() != null ? Arrays.stream(request.getCookies())
                .filter(cookie -> "token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null) : null;
        if (token != null && !token.isBlank()) {
            return Optional.of(new SimpleToken(token, false, "cookie", "token"));
        }

        return Optional.empty();
    }
}
