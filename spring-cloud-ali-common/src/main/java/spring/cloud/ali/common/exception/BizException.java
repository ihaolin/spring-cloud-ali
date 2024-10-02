package spring.cloud.ali.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 业务异常类
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BizException extends RuntimeException {

    // TODO 错误码，建议按业务分段

    // 1 ~ 1000：通用异常
    public static final BizException RESPONSE_DATA_ERROR = new BizException(400, "response.data.error");
    public static final BizException UN_AUTH = new BizException(401, "un.auth");
    public static final BizException PATH_NOT_FOUND = new BizException(404, "page.not.found");
    public static final BizException DATA_NOT_FOUND = new BizException(405, "data.not.found");
    public static final BizException REQUEST_TOO_MANY = new BizException(429, "request.too.many");
    public static final BizException SERVER_INTERNAL_ERROR = new BizException(500, "server.interval.error");
    public static final BizException SERVER_EXTERNAL_ERROR = new BizException(501, "server.external.error");
    public static final BizException SERVICE_ERROR = new BizException(502, "service.error");
    public static final BizException SERVICE_NOT_UNAVAILABLE = new BizException(503, "service.not.available");
    public static final BizException SERVICE_TIMEOUT = new BizException(504, "service.timeout");

    // 1001 ~ 2000：用户异常

    // 2001 ~ 3000：订单异常

    // ...

    private Integer code;

    private String message;

    public static BizException throwBizException(Integer code, String message){
        throw new BizException(code, message);
    }

    public static BizException throwRespDataError(){
        throw RESPONSE_DATA_ERROR;
    }

    public static BizException throwDataNotFound(){
        throw DATA_NOT_FOUND;
    }
}
