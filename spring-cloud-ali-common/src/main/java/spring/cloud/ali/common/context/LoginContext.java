package spring.cloud.ali.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;

@Data
public class LoginContext {

    public static final String HTTP_HEADER_LOGIN_TOKEN = "Login-Token";

    public static final String HTTP_HEADER_LOGIN_USER_ID = "Login-User-ID";

    private static final TransmittableThreadLocal<LoginUser> CTX = new TransmittableThreadLocal<>();

    public static LoginUser get(){
        return CTX.get();
    }

    public static void set(LoginUser loginUser){
        CTX.set(loginUser);
    }

    public static void clear(){
        CTX.remove();
    }
}
