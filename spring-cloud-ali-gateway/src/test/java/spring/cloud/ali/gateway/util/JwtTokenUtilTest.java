package spring.cloud.ali.gateway.util;

import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import org.junit.Test;
import spring.cloud.ali.common.util.JwtTokenUtil;

import static org.junit.Assert.assertNotNull;
import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;

public class JwtTokenUtilTest {

    @Test
    public void testGenerate(){
        String loginToken = JwtTokenUtil.generate("vW6fv8ADmE3E6UjXsu8jxuBdO6+5Mkn67M+SELwMy6E=", ImmutableMap.of(HTTP_HEADER_LOGIN_USER_ID, "1"), 3600);
        assertNotNull(loginToken);
        System.out.println(loginToken);
    }

    @Test
    public void testGenerateSignKey(){
        System.out.println(JwtTokenUtil.generateSignKey());
    }
}
