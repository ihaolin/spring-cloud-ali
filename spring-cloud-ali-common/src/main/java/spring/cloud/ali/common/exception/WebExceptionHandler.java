package spring.cloud.ali.common.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.google.common.base.Throwables;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;
import spring.cloud.ali.common.dto.HttpResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.lang.reflect.UndeclaredThrowableException;

import static spring.cloud.ali.common.exception.BizException.PATH_NOT_FOUND;
import static spring.cloud.ali.common.exception.BizException.REQUEST_TOO_MANY;
import static spring.cloud.ali.common.exception.BizException.SERVER_INTERNAL_ERROR;

/**
 * 应用服务统一异常处理器
 */
@Slf4j
@ResponseBody
public class WebExceptionHandler {

    /**
     * Sentinel拦截异常
     */
    @ExceptionHandler({BlockException.class})
    public HttpResult<?> blockException(
            HttpServletRequest req, HttpServletResponse resp, BlockException e) {
        log.warn("{} sentinel block exception: {}", req.getRemoteAddr(), e.getRule());

        BizException bizException = SERVER_INTERNAL_ERROR;
        if (e instanceof FlowException){
            bizException = REQUEST_TOO_MANY;
        }

        return HttpResult.fail(bizException);
    }

    /**
     * 内部业务异常
     */
    @ExceptionHandler({BizException.class})
    public HttpResult<?> bizException(BizException e) {
        return HttpResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 访问路径不存在
     */
    @ExceptionHandler({NoHandlerFoundException.class})
    public HttpResult<?> noHandlerFoundException(
            HttpServletRequest req, HttpServletResponse resp, NoHandlerFoundException e) {
        log.warn("{} is accessing invalid path: {}", req.getRemoteAddr(), e.getRequestURL());
        resp.setStatus(PATH_NOT_FOUND.getCode());
        return HttpResult.fail(PATH_NOT_FOUND);
    }

    /**
     * 未声明异常，会被Spring包装成UndeclaredThrowableException
     */
    @ExceptionHandler({UndeclaredThrowableException.class})
    public HttpResult<?> undeclaredThrowableException(
            HttpServletRequest req, HttpServletResponse resp, UndeclaredThrowableException e) {

        Throwable wrapper = e.getUndeclaredThrowable();
        if (wrapper instanceof BlockException){
            return blockException(req, resp, ((BlockException) wrapper));
        }

        return unknownException(req, resp, wrapper);
    }

    /**
     * Feign远端调用异常
     */
    @ExceptionHandler({FeignException.class})
    public HttpResult<?> feignException(HttpServletRequest req, HttpServletResponse resp, FeignException e) {
        log.error("feign exception: {} {}", req.getRemoteAddr(), Throwables.getStackTraceAsString(e));
        return HttpResult.fail(SERVER_INTERNAL_ERROR);
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler({Throwable.class})
    public HttpResult<?> unknownException(HttpServletRequest req, HttpServletResponse resp, Throwable e) {
        log.error("unknown exception: {}", Throwables.getStackTraceAsString(e));
        return HttpResult.fail(SERVER_INTERNAL_ERROR);
    }
}
