package com.bocfintech.allstar.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.mapper.ComWorkholidayMapper;
import com.bocfintech.allstar.mapper.ParkingRecordMapper;
import com.bocfintech.allstar.service.ParkingBookService;
import com.bocfintech.allstar.service.ParkingRecordService;
import com.bocfintech.allstar.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ParkBookTask {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ParkingBookService parkingBookService;

    @Autowired
    private ParkingRecordService parkingRecordService;

    @Autowired
    ComWorkholidayMapper comWorkholidayMapper;

    @Autowired
    private ParkingRecordMapper parkingRecordMapper;

    private String token = null; // 存储登录token

    /**
     * 每天上午8点半执行定时任务
     * cron表达式：秒 分 时 日 月 周 年
     * 0 30 8 * * * 表示每天8:30:00执行
     */

    @Scheduled(cron = "0 30 8 * * *")
    public void executeSchedule() {

        LocalDate localDate = LocalDate.now();
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if("0".equals(comWorkholidayMapper.getStatusByDate(date))){
            //今天是假日
            return;
        }
        // 构建查询条件
        LambdaQueryWrapper<ParkingBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParkingBook::getAutoBook, 1); // 只执行book状态为1 需要预约的

        List<ParkingBook> list = parkingBookService.list(wrapper);

        for(ParkingBook book: list){
            executeParkTasks(book.getEmpNo(),book.getPassHash());
        }
    }

    /**
     * 核查当天记录是不是有变化
     * 每天上午17点10分执行定时任务
     * cron表达式：秒 分 时 日 月 周 年
     * 0 5 17 * * * 表示每天17:05:00执行
     */
    @Scheduled(cron = "0 5 17 * * *")
    public void checkSchedule() {

        LocalDate localDate = LocalDate.now();
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if("0".equals(comWorkholidayMapper.getStatusByDate(date))){
            //今天是假日
            return;
        }
        try {
            log.info("开始执行每日预约记录检查定时任务");

            // 获取今天的日期
            LocalDate today = LocalDate.now();

            // 查询所有自动预约状态为1的配置记录
            LambdaQueryWrapper<ParkingBook> bookWrapper = new LambdaQueryWrapper<>();
            bookWrapper.eq(ParkingBook::getAutoBook, 1);
            List<ParkingBook> autoBookConfigs = parkingBookService.list(bookWrapper);

            if (autoBookConfigs == null || autoBookConfigs.isEmpty()) {
                log.info("今天没有需要检查的自动预约配置");
                return;
            }

            log.info("找到 {} 个需要检查的自动预约配置", autoBookConfigs.size());

            // 逐个循环检查每个用户的预约状态
            for (ParkingBook config : autoBookConfigs) {
                checkParkingBook(config);
            }

        	log.info("每日预约记录检查定时任务执行完成");

        } catch (Exception e) {
            log.error("执行每日预约记录检查定时任务失败", e);
            throw new RuntimeException("定时任务执行失败", e);
        }
    }

    /**
     * 执行预约车辆
     */
    public void executeParkTasks(String userId,String passwork) {
        log.info("开始执行定时任务，时间：{}", new Date());
        ParkingRecord err = new ParkingRecord();
        err.setEmpNo(userId);
        err.setResult("失败");
        try {
            // 第一步：调用登录接口
            String token = login(userId, passwork);
            if (token == null || token.isEmpty()) {
                log.error("登录失败，无法获取token");
                err.setResultDesc("登录失败");
                parkingRecordMapper.insert(err);
                return;
            }
            // 第二步：调用apply接口
            apply(token);

            // 第三步：查询结果接口
            ParkingRecord record = queryResult(token);
            if(record ==null){
                err.setResultDesc("查询失败");
                parkingRecordMapper.insert(err);
                return;
            }
            parkingRecordMapper.insert(record);

            log.info("定时任务执行完成");
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    /**
     * 执行取消预约车辆
     */
    public void executeCancelParkTasks(String userId,String passwork) {
        log.info("开始执行定时任务，时间：{}", new Date());
        ParkingRecord err = new ParkingRecord();
        err.setEmpNo(userId);
        err.setResult("失败");
        try {
            // 第一步：调用登录接口
            String token = login(userId, passwork);
            if (token == null || token.isEmpty()) {
                log.error("登录失败，无法获取token");
                err.setResultDesc("登录失败");
                parkingRecordMapper.insert(err);
                return;
            }
            // 第二步：调用apply接口
            cancelBook(token);

            // 第三步：查询结果接口
            ParkingRecord record = queryResult(token);
            if(record ==null){
                err.setResultDesc("查询失败");
                parkingRecordMapper.insert(err);
                return;
            }
            parkingRecordMapper.insert(record);

            log.info("定时任务执行完成");
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    /**
     * 检查用户预约记录-逐个检查
     */
    public void checkParkingBook(ParkingBook config) {

            String userId = config.getEmpNo();
            String passHash =config.getPassHash();
            try {

                // 1. 调用登录接口
                String token = login(userId, passHash);
                if (token == null || token.isEmpty()) {
                    log.error("登录失败，无法获取token");
                    return;
                }

                // 2. 调用queryResult接口获取最新的预约结果-调接口
                ParkingRecord latestResult = queryResult(token);
                if (latestResult == null) {
                    log.warn("用户 {} 无法获取最新的预约结果", userId);
                    return;
                }

                // 3. 查询该用户当天最后一条记录
                ParkingRecord lastRecord = getLastRecordOfToday(userId);

                if (lastRecord != null) {
                    // 4. 比较结果是否一致
                    if (!lastRecord.getResult().equals(latestResult.getResult())) {
                        // 状态不一致，插入新记录
                        parkingRecordMapper.insert(latestResult);
                    } else {
                        log.info("用户 {} 当天预约状态无变化，无需插入新记录", userId);
                    }
                } else {
                    // 如果没有当天记录，则插入第一条记录
                    parkingRecordMapper.insert(latestResult);
                }
            } catch (Exception e) {
                log.error("检查用户 {} 预约状态时发生异常", userId, e);
            }
    }

    /**
     * 查询用户当天最后一条记录
     */
    private ParkingRecord getLastRecordOfToday(String empNo) {
        try {
            // 获取今天的日期
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

            LambdaQueryWrapper<ParkingRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ParkingRecord::getEmpNo, empNo)
                    .ge(ParkingRecord::getCreateTime, startDate)
                    .le(ParkingRecord::getCreateTime, endDate)
                    .orderByDesc(ParkingRecord::getCreateTime)
                    .last("LIMIT 1");

            return parkingRecordMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("查询用户 {} 当天记录失败", empNo, e);
            return null;
        }
    }


    /**
     * 验证登录接口密码正确性
     */
    public boolean validatePassword(String userId, String password) {
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

                // 检查returnCode是否为"0"
                if (responseBody != null && responseBody.contains("\"returnCode\":\"0\"")) {
                    log.info("登录成功，响应内容：{}", responseBody);
                    return true;
                }else{
                    log.info("登录失败，响应内容：{}", responseBody);
                    return false;
                }
            } else {
                log.error("登录接口调用失败，状态码：{}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("登录接口调用异常", e);
            return false;
        }
    }


    /**
     * 调用登录接口，返回token
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
                // 检查returnCode是否为"0"
                if (responseBody != null && responseBody.contains("\"returnCode\":\"0\"")) {
                    log.info("登录成功，响应内容：{}", responseBody);
                    // 解析token
                    return parseTokenFromResponse(responseBody);
                }else{
                    log.info("登录失败，响应内容：{}", responseBody);
                    return null;
                }
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
     * 调用apply接口-取消预约
     */
    private void cancelBook(String token) {
        try {
            String applyUrl = "http://22.189.55.96:81/park/apply";

            // 构建JSON请求体
            Map<String, Object> jsonBody = new HashMap<>();
            jsonBody.put("status", "0");

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
     * 查询结果接口, 返回最新查到的 ParkingRecord
     */
    private ParkingRecord queryResult(String token) {
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
                return parseAndPrintResult(responseBody);
            } else {
                log.error("queryResult接口调用失败，状态码：{}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("queryResult接口调用异常", e);
        }
        return null;
    }


    /**
     * 解析预约结果，response 内容输出到 ParkingRecord
     */
    private ParkingRecord parseAndPrintResult(String response) {
        ParkingRecord record = new ParkingRecord();
        try {
            // 检查apply.status是否为"1"
            if (response != null ) {

                // 可以进一步提取其他信息
                String empNo = JsonUtils.getJsonString(response, "data", "employeeInfo", "employeeNo");
                record.setEmpNo(empNo);//工号
                String parkingNoA = JsonUtils.getJsonString(response, "data", "carPlate", "parkingNo");
                String parkingNoB = JsonUtils.getJsonString(response, "data", "result", "parkingNo");
                String parkingType = JsonUtils.getJsonString(response, "data", "carPlate", "type");
                String plateNo =JsonUtils.getJsonString(response, "data", "carPlate", "plateNo");

                record.setParkingType(parkingType);//停车证类型: A, B
                String parkingNo = "A".equals(parkingType) ? parkingNoA : parkingNoB ;

                record.setParkingPosition(parkingNo);//车位位置
                record.setPlateNo(plateNo);//车牌号

                record.setAppointmentDate(JsonUtils.getJsonString(response, "data", "apply", "enterDay"));//预约入园日期
                record.setUsername(JsonUtils.getJsonString(response, "data", "employeeInfo", "name"));//用户姓名
                record.setDepartment(JsonUtils.getJsonString(response, "data", "employeeInfo", "depart"));//用户部门
                record.setPhone(JsonUtils.getJsonString(response, "data", "employeeInfo", "mobilePhone"));//用户手机号

                String message = JsonUtils.getJsonString(response, "data", "result", "message");

                String bookStatus = "";
                if(response.contains("\"status\"")){
                    bookStatus = JsonUtils.getJsonString(response, "data", "apply", "status"); // 1-预约成功，0-未预约
                    log.info("预约成功！");
                    record.setResultDesc("预约成功");

                    if("1".equals(bookStatus)){
                        if("未中签".equals(message)){
                            record.setResult("未中签");
                        }else if("不入园".equals(message)){
                            record.setResult("未预约");
                        }{
                            record.setResult("已成功预约");
                        }
                    }else if("0".equals(bookStatus)){
                        log.warn("未预约，状态码为0");
                        record.setResult("未预约");
                        record.setResultDesc("未预约，状态码为0");
                    } else{
                        log.warn("状态码异常");
                        record.setResult("未知状态码");
                        record.setResultDesc("未知状态码");
                    }

                }else if("未提交入园申请".equals(message)){
                    record.setResult("未提交入园申请");
                } else {
                    log.warn("预约失败，未找到status字段或状态不是1");
                }
                // 读取 message 从json中读

                log.info("预约详情：员工号={}, 车牌号={}, 停车位置={}", empNo, plateNo, parkingNo);

            }

            // 打印完整响应内容
            log.info("预约结果详情：{}", response);

        } catch (Exception e) {
            log.error("解析结果异常", e);
        }
        return record;
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

}
