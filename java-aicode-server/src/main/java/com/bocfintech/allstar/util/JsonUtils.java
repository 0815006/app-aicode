package com.bocfintech.allstar.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 逐层获取JSON节点值
     * @param jsonString JSON字符串
     * @param keys 层级键名，可变参数
     * @return 节点值，如果路径不存在或为null返回null
     */
    public static String getJsonString(String jsonString, String... keys) {
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                log.warn("JSON字符串为空");
                return null;
            }

            JsonNode currentNode = objectMapper.readTree(jsonString);

            // 逐层遍历
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                // 如果当前节点为null，直接返回null
                if (currentNode == null || currentNode.isNull()) {
                    log.warn("JSON路径 {} 中的节点为null", String.join(".", keys));
                    return null;
                }

                // 获取下一层节点
                currentNode = currentNode.get(key);

                // 如果找不到该节点，返回null
                if (currentNode == null) {
                    log.warn("JSON路径 {} 中的节点 {} 不存在", String.join(".", keys), key);
                    return null;
                }
            }


            // 返回最终节点的文本值
            if (currentNode != null && !currentNode.isNull()) {
                return currentNode.asText();
            }

            return null;

        } catch (Exception e) {
            log.error("从JSON中提取值失败，路径: {}", String.join(".", keys), e);
            return null;
        }
    }
}
