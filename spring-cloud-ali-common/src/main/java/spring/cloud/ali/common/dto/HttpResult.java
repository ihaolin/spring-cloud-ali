package spring.cloud.ali.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring.cloud.ali.common.exception.BizException;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HttpResult<T> {

    private Integer code;

    private T data;

    private String msg;

    public static <T> HttpResult<T> success(T data){
        HttpResult<T> r = new HttpResult<>();
        r.code = 0;
        r.data = data;
        return r;
    }

    public static <T> HttpResult<T> fail(Integer code, String msg){
        HttpResult<T> r = new HttpResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> HttpResult<T> fail(BizException e){
        HttpResult<T> r = new HttpResult<>();
        r.code = e.getCode();
        r.msg = e.getMessage();
        return r;
    }

    public static boolean isSuccess(int code){
        return 0 == code;
    }
}
