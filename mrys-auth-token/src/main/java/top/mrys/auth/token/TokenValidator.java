package top.mrys.auth.token;

import top.mrys.auth.exceptions.TokenException;

/**
 * token 验证器
 * <p>
 * 用来验证 token 的合法性
 */
public interface TokenValidator {

    /**
     * 是否支持该 token
     */
    default boolean support(Token token) {
        return true;
    }

    /**
     * 验证 token
     */
    void validate(TokenValidatorChain chain, TokenContext ctx) throws TokenException;
}
