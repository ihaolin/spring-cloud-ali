package spring.cloud.ali.common.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WebUtilTest {

    @Test
    public void testMatchPatten(){
        assertNotNull(WebUtil.matchPatten("/users/{userId}", "/users/1"));
    }

    @Test
    public void testMatchPatten_false(){
        assertNull(WebUtil.matchPatten("/users/{userId}", "/users/x/1"));
    }

    @Test
    public void testMatchUri_notPatten(){
        assertNotNull(WebUtil.matchPatten("users:{userId}", "users:1"));
    }

    @Test
    public void testMatchUri_notPatten_false(){
        assertNull(WebUtil.matchPatten("users:{userId}", "users1"));
    }
}
