package top.mrys.auth.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * 权限注解解析器
 *
 * @author mrys
 */
public class AuthAnnotationParser {

    /**
     * 解析注解
     *
     * @param element 注解元素
     * @return 解析结果
     */
    public static Optional<CheckAuth> parseCheckAuth(AnnotatedElement element) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, CheckAuth.class));
    }

    /**
     * 解析权限注解
     * 先获取方法上的注解，如果没有，则获取类上的注解
     *
     * @param handlerMethod
     * @return
     */
    public static Optional<CheckAuth> parseCheckAuth(HandlerMethod handlerMethod) {
        Optional<CheckAuth> checkAuth = parseCheckAuth(handlerMethod.getMethod());
        if (checkAuth.isEmpty()) {
            checkAuth = parseCheckAuth(handlerMethod.getBeanType());
        }
        return checkAuth;
    }

    public static Optional<AuthAlias> parseAuthAlias(AnnotatedElement element) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, AuthAlias.class));
    }

    public static Object getAliasAttrs(AnnotatedElement element) {
        Optional<AuthAlias> authAlias = parseAuthAlias(element);
        return authAlias.map(alias -> AnnotatedElementUtils.findMergedAnnotation(element, alias.value())).orElse(null);
    }
}
