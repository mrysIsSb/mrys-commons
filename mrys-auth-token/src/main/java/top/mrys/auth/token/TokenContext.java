package top.mrys.auth.token;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 令牌上下文
 * 用于存储当前请求的令牌信息
 */
@Getter
@Setter
public class TokenContext {
    private static final ThreadLocal<TokenContext> context = new ThreadLocal<>();

    private Token token;
    private UserInfo userInfo;

    public static TokenContext get() {
        return context.get();
    }

    public static void set(TokenContext tokenContext) {
        context.set(tokenContext);
    }

    public static void clear() {
        context.remove();
    }
} 