package spring.cloud.ali.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.exception.BizException;
import spring.cloud.ali.common.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static spring.cloud.ali.common.exception.BizException.PATH_NOT_FOUND;
import static spring.cloud.ali.common.exception.BizException.REQUEST_TOO_MANY;
import static spring.cloud.ali.common.exception.BizException.SERVER_INTERNAL_ERROR;
import static spring.cloud.ali.common.exception.BizException.SERVICE_ERROR;
import static spring.cloud.ali.common.exception.BizException.SERVICE_NOT_UNAVAILABLE;
import static spring.cloud.ali.common.exception.BizException.SERVICE_TIMEOUT;

/**
 * HTTP异常响应消息枚举
 */
@Getter
public enum HttpRespStatus {

    HTTP_NOT_FOUND(NOT_FOUND, json(PATH_NOT_FOUND), false),
    HTTP_REQUEST_TOO_MANY(TOO_MANY_REQUESTS, json(REQUEST_TOO_MANY), false),
    HTTP_BAD_GATEWAY(BAD_GATEWAY, json(SERVICE_ERROR), true),
    HTTP_SERVICE_UNAVAILABLE(SERVICE_UNAVAILABLE, json(SERVICE_NOT_UNAVAILABLE), true),
    HTTP_GATEWAY_TIMEOUT(GATEWAY_TIMEOUT, json(SERVICE_TIMEOUT), true),

    DEFAULT(INTERNAL_SERVER_ERROR, json(SERVER_INTERNAL_ERROR), true);

    HttpRespStatus(HttpStatus status, String msg, boolean serviceUnAvailable) {
        this.status = status;
        this.msg = msg;
        this.serviceUnAvailable = serviceUnAvailable;
    }

    private final HttpStatus status;

    private final String msg;

    private final boolean serviceUnAvailable;

    private static final Map<HttpStatus, HttpRespStatus> MAPPING;
    static {
        MAPPING = new HashMap<>();
        for (HttpRespStatus s : values()){
            MAPPING.put(s.status, s);
        }
    }

    public static HttpRespStatus get(HttpStatus status){
        HttpRespStatus s = MAPPING.get(status);
        if (s == null){
            return DEFAULT;
        }
        return s;
    }

    /**
     * 判断服务可用状态，用于服务熔断埋点统计
     */
    public static boolean isServiceUnAvailable(int status){
        return get(HttpStatus.resolve(status)).isServiceUnAvailable();
    }

    /**
     * 判断响应码状态，是否是服务不可用
     */
    public static boolean isServiceUnAvailable(HttpStatus status){
        return get(status).isServiceUnAvailable();
    }

    private static String json(BizException e) {
        return JsonUtil.toJson(HttpResult.fail(e));
    }
}
