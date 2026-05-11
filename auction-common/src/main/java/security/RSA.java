package security;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {
    // Hàm tạo khóa public và private
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // Mã hóa dữ liệu bằng Public Key(Dùng ở phía Client)
    public static String encrypt(String data, String publicKeystr) throws Exception {
        // Chuyển String Base64 thành Public Key object
        byte[] publicBytes = Base64.getDecoder().decode(publicKeystr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        //Tiến hành mã hóa
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        //Trả vè chuỗi Base64 gửi qua mạng
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    //Giải mã dữ liệu bằng Private Key(Dùng ở phía Server)
    public static String decrypt(String encryptedData, PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }
    // Hàm tiện ích để chuyển PrivateKey object thành String (để lưu trữ nếu cần)
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}