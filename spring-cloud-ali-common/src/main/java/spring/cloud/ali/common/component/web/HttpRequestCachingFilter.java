package spring.cloud.ali.common.component.web;

import com.alibaba.nacos.common.utils.IoUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 用于Http请求体缓存（当需要在Spring MVC读取请求body前或后，获取body时使用）
 */
@Slf4j
public class HttpRequestCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        ServletRequest requestWrapper = null;
        if(request instanceof HttpServletRequest) {
            requestWrapper = new RequestCachingWrapper((HttpServletRequest) request);
        }

        if(requestWrapper == null) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }
    }

    static class RequestCachingWrapper extends HttpServletRequestWrapper {

        private final String body;

        public RequestCachingWrapper(HttpServletRequest request) {
            super(request);

            try {
                body = IoUtils.toString(request.getInputStream(), "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return false;
                }
                @Override
                public boolean isReady() {
                    return false;
                }
                @Override
                public void setReadListener(ReadListener readListener) {
                }
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };

        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }
    }
}
