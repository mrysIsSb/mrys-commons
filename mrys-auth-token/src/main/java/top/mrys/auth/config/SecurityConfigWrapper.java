package top.mrys.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import top.mrys.auth.token.Token;
import top.mrys.auth.token.TokenExtractor;
import top.mrys.auth.token.TokenValidator;
import top.mrys.auth.token.TokenValidatorChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 配置类包装器
 *
 * @author mrys
 */
public class SecurityConfigWrapper {
    private final PathMatcher defaultPathMatcher = new AntPathMatcher();

    @Getter
    @Setter
    private String name;

    private PathMatcher pathMatcher = defaultPathMatcher;

    // 设置包含和排除的路径
    private String[] includePatterns = new String[0];
    private String[] excludePatterns = new String[0];

    // 添加 token 提取器和验证器
    private final List<TokenExtractor> tokenExtractors = new ArrayList<>();
    private final List<TokenValidator> tokenValidators = new ArrayList<>();

    /**
     * 添加 token 提取器
     */
    public SecurityConfigWrapper addTokenExtractors(TokenExtractor... tokenExtractors) {
        this.tokenExtractors.addAll(List.of(tokenExtractors));
        return this;
    }

    /**
     * 添加 token 验证器
     */
    public SecurityConfigWrapper addTokenValidators(TokenValidator... tokenValidators) {
        this.tokenValidators.addAll(List.of(tokenValidators));
        return this;
    }

    /**
     * 设置路径匹配器
     */
    public SecurityConfigWrapper setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
        return this;
    }

    /**
     * 设置包含的路径
     */
    public SecurityConfigWrapper setIncludePatterns(String... includePatterns) {
        this.includePatterns = includePatterns;
        return this;
    }

    /**
     * 设置排除的路径
     */
    public SecurityConfigWrapper setExcludePatterns(String... excludePatterns) {
        this.excludePatterns = excludePatterns;
        return this;
    }

    /**
     * 匹配
     */
    public boolean match(String path) {
        // 如果路径为空或未设置包含和排除模式，则默认匹配
        boolean isMatch = false;
        // 如果路径为空或未设置，则返回false
        if (path == null || path.isEmpty()) {
            return false;
        }
        // 如果没有设置包含和排除模式，则返回true
        if (includePatterns.length == 0 && excludePatterns.length == 0) {
            return true;
        }
        if (includePatterns.length == 0) {
            isMatch = true;
        }
        // 匹配 includePatterns
        for (String pattern : includePatterns) {
            if (pathMatcher.match(pattern, path)) {
                isMatch = true;
                break;
            }
        }
        // 如果没有匹配到 includePatterns，则返回 false
        for (String pattern : excludePatterns) {
            if (pathMatcher.match(pattern, path)) {
                isMatch = false;
            }
        }
        return isMatch;
    }

    /**
     * 获取 TokenValidatorChain
     */
    public TokenValidatorChain getTokenValidatorChain() {
        return TokenValidatorChain.create(this.tokenValidators);
    }

    /**
     * 获取 token
     */
    public Optional<Token> getToken() {
        for (TokenExtractor extractor : tokenExtractors) {
            Optional<Token> token = extractor.extract();
            if (token.isPresent()) {
                return token;
            }
        }
        return Optional.empty();
    }

}
