package spring.cloud.ali.common.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WebUtilTest {

    @Test
    public void testMatchUri(){
        assertNotNull(WebUtil.matchUri("/users/{userId}", "/users/1"));
    }

    @Test
    public void testMatchUri_false(){
        assertNull(WebUtil.matchUri("/users/{userId}", "/users/x/1"));
    }

    @Test
    public void testMatchUri_notUri(){
        assertNotNull(WebUtil.matchUri("users:{userId}", "users:1"));
    }

    @Test
    public void testMatchUri_notUri_false(){
        assertNull(WebUtil.matchUri("users:{userId}", "users1"));
    }
}
