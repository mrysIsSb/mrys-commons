package top.mrys.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解，验证权限
 * 此注解可用于方法或类上，用于指定需要验证的权限。
 * 通过设置 value 属性为 SpEL 表达式，定义权限验证逻辑。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuth {

    /**
     * spel表达式 用来验证权限
     * 例如：@CheckAuth("hasRole('ROLE_ADMIN') and hasPermission('user:add')")
     */
    String value();

    String msg() default "没有权限访问此资源";
}
