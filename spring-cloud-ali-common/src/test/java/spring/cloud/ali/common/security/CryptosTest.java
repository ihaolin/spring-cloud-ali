package spring.cloud.ali.common.security;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CryptosTest {

    @Test
    public void testGenByteKey(){
        byte[] keys = Cryptos.genByteKey();
        assertNotNull(keys);
        System.out.println(Arrays.toString(keys));
    }

    @Test
    public void testGenStrKey(){
        String keys = Cryptos.genStrKey();
        assertNotNull(keys);
        System.out.println(keys);
    }

    @Test
    public void testBcryptPwdEncode(){
        String pwd = "123456";
        String encryptPwd = Cryptos.bcrypt().encode(pwd);
        assertTrue(Cryptos.bcrypt().matches(pwd, encryptPwd));
    }

    @Test
    public void testPbkPwdEncode(){
        String pwd = "123456";
        String encryptPwd = Cryptos.pbkdf2().encode(pwd);
        assertTrue(Cryptos.pbkdf2().matches(pwd, encryptPwd));
    }
}
