package top.mrys.example.auth.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.mrys.auth.exceptions.TokenException;
import top.mrys.auth.token.*;
import top.mrys.example.auth.model.CustomUserInfo;
import top.mrys.example.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

/**
 * 自定义 Token 验证器
 * 负责验证请求中的 Token 是否有效，并设置用户上下文
 *
 * @author mrys
 */
@Slf4j
@Component
public class CustomTokenValidator implements TokenValidator {

    private final UserService userService;

    public CustomTokenValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void validate(TokenValidatorChain chain, TokenContext ctx) {
        Token token = ctx.getToken();

        if (token == null || !(token instanceof SimpleToken)) {
            log.debug("Token 为空或类型不匹配");
            throw new TokenException(token, "Token 为空或类型不匹配");
        }

        SimpleToken simpleToken = (SimpleToken) token;
        String tokenValue = simpleToken.getToken();

        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            log.debug("Token 值为空");
            throw new TokenException(token, "Token 值为空");
        }

        try {
            // 验证 Token 格式和有效性
            if (!isValidTokenFormat(tokenValue)) {
                log.debug("Token 格式无效: {}", tokenValue);
                throw new TokenException(token, "Token 格式无效");
            }

            // 从 Token 中解析用户信息
            CustomUserInfo userInfo = parseUserFromToken(tokenValue);
            if (userInfo == null) {
                log.debug("无法从 Token 解析用户信息: {}", tokenValue);
                throw new TokenException(token, "无法从 Token 解析用户信息");
            }

            // 检查用户状态
            if (!userInfo.isActive()) {
                log.debug("用户状态非活跃: {}", userInfo.getUserId());
                throw new TokenException(token, "用户状态非活跃");
            }

            // 设置用户上下文
            token.setValid(true);
            TokenContext context = new TokenContext();
            context.setToken(token);
            context.setUserInfo(userInfo);
            TokenContext.set(context);

            log.debug("Token 验证成功，用户: {}", userInfo.getUsername());


        } catch (Exception e) {
            log.error("Token 验证过程中发生异常", e);
            throw new TokenException(token, "Token 验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证 Token 格式是否有效
     * 简单示例：Token 应该以 "Bearer_" 开头
     */
    private boolean isValidTokenFormat(String token) {
        return token.startsWith("Bearer_") && token.length() > 7;
    }

    /**
     * 从 Token 中解析用户信息
     * 这里是一个简化的示例实现
     */
    private CustomUserInfo parseUserFromToken(String token) {
        try {
            // 移除 "Bearer_" 前缀
            String userToken = token.substring(7);

            // 根据 Token 获取用户信息（这里使用模拟数据）
            return userService.getUserByToken(userToken);

        } catch (Exception e) {
            log.error("解析 Token 失败: {}", token, e);
            return null;
        }
    }

}