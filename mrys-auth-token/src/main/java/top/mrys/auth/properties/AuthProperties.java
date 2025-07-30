package top.mrys.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证框架配置属性
 * 支持通过配置文件自定义认证行为
 *
 * @author mrys
 */
@Data
@ConfigurationProperties(prefix = "mrys.auth")
public class AuthProperties {

    /**
     * 是否启用认证框架
     */
    private boolean enabled = true;

    /**
     * 拦截器顺序
     */
    private int interceptorOrder = 0;

    /**
     * 包含的路径模式
     * 默认拦截所有路径
     */
    private String[] includePatterns = {"/**"};

    /**
     * 排除的路径模式
     * 默认排除静态资源和错误页面
     */
    private String[] excludePatterns = {
            "/static/**",
            "/public/**",
            "/resources/**",
            "/META-INF/resources/**",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/actuator/**"
    };

    /**
     * Token 配置
     */
    private TokenConfig token = new TokenConfig();

    /**
     * SpEL 表达式配置
     */
    private SpelConfig spel = new SpelConfig();

    /**
     * 异常处理配置
     */
    private ExceptionConfig exception = new ExceptionConfig();

    /**
     * Token 相关配置
     */
    @Data
    public static class TokenConfig {
        /**
         * Token 参数名
         */
        private String parameterName = "token";

        /**
         * Token 请求头名称
         */
        private String[] headerNames = {"Authorization", "X-Authorization", "X-Token", "token"};

        /**
         * Token Cookie 名称
         */
        private String cookieName = "token";

        /**
         * 是否启用从请求头提取 Token
         */
        private boolean enableHeaderExtraction = true;

        /**
         * 是否启用从查询参数提取 Token
         */
        private boolean enableParameterExtraction = true;

        /**
         * 是否启用从 Cookie 提取 Token
         */
        private boolean enableCookieExtraction = true;
    }

    /**
     * SpEL 表达式相关配置
     */
    @Data
    public static class SpelConfig {
        /**
         * 是否启用 SpEL 表达式缓存
         */
        private boolean enableCache = true;

        /**
         * SpEL 表达式缓存大小
         */
        private int cacheSize = 256;

        /**
         * 是否启用安全模式（禁用一些危险的 SpEL 功能）
         */
        private boolean secureMode = true;
    }

    /**
     * 异常处理相关配置
     */
    @Data
    public static class ExceptionConfig {
        /**
         * 是否启用全局异常处理
         */
        private boolean enableGlobalHandler = true;

        /**
         * 认证失败时的 HTTP 状态码
         */
        private int authFailureStatus = 401;

        /**
         * 权限不足时的 HTTP 状态码
         */
        private int accessDeniedStatus = 403;

        /**
         * 是否返回详细错误信息
         */
        private boolean includeErrorDetails = false;

        /**
         * 默认错误消息
         */
        private String defaultErrorMessage = "认证失败";
    }
}