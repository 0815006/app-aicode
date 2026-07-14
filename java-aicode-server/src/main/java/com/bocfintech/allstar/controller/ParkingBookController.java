package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.entity.ParkingBookDTO;
import com.bocfintech.allstar.entity.ParkingBookWithRusult;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.service.ParkingBookService;
import com.bocfintech.allstar.service.ParkingRecordService;
import com.bocfintech.allstar.service.impl.BankEmailPlaywrightService;
import com.bocfintech.allstar.task.ParkBookTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/parking/book")
public class ParkingBookController {

    @Autowired
    private ParkingBookService parkingBookService;

    @Autowired
    private ParkBookTask parkBookTask;

    @Autowired
    private ParkingRecordService parkingRecordService;

    @Autowired
    private BankEmailPlaywrightService bankEmailPlaywrightService;

    /**
     * 查询所有车位预约记录（不返回密码哈希）
     */
    @GetMapping("/list")
    public ResultBean<List<ParkingBookWithRusult>> listAll() {
        try {
            List<ParkingBookWithRusult> list = parkingBookService.getAllRecords();
            return ResultBean.success(list);
        } catch (Exception e) {
            return ResultBean.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增车位预约
     */
    @PostMapping("/create")
    public ResultBean<String> create(@RequestBody ParkingBookDTO dto) {
        try {
            ParkingBook record = new ParkingBook();
            record.setEmpNo(dto.getEmpNo());
            record.setUsername(dto.getUsername());
            record.setPassHash(dto.getPassHash()); // 前端已加密
            record.setAutoBook(dto.getAutoBook());
            record.setNextAutoBookDate(dto.getNextAutoBookDate());
            record.setBookWeekdays(dto.getBookWeekdays() != null ? dto.getBookWeekdays() : "1,2,3,4,5");
            record.setEmailEnabled(dto.getEmailEnabled() != null ? dto.getEmailEnabled() : 0);
            record.setEmailUser(dto.getEmailUser());
            record.setEmailPassword(dto.getEmailPassword());
            record.setEmailRecipient(dto.getEmailRecipient());
            parkingBookService.save(record);
            return ResultBean.success("新增成功");
        } catch (Exception e) {
            return ResultBean.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 修改车位预约（密码可选）
     */
    @PutMapping("/update")
    public ResultBean<String> update(@RequestBody ParkingBookDTO dto) {
        try {
            ParkingBook existing = parkingBookService.getById(dto.getEmpNo());
            if (existing == null) {
                return ResultBean.error("用户不存在");
            }

            existing.setUsername(dto.getUsername());
            existing.setAutoBook(dto.getAutoBook());
            existing.setNextAutoBookDate(dto.getNextAutoBookDate());
            if (dto.getBookWeekdays() != null && !dto.getBookWeekdays().isEmpty()) {
                existing.setBookWeekdays(dto.getBookWeekdays());
            }
            if (dto.getPassHash() != null && !dto.getPassHash().isEmpty()) {
                existing.setPassHash(dto.getPassHash());
            }
            existing.setEmailEnabled(dto.getEmailEnabled() != null ? dto.getEmailEnabled() : 0);
            existing.setEmailUser(dto.getEmailUser());
            if (dto.getEmailPassword() != null && !dto.getEmailPassword().isEmpty()) {
                existing.setEmailPassword(dto.getEmailPassword());
            }
            existing.setEmailRecipient(dto.getEmailRecipient());
            parkingBookService.updateById(existing);

            return ResultBean.success("修改成功");
        } catch (Exception e) {
            return ResultBean.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 1. 查询本人配置 (单条)
     * 从 Header 获取 token (emp_no)
     */
    @GetMapping("/queryMyConfig")
    public ResultBean queryMyConfig(@RequestHeader("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
        }

        ParkingBook config = parkingBookService.getById(token);
        if (config == null) {
            // 如果不存在，返回空对象或提示创建
            return ResultBean.success(new ParkingBook());
        }
        return ResultBean.success(config);
    }

    /**
     * 2. 修改本人配置 (使用密码校验)
     * 前端传入 emp_no, username, passwordHash, autoBook
     * 后端强制密码是否正确
     */
    @PostMapping("/saveOrUpdate")
    public ResultBean saveOrUpdate(@RequestBody ParkingBook config) {
        if (!StringUtils.hasText(config.getPassHash())) {
            return ResultBean.error(ErrorEnum.参数异常, "修改配置信息需要校验密码！");
        }        
        if (!StringUtils.hasText(config.getNextAutoBookDate())) {
            config.setNextAutoBookDate(null);
        }
        boolean success = parkingBookService.updateMyConfig(config);
        return success ? ResultBean.success("保存成功") : ResultBean.error(ErrorEnum.操作失败, "保存失败，请检查用户密码");
    }

    /**
     * 3. 删除本人配置
     */
    @PostMapping("/delete")
    public ResultBean deleteConfig(@RequestHeader("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResultBean.error(ErrorEnum.参数异常, "未登录");
        }

        boolean success = parkingBookService.removeById(token);
        return success ? ResultBean.success("删除成功") : ResultBean.error(ErrorEnum.操作失败, "删除失败或记录不存在");
    }


    /**
     * 手动触发park接口定时任务
     */
    @PostMapping("/execute")
    public ResultBean executeParkTasks(@RequestHeader("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
        }

        ParkingBook config = parkingBookService.getById(token);
        if (config == null) {
            // 如果不存在，返回空对象或提示创建
            return ResultBean.error(ErrorEnum.操作失败,"没创建配置信息");
        }
        try {
            log.info("手动触发预约");
            parkBookTask.executeParkTasks(config.getEmpNo(),config.getPassHash());

        } catch (Exception e) {
            log.error("手动触发预约失败", e);
            return ResultBean.error(ErrorEnum.操作失败,"手动触发预约执行失败: " + e.getMessage());
        }
        return ResultBean.success("手动触发预约成功");

    }

    /**
     * 手动触发park接口检查任务
     */
    @PostMapping("/check")
    public ResultBean checkParkTasks(@RequestHeader("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
        }

        ParkingBook config = parkingBookService.getById(token);
        if (config == null) {
            // 如果不存在，返回空对象或提示创建
            return ResultBean.error(ErrorEnum.操作失败,"没创建配置信息");
        }
        try {
            log.info("手动触发检查");
            parkBookTask.checkParkingBook(config);

        } catch (Exception e) {
            log.error("手动触发检查失败", e);
            return ResultBean.error(ErrorEnum.操作失败,"手动触发检查执行失败: " + e.getMessage());
        }
        return ResultBean.success("手动触发检查成功");

    }

    /**
     * 手动触发park取消预约
     */
    @PostMapping("/cancel")
    public ResultBean cancelParkTasks(@RequestHeader("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
        }

        ParkingBook config = parkingBookService.getById(token);
        if (config == null) {
            // 如果不存在，返回空对象或提示创建
            return ResultBean.error(ErrorEnum.操作失败,"没创建配置信息");
        }
        try {
            log.info("手动触发取消预约");
            parkBookTask.executeCancelParkTasks(config.getEmpNo(),config.getPassHash());

        } catch (Exception e) {
            log.error("手动触发取消预约失败", e);
            return ResultBean.error(ErrorEnum.操作失败,"手动触发取消预约执行失败: " + e.getMessage());
        }
        return ResultBean.success("手动触发取消预约成功");

    }

    /**
     * 【测试用】给指定工号的用户发一封带车位截图的测试邮件。
     * GET /api/parking/book/testSendMail?empNo=2036377
     *
     * @param empNo 工号（可选，默认 2036377）
     */
    @GetMapping("/testSendMail")
    public ResultBean testSendMail(@RequestParam(required = false, defaultValue = "2036377") String empNo) {
        try {
            // 1. 查配置
            ParkingBook config = parkingBookService.getById(empNo);
            if (config == null) {
                return ResultBean.error("用户 " + empNo + " 未配置 ParkingBook");
            }

            // 2. 查最新一条预约记录（按 id 倒序取第一条）
            ParkingRecord latestRecord = parkingRecordService.getOne(
                new LambdaQueryWrapper<ParkingRecord>()
                    .eq(ParkingRecord::getEmpNo, empNo)
                    .orderByDesc(ParkingRecord::getId)
                    .last("LIMIT 1")
            );
            if (latestRecord == null) {
                return ResultBean.error("用户 " + empNo + " 没有任何预约记录");
            }

            // 3. 发送邮件（异步，含截图）
            bankEmailPlaywrightService.sendParkingNotification(config, latestRecord);

            log.info("测试邮件已触发：empNo={}, recordId={}", empNo, latestRecord.getId());
            return ResultBean.success("测试邮件已触发，subject=" + latestRecord.getResult()
                + " " + latestRecord.getAppointmentDate()
                + " 位置：" + latestRecord.getParkingPosition());

        } catch (Exception e) {
            log.error("测试邮件触发失败", e);
            return ResultBean.error("测试邮件触发失败：" + e.getMessage());
        }
    }

}
