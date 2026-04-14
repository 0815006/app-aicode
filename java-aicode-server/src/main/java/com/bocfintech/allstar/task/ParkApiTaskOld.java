package com.bocfintech.allstar.task;

import com.bocfintech.allstar.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ParkApiTaskOld {

    @Autowired
    private RestTemplate restTemplate;

    private String token = null; // 存储登录token

    /**
     * 每天下午3点执行定时任务
     * cron表达式：秒 分 时 日 月 周 年
     * 0 0 15 * * * 表示每天15:00:00执行
     */


//    @Scheduled(cron = "0 30 8 * * *")
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void executeSchedule() {
        List<UserInfo> userList = new ArrayList<>();
//        userList.add(new UserInfo("2036377","zaj6HSat8Y1+T36Pwje6iw=="));
//        userList.add(new UserInfo("4861200","q9zGKC4Vvkn8tTcvf5e6wg==")); //阚飞
//        userList.add(new UserInfo("0695697","JUqqmqSsCokLkV4w0JPdsw==")); //刘畅
//        userList.add(new UserInfo("4685996","4lDY60r9druekO+wb/Nv6A==")); //王加丽
//        userList.add(new UserInfo("3672438","XUUBsaIze81waWXw0rAMPw==")); //阚飞的黄乐天
//        userList.add(new UserInfo("2950552","0yEaXVPYE9gvsFHCAOMVcA==")); //刘迪

        for(UserInfo userinfo: userList){
            executeParkTasks(userinfo);
        }
    }
    public void executeParkTasks(UserInfo userInfo) {
        log.info("开始执行定时任务，时间：{}", new java.util.Date());

        try {
            // 第一步：调用登录接口
            String loginResult = login(userInfo.getUserId(),userInfo.getPassword());
            if (loginResult == null || loginResult.isEmpty()) {
                log.error("登录失败，无法获取token");
                return;
            }

            // 第二步：调用apply接口
            apply(loginResult);

            // 第三步：查询结果接口
            queryResult(loginResult);

            log.info("定时任务执行完成");
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    /**
     * 调用登录接口
     */
    private String login(String userId,String password) {
        try {
            String loginUrl = "http://22.189.55.96:81/park/login";

            // 构建form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("userId", userId);
            formData.add("password", password);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                log.info("登录成功，响应内容：{}", responseBody);

                // 解析token
                return parseTokenFromResponse(responseBody);
            } else {
                log.error("登录接口调用失败，状态码：{}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("登录接口调用异常", e);
            return null;
        }
    }

    /**
     * 解析token（根据实际返回格式修改）
     */
    private String parseTokenFromResponse(String response) {
        // 根据你提供的返回格式，直接解析token
        if (response != null && response.contains("\"token\":\"")) {
            int start = response.indexOf("\"token\":\"") + 9;
            int end = response.indexOf("\"", start);
            if (start > 8 && end > start) {
                return response.substring(start, end);
            }
        }
        return null;
    }

    /**
     * 调用apply接口
     */
    private void apply(String token) {
        try {
            String applyUrl = "http://22.189.55.96:81/park/apply";

            // 构建JSON请求体
            Map<String, Object> jsonBody = new HashMap<>();
            jsonBody.put("status", "1");

            // 设置请求头 - 注意这里使用的是 "authentication" 而不是 "Authorization"
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("authentication", token); // 关键修改：使用authentication字段

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(applyUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("apply接口调用成功，响应内容：{}", response.getBody());
            } else {
                log.error("apply接口调用失败，状态码：{}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("apply接口调用异常", e);
        }
    }

    /**
     * 查询结果接口
     */
    private void queryResult(String token) {
        try {
            String queryUrl = "http://22.189.55.96:81/park/queryResult";

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("authentication", token);

            // GET请求只需要headers，不需要body
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(queryUrl, HttpMethod.GET, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                log.info("queryResult接口调用成功，响应内容：{}", responseBody);
                parseAndPrintResult(responseBody);
            } else {
                log.error("queryResult接口调用失败，状态码：{}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("queryResult接口调用异常", e);
        }
    }

    /**
     * 解析并打印预约结果
     */
    private void parseAndPrintResult(String response) {
        try {
            // 检查apply.status是否为"1"
            if (response != null && response.contains("\"status\":\"1\"")) {
                log.info("预约成功！");
                // 可以进一步提取其他信息
                log.info("预约详情：员工号={}, 车牌号={}, 停车位置={}",
                        extractValue(response, "employeeNo"),
                        extractValue(response, "plateNo"),
                        extractValue(response, "parkingNo"));
            } else if (response != null && response.contains("\"status\":\"0\"")) {
                log.warn("预约失败，状态码为0");
            } else {
                log.warn("预约失败，未找到status字段或状态不是1");
            }

            // 打印完整响应内容
            log.info("预约结果详情：{}", response);

        } catch (Exception e) {
            log.error("解析结果异常", e);
        }
    }


    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractValue(String json, String fieldName) {
        try {
            String searchKey = "\"" + fieldName + "\":\"";
            int start = json.indexOf(searchKey);
            if (start > 0) {
                start += searchKey.length();
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "null";
    }

    }
