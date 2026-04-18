package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.service.DbConnectionConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/db/config")
public class DbConnectionConfigController {

    @Autowired
    private DbConnectionConfigService dbConnectionConfigService;

    /**
     * 查询所有数据库配置
     */
    @GetMapping("/list")
    public ResultBean<List<DbConnectionConfig>> listAll(@RequestHeader(value = "token", required = false) String token) {
        try {
            String empNo = getEmpNoFromToken(token);
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            List<DbConnectionConfig> list = dbConnectionConfigService.listByUser(empNo);
            return ResultBean.success(list);
        } catch (Exception e) {
            log.error("查询数据库配置失败", e);
            return ResultBean.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增数据库配置
     */
    @PostMapping("/create")
    public ResultBean<String> create(@RequestBody DbConnectionConfig config,
                                     @RequestHeader(value = "token", required = false) String token) {
        try {
            String empNo = getEmpNoFromToken(token);
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            if (config.getConfigName() == null || config.getConfigName().trim().isEmpty()) {
                return ResultBean.error("配置名称不能为空");
            }

            config.setCreatedBy(empNo);
            dbConnectionConfigService.save(config);
            return ResultBean.success("新增成功");
        } catch (Exception e) {
            log.error("新增数据库配置失败", e);
            return ResultBean.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 修改数据库配置（密码可选）
     */
    @PutMapping("/update")
    public ResultBean<String> update(@RequestBody DbConnectionConfig config,
                                     @RequestHeader(value = "token", required = false) String token) {
        try {
            String empNo = getEmpNoFromToken(token);
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            DbConnectionConfig existing = dbConnectionConfigService.getById(config.getId());
            if (existing == null) {
                return ResultBean.error("配置不存在");
            }
            if (!existing.getCreatedBy().equals(empNo)) {
                return ResultBean.error(ErrorEnum.权限异常, "无权修改他人配置");
            }

            // 保留原密码，除非新传了
            if (config.getPassword() == null || config.getPassword().isEmpty()) {
                config.setPassword(existing.getPassword());
            }

            config.setCreatedBy(empNo); // 防篡改
            config.setUpdatedTime(existing.getUpdatedTime()); // 由数据库自动更新
            dbConnectionConfigService.updateById(config);

            return ResultBean.success("修改成功");
        } catch (Exception e) {
            log.error("修改数据库配置失败", e);
            return ResultBean.error("修改失败：" + e.getMessage());
        }
    }

    /**
     *  删除本人的数据库配置
     */
    @DeleteMapping("/delete")
    public ResultBean<String> deleteConfig(@RequestParam Long id,
                                           @RequestHeader(value = "token", required = false) String token) {
        try {
            String empNo = getEmpNoFromToken(token);
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录");
            }

            DbConnectionConfig config = dbConnectionConfigService.getById(id);
            if (config == null) {
                return ResultBean.error("配置不存在");
            }
            if (!config.getCreatedBy().equals(empNo)) {
                return ResultBean.error(ErrorEnum.权限异常, "无权删除他人配置");
            }
            dbConnectionConfigService.removeById(id);
            return ResultBean.success("删除成功");
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return ResultBean.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 手动触发测试连接（源库）
     */
    @PostMapping("/testConnect")
    public ResultBean<String> testConnect(@RequestBody DbConnectionConfig config,
                                          @RequestHeader(value = "token", required = false) String token) {
        try {
            String empNo = getEmpNoFromToken(token);
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            boolean success = dbConnectionConfigService.testConnection(config);
            return success ? ResultBean.success("连接成功") : ResultBean.error("连接失败，请检查配置");
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return ResultBean.error("测试连接异常：" + e.getMessage());
        }
    }

    // 从 Header 提取员工号
    private String getEmpNoFromToken(String token) {
        return StringUtils.hasText(token) ? token.trim() : null;
    }
}
