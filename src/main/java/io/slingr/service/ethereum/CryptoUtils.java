package io.slingr.service.ethereum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Created by dgaviola on 01/08/18.
 */
public class CryptoUtils {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtils.class);

    private SecretKey secretKey;
    private Cipher ecipher;
    private Cipher dcipher;

    public CryptoUtils(String password) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        byte[] encodedKey = org.apache.commons.codec.binary.Base64.decodeBase64(password);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), encodedKey, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        this.secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        this.ecipher = Cipher.getInstance("AES");
        this.dcipher = Cipher.getInstance("AES");
        this.ecipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
        this.dcipher.init(Cipher.DECRYPT_MODE, this.secretKey);
    }

    public String encrypt(String plaintext) {
        try {
            byte[] encryptedBytes = ecipher.doFinal(plaintext.getBytes("UTF8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            return plaintext;
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            return new String(dcipher.doFinal(decodedBytes), "UTF8");
        } catch (Exception e) {
            return ciphertext;
        }
    }
}
