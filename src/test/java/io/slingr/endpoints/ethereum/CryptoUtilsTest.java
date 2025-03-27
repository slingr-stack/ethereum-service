package io.slingr.endpoints.ethereum;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by dgaviola on 31/07/18.
 */
public class CryptoUtilsTest {
    private static CryptoUtils cryptoUtils;

    @BeforeClass
    public static void init() throws Exception {
        cryptoUtils = new CryptoUtils("jdU72Jus72bnfOnzA82su!8s_27hsN0jsy#");
    }

    @Test
    public void testEncryptionAndDecryption() {
        String privateKey = "49abb5988f0a6f06079c873a7bd1a962bc6d9ea84466a8a1b1b0382c172c131c";
        String encryptedPrivateKey = cryptoUtils.encrypt(privateKey);
        System.out.println(encryptedPrivateKey);
        assertNotNull(encryptedPrivateKey);
        assertNotEquals(encryptedPrivateKey, privateKey);
        String decryptedPrivateKey = cryptoUtils.decrypt(encryptedPrivateKey);
        assertNotNull(decryptedPrivateKey);
        assertEquals(privateKey, decryptedPrivateKey);
    }
}
