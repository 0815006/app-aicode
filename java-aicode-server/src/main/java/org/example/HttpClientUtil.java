package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpClientUtil {

    private static final String UTF_8 = "UTF-8";

    public static String sendGet(String url, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            setHeaders(request, headers);

            try (CloseableHttpResponse response = client.execute(request)) {
                // 检查响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return EntityUtils.toString(response.getEntity(), UTF_8);
                } else {
                    throw new IOException("GET request failed with status code: " + statusCode);
                }
            }
        }
    }

    public static String sendPost(String url, String jsonBody, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            setHeaders(request, headers);
            request.setEntity(new StringEntity(jsonBody, UTF_8));

            try (CloseableHttpResponse response = client.execute(request)) {
                // 检查响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return EntityUtils.toString(response.getEntity(), UTF_8);
                } else {
                    throw new IOException("POST request failed with status code: " + statusCode);
                }
            }
        }
    }

    public static String sendPut(String url, String jsonBody, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(url);
            setHeaders(request, headers);
            request.setEntity(new StringEntity(jsonBody, UTF_8));

            try (CloseableHttpResponse response = client.execute(request)) {
                // 检查响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return EntityUtils.toString(response.getEntity(), UTF_8);
                } else {
                    throw new IOException("PUT request failed with status code: " + statusCode);
                }
            }
        }
    }

    public static String sendDelete(String url, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(url);
            setHeaders(request, headers);

            try (CloseableHttpResponse response = client.execute(request)) {
                // 检查响应状态码
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return EntityUtils.toString(response.getEntity(), UTF_8);
                } else {
                    throw new IOException("DELETE request failed with status code: " + statusCode);
                }
            }
        }
    }

    private static void setHeaders(org.apache.http.client.methods.HttpUriRequest request, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void main(String[] args) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("User-Agent", "Mozilla/5.0");

            String result = sendGet("https://jsonplaceholder.typicode.com/posts/1", headers);
            System.out.println("GET Response: " + result);

            String postBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
            String postResult = sendPost("https://jsonplaceholder.typicode.com/posts", postBody, headers);
            System.out.println("POST Response: " + postResult);

        } catch (IOException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
