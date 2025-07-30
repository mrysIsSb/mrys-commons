package top.mrys.auth.spel;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import top.mrys.auth.token.TokenContext;
import top.mrys.auth.token.UserInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SpEL 表达式求值器
 * 用于解析和执行权限验证表达式
 *
 * @author mrys
 */
@Slf4j
public class AuthSpelEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentMap<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final AuthSpelFunctions spelFunctions = new AuthSpelFunctions();

    @Getter
    @Setter
    private Consumer<EvaluationContext> evaluationContextConsumer;

    /**
     * 评估 SpEL 表达式
     *
     * @param expression SpEL 表达式字符串
     * @param context    认证上下文
     * @param request    HTTP 请求
     * @return 表达式执行结果
     */
    public boolean evaluate(String expression, TokenContext context, HttpServletRequest request) {
        if (!StringUtils.hasText(expression)) {
            return true;
        }

        expression = expression.trim();

        try {
            // 特殊处理简单表达式
            if ("true".equals(expression.trim())) {
                return true;
            }
            if ("false".equals(expression.trim())) {
                return false;
            }
            if ("".equals(expression.trim())) {
                // 空字符串表示需要登录但不需要特定权限
                return context.getToken() != null && context.getToken().isValid();
            }

            // 获取或创建表达式
            Expression expr = getExpression(expression);

            // 创建求值上下文
            EvaluationContext evalContext = createEvaluationContext(context, request);

            // 执行表达式
            Object result = expr.getValue(evalContext);
            
            // 转换结果为布尔值
            return convertToBoolean(result);

        } catch (Exception e) {
            log.error("SpEL 表达式执行失败: {}", expression, e);
            return false;
        }
    }

    /**
     * 获取或创建表达式（带缓存）
     */
    private Expression getExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, parser::parseExpression);
    }

    /**
     * 创建 SpEL 求值上下文
     */
    private EvaluationContext createEvaluationContext(TokenContext context, HttpServletRequest request) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();

        // 设置根对象为认证上下文
        evalContext.setRootObject(context);

        // 注册变量
        evalContext.setVariable("token", context.getToken());
        evalContext.setVariable("user", context.getUserInfo());
        evalContext.setVariable("request", request);

        // 注册自定义函数
        registerSpelFunctions(evalContext);

        // 如果有额外的上下文处理器，则执行
        if (evaluationContextConsumer != null) {
            evaluationContextConsumer.accept(evalContext);
        }
        return evalContext;
    }

    /**
     * 注册 SpEL 自定义函数
     */
    private void registerSpelFunctions(StandardEvaluationContext context) {
        try {
            // 注册权限检查函数
            context.registerFunction("hasRole", 
                AuthSpelFunctions.class.getDeclaredMethod("hasRole", String.class));
            context.registerFunction("hasAnyRole", 
                AuthSpelFunctions.class.getDeclaredMethod("hasAnyRole", String[].class));
            context.registerFunction("hasPermission", 
                AuthSpelFunctions.class.getDeclaredMethod("hasPermission", String.class));
            context.registerFunction("hasAnyPermission", 
                AuthSpelFunctions.class.getDeclaredMethod("hasAnyPermission", String[].class));
            context.registerFunction("isAuthenticated", 
                AuthSpelFunctions.class.getDeclaredMethod("isAuthenticated"));
            context.registerFunction("isAnonymous", 
                AuthSpelFunctions.class.getDeclaredMethod("isAnonymous"));
            context.registerFunction("hasUserId", 
                AuthSpelFunctions.class.getDeclaredMethod("hasUserId", String.class));
            context.registerFunction("hasUsername", 
                AuthSpelFunctions.class.getDeclaredMethod("hasUsername", String.class));

        } catch (NoSuchMethodException e) {
            log.error("注册 SpEL 函数失败", e);
        }
    }

    /**
     * 将结果转换为布尔值
     */
    private boolean convertToBoolean(Object result) {
        if (result == null) {
            return false;
        }
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        if (result instanceof String) {
            return Boolean.parseBoolean((String) result);
        }
        if (result instanceof Number) {
            return ((Number) result).intValue() != 0;
        }
        return true;
    }

    /**
     * 清理表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return expressionCache.size();
    }
}