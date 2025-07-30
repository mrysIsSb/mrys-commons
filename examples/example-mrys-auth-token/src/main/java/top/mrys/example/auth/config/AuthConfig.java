package top.mrys.example.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.mrys.auth.token.SimpleTokenExtractor;
import top.mrys.example.auth.validator.CustomTokenValidator;
import top.mrys.example.auth.service.UserService;
import top.mrys.auth.config.SecurityManager;

import jakarta.annotation.PostConstruct;

/**
 * 权限验证配置类
 * 配置 mrys-auth-token 相关的 Bean 和安全规则
 * 
 * 新版本特性：
 * - 使用自动配置，减少手动配置
 * - 基于 SecurityManager 进行安全规则配置
 * - 支持多种 Token 验证器
 * 
 * @author mrys
 */
@Slf4j
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityManager securityManager;
    
    @Autowired
    private UserService userService;


    /**
     * 配置安全规则
     * 使用 SecurityManager 配置不同的安全策略
     */
    @PostConstruct
    public void configureAuth() {
        log.info("配置认证安全规则");
        
        // 配置 API 接口的安全规则
        securityManager.add("api")
                .setIncludePatterns("/api/**")
                .setExcludePatterns("/api/public/**","/index.html","/")
                .addTokenExtractors(new SimpleTokenExtractor())
                .addTokenValidators(new CustomTokenValidator(userService));
        
        log.info("认证安全规则配置完成");
    }

    /**
     * 配置跨域访问
     * 允许前端应用访问 API
     * 
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        
        log.info("配置 CORS 跨域访问支持");
    }
}