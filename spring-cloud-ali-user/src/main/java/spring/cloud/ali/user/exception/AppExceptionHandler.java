package spring.cloud.ali.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import spring.cloud.ali.common.exception.WebExceptionHandler;
import spring.cloud.ali.common.component.web.HttpResultAdvice;

/**
 * 统一异常处理器
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class AppExceptionHandler extends WebExceptionHandler implements HttpResultAdvice {

}
