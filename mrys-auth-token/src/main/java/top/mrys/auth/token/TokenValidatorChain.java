package top.mrys.auth.token;

import top.mrys.auth.exceptions.TokenException;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * @author mrys
 */
public class TokenValidatorChain {
    @Nullable
    private TokenValidatorChain next;

    private final TokenValidator validator;

    public TokenValidatorChain(TokenValidator validator, TokenValidatorChain next) {
        this.validator = validator;
        this.next = next;
    }

    public static TokenValidatorChain create(List<TokenValidator> validators) {
        TokenValidatorChain chain = new TokenValidatorChain(new EmptyTokenValidator(), null);
        TokenValidatorChain current = chain;
        for (int i = 0; i < validators.size(); i++) {
            TokenValidator validator = validators.get(i);
            current.next = new TokenValidatorChain(validator, null);
            current = current.next;
        }
        return chain;
    }

    /**
     * 执行下一个验证器
     * @param ctx
     * @throws TokenException
     */
    public void validate(TokenContext ctx) throws TokenException {
        if (validator.support(ctx.getToken())) {
            validator.validate(next, ctx);
        } else {
            if (next != null) {
                next.validate(ctx);
            }
        }
    }
}
