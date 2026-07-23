已在 ParkingBookController 中新增 /api/parking/book/triggerSchedule 接口，通过 type 参数（autoEnable / execute / check）一键批量触发 ParkBookTask 中的3个定时任务。

Postman 使用方式：

POST /api/parking/book/triggerSchedule?type=autoEnable — 批量恢复自动预约（对应 8:00 定时任务）
POST /api/parking/book/triggerSchedule?type=execute — 批量执行预约（对应 8:30 定时任务）
POST /api/parking/book/triggerSchedule?type=check — 批量核查当天记录（对应 17:05 定时任务）
无需 token，Postman 直接调用即可兜底。

你写的邮件测试接口是 [`testSendMail`](java-aicode-server/src/main/java/com/bocfintech/allstar/controller/ParkingBookController.java:242)，位于 `ParkingBookController` 中：

- **地址**：`GET /api/parking/book/testSendMail?empNo=工号`
- **默认**：不传 empNo 时默认 `2036377`
- **功能**：查配置 + 最新预约记录，通过 `BankEmailPlaywrightService.sendParkingNotification()` 发送带车位截图的邮件

Postman 调用示例：
```
GET http://localhost:8080/api/parking/book/testSendMail?empNo=2036377
```