package top.mrys.auth.interceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.mrys.auth.annotation.AuthAnnotationParser;
import top.mrys.auth.annotation.CheckAuth;
import top.mrys.auth.config.SecurityConfigWrapper;
import top.mrys.auth.config.SecurityManager;
import top.mrys.auth.exceptions.TokenException;
import top.mrys.auth.properties.AuthProperties;
import top.mrys.auth.spel.AuthSpelEvaluator;
import top.mrys.auth.token.Token;
import top.mrys.auth.token.TokenContext;

import java.util.Optional;

/**
 * 认证拦截器
 * 负责拦截请求并进行认证和权限验证
 *
 * @author mrys
 */
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SecurityManager securityManager;
    @Resource
    private AuthSpelEvaluator authSpelEvaluator;
    private final AuthProperties authProperties;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果认证框架未启用，直接放行
        if (!authProperties.isEnabled()) {
            return true;
        }
        try {
            // 创建认证上下文
            TokenContext context = new TokenContext();
            TokenContext.set(context);

            // 查找匹配的安全配置
            SecurityConfigWrapper matchedConfig = securityManager.getSecurityConfigWrappers()
                    .stream()
                    .filter(config -> config.match(request.getRequestURI())) // 匹配请求路径
                    .findFirst()
                    .orElse(null);

            if (matchedConfig == null) {
                log.debug("未找到匹配的安全配置，放行请求: {}", request.getRequestURI());
                return true;
            }

            // 提取 Token
            Optional<Token> token = matchedConfig.getToken();

            if (token.isPresent()) {
                context.setToken(token.get());
                // 验证token
                matchedConfig.getTokenValidatorChain().validate(context);

                if (!context.getToken().isValid()) {
                    throw new TokenException(context.getToken(), "token 验证未通过");
                }
            }


            // 只处理方法处理器 不处理其他类型的处理器 如:
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }

            checkPermission(request, handlerMethod);

            return true;

        } catch (TokenException e) {
            log.warn("认证失败: {} - {}", request.getRequestURI(), e.getMessage());
            handleAuthenticationFailure(request, response, e);
            return false;
        } catch (Exception e) {
            log.error("认证过程中发生异常: {}", request.getRequestURI(), e);
            handleAuthenticationFailure(request, response, new TokenException(null, "认证过程中发生异常"));
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理认证上下文
        TokenContext.clear();
    }

    /**
     * 处理权限验证
     */
    private void checkPermission(HttpServletRequest request, HandlerMethod handlerMethod) throws TokenException {
        // 获取权限注解
        Optional<CheckAuth> checkAuth = AuthAnnotationParser.parseCheckAuth(handlerMethod);

        if (checkAuth.isEmpty()) {
            // 判断是否登录
            TokenContext context = TokenContext.get();
            if (!context.getToken().isValid()) {
                throw new TokenException(context.getToken(), "未登录或登录已过期");
            } else {
                return; // 如果没有权限注解，表示只需要登录即可访问
            }
        }

        Object attrs = AuthAnnotationParser.getAliasAttrs(handlerMethod.getMethod());
        authSpelEvaluator.setEvaluationContextConsumer(context -> {
            context.setVariable("alias", attrs);
        });
        boolean evaluate = authSpelEvaluator.evaluate(checkAuth.get().value(), TokenContext.get(), request);
        if (!evaluate) {
            // 如果权限验证失败，抛出异常
            throw new TokenException(TokenContext.get().getToken(), checkAuth.get().msg());
        }
    }


    /**
     * 处理认证失败
     */
    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, TokenException e) {
        // 设置响应状态码
        if (e.getToken() == null) {
            response.setStatus(authProperties.getException().getAuthFailureStatus());
        } else {
            response.setStatus(authProperties.getException().getAccessDeniedStatus());
        }

        // 设置响应头
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
    }
}