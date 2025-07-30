package top.mrys.auth.token;

import top.mrys.auth.exceptions.TokenException;

/**
 * 空token 验证器
 * 验证token 是否为空
 */
public class EmptyTokenValidator implements TokenValidator{
    @Override
    public void validate(TokenValidatorChain chain, TokenContext ctx) throws TokenException {
        if (ctx.getToken() == null) {
            throw new TokenException(null, "token 不能为空 联系开发人员 是否配置错误");
        }
        if (chain != null) {
            chain.validate(ctx);
        }
    }
}
