package spring.cloud.ali.user.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VerifyTokenResult {

    public static final VerifyTokenResult NOT_PASS = new VerifyTokenResult(false, null);

    private boolean pass;

    private Long userId;
}
