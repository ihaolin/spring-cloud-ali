package spring.cloud.ali.common.component.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import spring.cloud.ali.common.dto.HttpResult;

public interface HttpResultAdvice extends ResponseBodyAdvice<Object> {

    String HTTP_RESULT_BIZ_CODE = "Hr-Biz-Code";

    @Override
    default boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType){
        return HttpResult.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    default Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response){
        if (body instanceof HttpResult) {
            HttpResult<?> httpResult = (HttpResult<?>) body;
            if (!HttpResult.isSuccess(httpResult.getCode())){
                response.getHeaders().add(HTTP_RESULT_BIZ_CODE, httpResult.getCode().toString());
            }
        }
        return body;
    }
}
