package top.mrys.auth.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.mrys.auth.interceptor.AuthInterceptor;
import top.mrys.auth.properties.AuthProperties;
import top.mrys.auth.spel.AuthSpelEvaluator;
import top.mrys.auth.token.SimpleTokenExtractor;
import top.mrys.auth.token.TokenExtractor;

/**
 * 认证框架自动配置类
 * 负责自动配置认证相关的 Bean 和组件
 *
 * @author mrys
 */
@AutoConfiguration
@ConditionalOnClass({SecurityManager.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AuthProperties.class)
public class AuthAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthAutoConfiguration.class);

    /**
     * 配置安全管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityManager securityManager() {
        return new SecurityManager();
    }

    /**
     * 配置默认的 Token 提取器
     */
    @Bean
    @ConditionalOnMissingBean
    public TokenExtractor tokenExtractor() {
        return new SimpleTokenExtractor();
    }

    /**
     * 配置 SpEL 表达式求值器
     * 每次请求都会创建一个新的实例，
     */
    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AuthSpelEvaluator authSpelEvaluator() {
        log.debug("创建新的 AuthSpelEvaluator 实例");
        return new AuthSpelEvaluator();
    }

    /**
     * 配置认证拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthInterceptor authInterceptor(SecurityManager securityManager,
                                           AuthProperties authProperties) {
        return new AuthInterceptor(securityManager, authProperties);
    }

    /**
     * Web MVC 配置
     */
    @Configuration
    @ConditionalOnClass({WebMvcConfigurer.class})
    @RequiredArgsConstructor
    public static class AuthWebMvcConfiguration implements WebMvcConfigurer {

        private final AuthInterceptor authInterceptor;
        private final AuthProperties authProperties;


        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(authInterceptor)
                    .addPathPatterns(authProperties.getIncludePatterns())
                    .excludePathPatterns(authProperties.getExcludePatterns())
                    .order(authProperties.getInterceptorOrder());
        }
    }
}