package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.UserInfo;
import com.bocfintech.allstar.task.ParkApiTaskOld;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/task")
@Slf4j
public class TaskController {

    @Autowired
    private ParkApiTaskOld parkApiTask;

    /**
     * 手动触发park接口定时任务
     */
    @PostMapping("/executeParkTasks")
    public ResultBean executeParkTasks(@RequestHeader(value = "token", required = false) String token) {
        try {
            log.info("手动触发定时任务");
            if (token == null || token.trim().isEmpty()) {
                return ResultBean.error("未登录或Token无效");
            }
            String userId = token.trim();
            UserInfo user = new UserInfo(userId,"zaj6HSat8Y1+T36Pwje6iw==");
            // parkApiTask.executeParkTasks(user);


            // 测试数据
            String ciphertextBase64 = "zaj6HSat8Y1+T36Pwje6iw=="; // 注意：这里应该放真实的密文，而不是工号

            try {
                // 1. 定义原始密钥和 IV
                String keyStr = "ihaierForTodoKey";
                String ivStr = "ihaierForTodo_Iv";

                // 2. 【关键步骤】处理密钥和 IV 长度，确保为 16 字节 (对应 Python 的 pad(..., 16)[:16])
                byte[] keyBytes = processBytes(keyStr);
                byte[] ivBytes = processBytes(ivStr);

                // 3. 初始化 Cipher
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

                // 4. Base64 解码
                byte[] encryptedBytes = java.util.Base64.getDecoder().decode(ciphertextBase64);

                // 5. 解密
                byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

                // 6. 输出结果
                String result = new String(decryptedBytes, StandardCharsets.UTF_8);
                System.out.println("解密成功：" + result);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("AES Decryption failed", e);
            }
        } catch (Exception e) {
            log.error("手动触发定时任务失败", e);
            return ResultBean.error("定时任务执行失败: " + e.getMessage());
        }
           return ResultBean.success("定时任务执行成功");

}

    /**
     * 辅助方法：将字符串转换为 16 字节数组
     * 逻辑：如果长度 < 16，末尾补 0；如果 > 16，截断前 16 位。
     */
    private static byte[] processBytes(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 16) {
            byte[] padded = new byte[16];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            return padded;
        } else {
            return Arrays.copyOf(bytes, 16);
        }
    }
}
