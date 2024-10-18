package spring.cloud.ali.common.component.web;

import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.cloud.ali.common.context.LoginContext;
import spring.cloud.ali.common.context.LoginUser;


import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 从请求头中，获取用户信息（来自网关埋点）
        String loginUserId = request.getHeader(HTTP_HEADER_LOGIN_USER_ID);
        if (!Strings.isNullOrEmpty(loginUserId)){
            LoginContext.set(new LoginUser(Long.valueOf(loginUserId)));
        }

        // 微服务不做认证拦截

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContext.clear();
    }
}
