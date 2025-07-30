package top.mrys.auth.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleToken implements Token {

    private String token;
    private boolean valid;
    private String from;
    private String key;
}
