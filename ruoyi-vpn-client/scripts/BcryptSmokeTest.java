import org.springframework.security.crypto.bcrypt.BCrypt;

public class BcryptSmokeTest {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        System.out.println("HASH=" + hash);
        System.out.println("MATCH=" + BCrypt.checkpw("admin123", hash));
        String fixed = "$2a$10$ogDPlVsaKYNLzcKYZ/x7gOR4jXlMgI9bADOQtRLpMqasX/yW1ZrFO";
        System.out.println("REHASH=" + BCrypt.hashpw("admin123", fixed));
        System.out.println("FIXED_MATCH=" + BCrypt.checkpw("admin123", fixed));
        System.out.println("VECTOR="
            + BCrypt.checkpw("password", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
    }
}
