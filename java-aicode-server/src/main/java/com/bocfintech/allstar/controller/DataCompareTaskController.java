package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.DataCompareTask;
import com.bocfintech.allstar.service.DataCompareTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/compare/task")
public class DataCompareTaskController {

    @Autowired
    private DataCompareTaskService dataCompareTaskService;

    /**
     * 查询所有比对任务（仅自己的）
     */
    @GetMapping("/list")
    public ResultBean<List<DataCompareTask>> listAll() {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            List<DataCompareTask> list = dataCompareTaskService.listByUser(empNo);
            return ResultBean.success(list);
        } catch (Exception e) {
            log.error("查询比对任务失败", e);
            return ResultBean.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 保存新的比对任务
     */
    @PostMapping("/save")
    public ResultBean<String> save(@RequestBody DataCompareTask task) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            if (task.getTaskName() == null || task.getTaskName().trim().isEmpty()) {
                return ResultBean.error("任务名称不能为空");
            }

            task.setCreatedBy(empNo);
            dataCompareTaskService.save(task);
            return ResultBean.success("保存成功");
        } catch (Exception e) {
            log.error("保存比对任务失败", e);
            return ResultBean.error("保存失败：" + e.getMessage());
        }
    }

    // 工具方法
    private String getEmpNoFromToken() {
        return "2036377"; // TODO: 替换
    }
}
