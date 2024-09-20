package spring.cloud.ali.common.util;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public class WebUtil {

    private static final PathPatternParser PATH_PARSER = new PathPatternParser();

    private WebUtil(){}

    /**
     * 匹配uri
     * @param pattern 匹配模式，如/users/{userId}
     * @param uri 请求uri，如/users/123
     * @return 匹配结果，非空表示匹配成功
     */
    public static PathPattern.PathMatchInfo matchUri(String pattern, String uri){
        return PATH_PARSER.parse(pattern)
                        .matchAndExtract(PathContainer.parsePath(uri));
    }
}
