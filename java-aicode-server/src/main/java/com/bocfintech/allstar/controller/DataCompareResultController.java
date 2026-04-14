package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.CompareRequest;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.entity.CompareResultItem;
import com.bocfintech.allstar.service.DataCompareResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据比对功能控制器
 * 提供表结构、行数、完整数据等维度的比对接口
 */
@RestController
@Slf4j
@RequestMapping("/api/compare")
public class DataCompareResultController {

    @Autowired
    private DataCompareResultService dataCompareResultService;

    /**
     * 表结构比对
     * 比较源库与目标库的表字段定义是否一致
     *
     * @param request 请求参数，包含源库、目标库配置及表名列表
     * @param token 用户身份标识（员工号）
     * @return 比对结果列表
     */
    @PostMapping("/structure")
    public ResultBean<List<CompareResultItem>> compareStructure(
            @RequestBody CompareRequest request) {

        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            validateCompareRequest(request);
            List<CompareResultItem> result = dataCompareResultService.compareTableStructure(
                    request.getSourceConfig(),
                    request.getTargetConfig(),
                    request.getTables());
            return ResultBean.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResultBean.error(ErrorEnum.参数异常, e.getMessage());
        } catch (Exception e) {
            log.error("表结构比对执行失败", e);
            return ResultBean.error("表结构比对失败：" + e.getMessage());
        }
    }

    /**
     * 数据量（行数）比对
     * 统计并比较各表的记录数
     *
     * @param request 请求参数，包含源库、目标库配置及表名列表
     * @return 比对结果列表
     */
    @PostMapping("/rowCount")
    public ResultBean<List<CompareResultItem>> compareRowCount(
            @RequestBody CompareRequest request) {

        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            validateCompareRequest(request);
            List<CompareResultItem> result = dataCompareResultService.compareRowCount(
                    request.getSourceConfig(),
                    request.getTargetConfig(),
                    request.getTables());
            return ResultBean.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResultBean.error(ErrorEnum.参数异常, e.getMessage());
        } catch (Exception e) {
            log.error("数据量比对执行失败", e);
            return ResultBean.error("数据量比对失败：" + e.getMessage());
        }
    }

    /**
     * 完整数据比对
     * 逐行比对表中所有数据内容是否一致
     *
     * @param request 请求参数，包含源库、目标库配置及表名列表
     * @return 比对结果列表
     */
    @PostMapping("/fullData")
    public ResultBean<List<CompareResultItem>> compareFullData(
            @RequestBody CompareRequest request) {

        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            validateCompareRequest(request);
            List<CompareResultItem> result = dataCompareResultService.compareFullData(
                    request.getSourceConfig(),
                    request.getTargetConfig(),
                    request.getTables());
            return ResultBean.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ResultBean.error(ErrorEnum.参数异常, e.getMessage());
        } catch (Exception e) {
            log.error("完整数据比对执行失败", e);
            return ResultBean.error("完整数据比对失败：" + e.getMessage());
        }
    }

    // 工具方法
    private String getEmpNoFromToken() {
        return "2036377"; // TODO: 替换
    }

    /**
     * 校验比对请求参数
     */
    private void validateCompareRequest(CompareRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (request.getSourceConfig() == null) {
            throw new IllegalArgumentException("源库配置不能为空");
        }
        if (request.getTargetConfig() == null) {
            throw new IllegalArgumentException("目标库配置不能为空");
        }
        if (request.getTables() == null || request.getTables().isEmpty()) {
            throw new IllegalArgumentException("表名列表不能为空");
        }
        // 校验必要字段
        validateDbConfig(request.getSourceConfig(), "源库");
        validateDbConfig(request.getTargetConfig(), "目标库");
    }

    /**
     * 校验数据库配置
     */
    private void validateDbConfig(DbConnectionConfig config, String prefix) {
        if (!StringUtils.hasText(config.getHost())) {
            throw new IllegalArgumentException(prefix + "主机地址不能为空");
        }
        if (config.getPort() == null || config.getPort() <= 0) {
            throw new IllegalArgumentException(prefix + "端口必须大于0");
        }
        if (!StringUtils.hasText(config.getUsername())) {
            throw new IllegalArgumentException(prefix + "用户名不能为空");
        }
        if (!StringUtils.hasText(config.getPassword())) {
            throw new IllegalArgumentException(prefix + "密码不能为空");
        }
        if (!StringUtils.hasText(config.getDatabaseName())) {
            throw new IllegalArgumentException(prefix + "数据库名不能为空");
        }
    }

    // ------------------- 内部类：请求 DTO -------------------

}
