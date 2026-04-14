package org.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpURLConnectionUtil {
    //java后台实现：调第三方接口，使用post，get，报文使用json传，返回报文也是json,写一个通用的demo，url作为参数

    /**
     * 发送GET请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应结果
     */
    public static String sendGet(String url, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            URL uri = new URL(url);
            connection = (HttpURLConnection) uri.openConnection();

            // 设置请求方法
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                throw new RuntimeException("GET请求失败，响应码: " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("GET请求异常: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送POST请求（JSON数据）
     *
     * @param url      请求URL
     * @param jsonBody 请求体JSON数据
     * @param headers  请求头
     * @return 响应结果
     */
    public static String sendPost(String url, String jsonBody, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            URL uri = new URL(url);
            connection = (HttpURLConnection) uri.openConnection();

            // 设置请求方法
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 写入请求体
            if (jsonBody != null && !jsonBody.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_CREATED) {
                return readResponse(connection);
            } else {
                throw new RuntimeException("POST请求失败，响应码: " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("POST请求异常: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送PUT请求（JSON数据）
     *
     * @param url      请求URL
     * @param jsonBody 请求体JSON数据
     * @param headers  请求头
     * @return 响应结果
     */
    public static String sendPut(String url, String jsonBody, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            URL uri = new URL(url);
            connection = (HttpURLConnection) uri.openConnection();

            // 设置请求方法
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 写入请求体
            if (jsonBody != null && !jsonBody.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return readResponse(connection);
            } else {
                throw new RuntimeException("PUT请求失败，响应码: " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("PUT请求异常: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送DELETE请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应结果
     */
    public static String sendDelete(String url, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            URL uri = new URL(url);
            connection = (HttpURLConnection) uri.openConnection();

            // 设置请求方法
            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                throw new RuntimeException("DELETE请求失败，响应码: " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("DELETE请求异常: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 读取响应内容
     *
     * @param connection HTTP连接对象
     * @return 响应字符串
     * @throws IOException IO异常
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * 读取错误响应内容
     *
     * @param connection HTTP连接对象
     * @return 错误响应字符串
     * @throws IOException IO异常
     */
    private static String readErrorResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    // 测试示例
    public static void main(String[] args) {
        // 示例1: GET请求
        try {
            System.out.println("=== GET请求示例 ===");
            Map<String, String> getHeaders = new HashMap<>();
            getHeaders.put("User-Agent", "Mozilla/5.0");
            getHeaders.put("Accept", "application/json");

            String getUrl = "https://jsonplaceholder.typicode.com/posts/1";
            String getResult = sendGet(getUrl, getHeaders);
            System.out.println("GET响应: " + getResult);
        } catch (Exception e) {
            System.err.println("GET请求失败: " + e.getMessage());
        }

        // 示例2: POST请求
        try {
            System.out.println("\n=== POST请求示例 ===");
            Map<String, String> postHeaders = new HashMap<>();
            postHeaders.put("User-Agent", "Mozilla/5.0");
            postHeaders.put("Authorization", "Bearer your-token-here");

            String postUrl = "https://jsonplaceholder.typicode.com/posts";
            String postJsonBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
            String postResult = sendPost(postUrl, postJsonBody, postHeaders);
            System.out.println("POST响应: " + postResult);
        } catch (Exception e) {
            System.err.println("POST请求失败: " + e.getMessage());
        }

        // 示例3: PUT请求
        try {
            System.out.println("\n=== PUT请求示例 ===");
            Map<String, String> putHeaders = new HashMap<>();
            putHeaders.put("User-Agent", "Mozilla/5.0");

            String putUrl = "https://jsonplaceholder.typicode.com/posts/1";
            String putJsonBody = "{\"id\":1,\"title\":\"updated-title\",\"body\":\"updated-body\",\"userId\":1}";
            String putResult = sendPut(putUrl, putJsonBody, putHeaders);
            System.out.println("POST响应: " + putResult);
        } catch (Exception e) {
            System.err.println("POST请求失败: " + e.getMessage());
        }
    }
}