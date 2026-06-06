package com.bocfintech.allstar.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-CBC 加解密工具类
 * 与前端 CryptoJS.AES.encrypt 保持一致
 * 密钥: ihaierForTodoKey (16字节)
 * IV:   ihaierForTodo_Iv  (16字节)
 */
public class AesUtils {

    private static final String KEY = "ihaierForTodoKey";
    private static final String IV  = "ihaierForTodo_Iv";

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * AES-CBC 加密
     * @param plainText 明文
     * @return Base64 密文；明文为空则返回空串
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES-CBC 解密
     * @param cipherText Base64 密文（前端 CryptoJS.AES.encrypt 输出）
     * @return 明文；密文为空则返回空串
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return "";
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
}
