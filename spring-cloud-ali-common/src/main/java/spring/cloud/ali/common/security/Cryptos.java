package spring.cloud.ali.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

/**
 * The crypto util based on Spring Security
 * <a href="https://docs.spring.io/spring-security/reference/features/integrations/cryptography.html">...</a>
 */
public class Cryptos {

    private static final BytesKeyGenerator BKG = KeyGenerators.secureRandom();

    private static final StringKeyGenerator SKG = KeyGenerators.string();

    private static final BCryptPasswordEncoder BCPE = new BCryptPasswordEncoder(16);

    private static final Pbkdf2PasswordEncoder PBKD = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    private Cryptos(){}

    public static byte[] genByteKey(){
        return BKG.generateKey();
    }

    public static String genStrKey(){
        return SKG.generateKey();
    }

    public static PasswordEncoder bcrypt(){
        return BCPE;
    }

    public static PasswordEncoder pbkdf2(){
        return PBKD;
    }
}
