package org.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpURLConnectionOptimizeUtil{

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 5000;

    /**
     * 发送HTTP请求（GET/POST/PUT/DELETE）
     *
     * @param method   HTTP方法
     * @param url      请求地址
     * @param body     请求体（仅用于POST/PUT）
     * @param headers  请求头
     * @param timeout  超时时间（毫秒）
     * @return 响应字符串
     */
    public static String sendRequest(String method, String url, String body, Map<String, String> headers, int timeout) {
        HttpURLConnection connection = null;
        try {
            URL uri = new URL(url);
            connection = (HttpURLConnection) uri.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            // 设置 Content-Type 和 Accept 头部
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
            }

            // 设置自定义头部
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 写入请求体
            if (body != null && !body.isEmpty() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();
            String responseBody;

            if (responseCode >= 200 && responseCode < 300) {
                responseBody = readResponse(connection);
            } else {
                responseBody = readErrorResponse(connection);
                throw new RuntimeException("HTTP " + method + " request failed with code: " + responseCode + ", message: " + responseBody);
            }

            return responseBody;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute HTTP " + method + " request to " + url + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String sendGet(String url, Map<String, String> headers) {
        return sendRequest("GET", url, null, headers, DEFAULT_CONNECT_TIMEOUT);
    }

    public static String sendPost(String url, String jsonBody, Map<String, String> headers) {
        return sendRequest("POST", url, jsonBody, headers, DEFAULT_CONNECT_TIMEOUT);
    }

    public static String sendPut(String url, String jsonBody, Map<String, String> headers) {
        return sendRequest("PUT", url, jsonBody, headers, DEFAULT_CONNECT_TIMEOUT);
    }

    public static String sendDelete(String url, Map<String, String> headers) {
        return sendRequest("DELETE", url, null, headers, DEFAULT_CONNECT_TIMEOUT);
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        return readStream(connection.getInputStream());
    }

    private static String readErrorResponse(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            return readStream(errorStream);
        }
        return "";
    }

    private static String readStream(InputStream inputStream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
        }
        return response.toString().trim();
    }

    // 测试示例
    public static void main(String[] args) {
        try {
            System.out.println("=== GET请求示例 ===");
            Map<String, String> getHeaders = new HashMap<>();
            getHeaders.put("User-Agent", "Mozilla/5.0");
            getHeaders.put("Accept", "application/json");

            String getResult = sendGet("https://jsonplaceholder.typicode.com/posts/1", getHeaders);
            System.out.println("GET响应: " + getResult);

            System.out.println("\n=== POST请求示例 ===");
            Map<String, String> postHeaders = new HashMap<>();
            postHeaders.put("User-Agent", "Mozilla/5.0");
            postHeaders.put("Authorization", "Bearer your-token-here");

            String postJsonBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
            String postResult = sendPost("https://jsonplaceholder.typicode.com/posts", postJsonBody, postHeaders);
            System.out.println("POST响应: " + postResult);

            System.out.println("\n=== PUT请求示例 ===");
            String putJsonBody = "{\"id\":1,\"title\":\"updated-title\",\"body\":\"updated-body\",\"userId\":1}";
            String putResult = sendPut("https://jsonplaceholder.typicode.com/posts/1", putJsonBody, postHeaders);
            System.out.println("PUT响应: " + putResult);

        } catch (Exception e) {
            System.err.println("请求失败: " + e.getMessage());
        }
    }
}
