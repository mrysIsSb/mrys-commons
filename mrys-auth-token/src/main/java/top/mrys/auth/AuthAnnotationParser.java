package top.mrys.auth;

import org.springframework.core.annotation.AnnotatedElementUtils;
import top.mrys.auth.annotation.AuthAlias;
import top.mrys.auth.annotation.CheckAuth;

import java.lang.reflect.AnnotatedElement;

public class AuthAnnotationParser {

    /**
     * 解析注解
     *
     * @param element 注解元素
     * @return 解析结果
     */
    public static CheckAuth parseCheckAuth(AnnotatedElement element) {
        return AnnotatedElementUtils.findMergedAnnotation(element, CheckAuth.class);
    }

    public static AuthAlias parseAuthAlias(AnnotatedElement element) {
        return AnnotatedElementUtils.findMergedAnnotation(element, AuthAlias.class);
    }

    public static Object getAliasAttrs(AnnotatedElement element) {
        AuthAlias authAlias = parseAuthAlias(element);
        if (authAlias == null) {
            return null;
        }
        return AnnotatedElementUtils.findMergedAnnotation(element, authAlias.value());
    }
}
