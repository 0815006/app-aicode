package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.service.DbConnectionConfigService;
import com.bocfintech.allstar.service.DataCompareTaskService;
import com.bocfintech.allstar.service.DataCompareResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/datamigration")
public class DataMigrationController {

    @Autowired
    private DbConnectionConfigService dbConnectionConfigService;

    @Autowired
    private DataCompareTaskService dataCompareTaskService;

    /**
     * 根据配置ID获取数据库配置详情
     */
    @GetMapping("/config/detail")
    public ResultBean<DbConnectionConfig> getConfigDetail(@RequestParam Long id) {
        if (id == null) {
            return ResultBean.error("配置ID不能为空");
        }
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            DbConnectionConfig config = dbConnectionConfigService.getById(id);
            if (config == null) {
                return ResultBean.error("配置不存在");
            }
            if (!config.getCreatedBy().equals(empNo)) {
                return ResultBean.error(ErrorEnum.权限异常, "无权访问该配置");
            }
            // 不返回密码明文
            DbConnectionConfig safeConfig = new DbConnectionConfig();
            safeConfig.setId(config.getId());
            safeConfig.setConfigName(config.getConfigName());
            safeConfig.setDbType(config.getDbType());
            safeConfig.setHost(config.getHost());
            safeConfig.setPort(config.getPort());
            safeConfig.setDatabaseName(config.getDatabaseName());
            safeConfig.setCharset(config.getCharset());
            return ResultBean.success(safeConfig);
        } catch (Exception e) {
            log.error("获取配置详情失败", e);
            return ResultBean.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 查询数据库所有表信息（表名、中文名、记录数、存储空间）
     */
    @PostMapping("/table/list")
    public ResultBean<List<TableInfo>> getTableList(@RequestBody DbConnectionConfig config) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            boolean connected = dbConnectionConfigService.testConnection(config);
            if (!connected) {
                return ResultBean.error("无法连接数据库，无法查询表信息");
            }
            List<TableInfo> tables = dbConnectionConfigService.getTableInfoList(config);
            return ResultBean.success(tables);
        } catch (Exception e) {
            log.error("查询表信息失败", e);
            return ResultBean.error("查询表信息失败：" + e.getMessage());
        }
    }


    /**
     * 迁移表结构
     */
    @PostMapping("/migrate/structure")
    public ResultBean<String> migrateStructure(@RequestBody MigrateRequest request) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            dbConnectionConfigService.migrateTableStructure(request.getSourceConfig(), request.getTargetConfig(), request.getTableName());
            return ResultBean.success("表结构迁移成功");
        } catch (Exception e) {
            log.error("迁移表结构失败", e);
            return ResultBean.error("迁移失败：" + e.getMessage());
        }
    }

    /**
     * 仅迁移数据（要求字段名一致）
     */
    @PostMapping("/migrate/dataOnly")
    public ResultBean<String> migrateDataOnly(@RequestBody MigrateRequest request) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            dbConnectionConfigService.migrateTableDataOnly(request.getSourceConfig(), request.getTargetConfig(), request.getTableName());
            return ResultBean.success("数据迁移成功");
        } catch (Exception e) {
            log.error("仅迁移数据失败", e);
            return ResultBean.error("迁移失败：" + e.getMessage());
        }
    }

    /**
     * 迁移表结构和数据
     */
    @PostMapping("/migrate/data")
    public ResultBean<String> migrateData(@RequestBody MigrateRequest request) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            dbConnectionConfigService.migrateTableData(request.getSourceConfig(), request.getTargetConfig(), request.getTableName());
            return ResultBean.success("表结构和数据迁移成功");
        } catch (Exception e) {
            log.error("迁移表数据失败", e);
            return ResultBean.error("迁移失败：" + e.getMessage());
        }
    }

    /**
     * 清空目标表数据
     */
    @PostMapping("/table/truncate")
    public ResultBean<String> truncateTable(@RequestBody TableOperationRequest request) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            dbConnectionConfigService.truncateTable(request.getTargetConfig(), request.getTableName());
            return ResultBean.success("清空表成功");
        } catch (Exception e) {
            log.error("清空表失败", e);
            return ResultBean.error("清空失败：" + e.getMessage());
        }
    }

    /**
     * 删除目标表
     */
    @PostMapping("/table/drop")
    public ResultBean<String> dropTable(@RequestBody TableOperationRequest request) {
        try {
            String empNo = getEmpNoFromToken();
            if (!StringUtils.hasText(empNo)) {
                return ResultBean.error(ErrorEnum.参数异常, "未登录或Token无效");
            }

            dbConnectionConfigService.dropTable(request.getTargetConfig(), request.getTableName());
            return ResultBean.success("删除表成功");
        } catch (Exception e) {
            log.error("删除表失败", e);
            return ResultBean.error("删除失败：" + e.getMessage());
        }
    }

    // 工具方法
    private String getEmpNoFromToken() {
        return "2036377"; // TODO: 替换
    }

    // 内部类：迁移请求
    public static class MigrateRequest {
        private DbConnectionConfig sourceConfig;
        private DbConnectionConfig targetConfig;
        private String tableName;

        // getter/setter
        public DbConnectionConfig getSourceConfig() { return sourceConfig; }
        public void setSourceConfig(DbConnectionConfig sourceConfig) { this.sourceConfig = sourceConfig; }
        public DbConnectionConfig getTargetConfig() { return targetConfig; }
        public void setTargetConfig(DbConnectionConfig targetConfig) { this.targetConfig = targetConfig; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }

    // 内部类：表操作请求
    public static class TableOperationRequest {
        private DbConnectionConfig targetConfig;
        private String tableName;

        // getter/setter
        public DbConnectionConfig getTargetConfig() { return targetConfig; }
        public void setTargetConfig(DbConnectionConfig targetConfig) { this.targetConfig = targetConfig; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }
}
