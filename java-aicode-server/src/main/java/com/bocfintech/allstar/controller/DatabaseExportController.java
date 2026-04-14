package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ExportResult;
import com.bocfintech.allstar.config.ExportConfig;
import com.bocfintech.allstar.config.MysqlConfig;
import com.bocfintech.allstar.service.DatabaseExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/database")
public class DatabaseExportController {
    
    @Autowired
    private DatabaseExportService databaseExportService;

    @Autowired
    private ExportConfig exportConfig;

    @Autowired
    private MysqlConfig mysqlConfig;

    /**
     * 导出数据库表到SQL文件（使用配置文件中的表列表和默认路径）
     *
     * @return 导出结果
     */
    @PostMapping("/export/default")
    public ResponseEntity<ExportResult> exportDatabaseDefault() {
        try {
            // 使用配置文件中的默认输出路径
            String outputPath = exportConfig.getOutputPath();
            ExportResult result = databaseExportService.exportDatabase(outputPath);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            ExportResult errorResult = new ExportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 导出数据库表到SQL文件（使用配置文件中的表列表）
     * @param outputPath 输出路径
     * @return 导出结果
     */
    @PostMapping("/export")
    public ResponseEntity<ExportResult> exportDatabase(@RequestParam String outputPath) {
        try {
            ExportResult result = databaseExportService.exportDatabase(outputPath);
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            ExportResult errorResult = new ExportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * 导出数据库表到SQL文件（自定义表列表）
     * @param outputPath 输出路径
     * @param tables 表名列表（逗号分隔）
     * @return 导出结果
     */
    @PostMapping("/export-custom")
    public ResponseEntity<ExportResult> exportDatabaseCustom(
            @RequestParam String outputPath,
            @RequestParam String tables) {
        try {
            List<String> tableList = Arrays.asList(tables.split(","))
                                          .stream()
                                          .map(String::trim)
                                          .filter(s -> !s.isEmpty())
                                          .collect(Collectors.toList());
            ExportResult result = databaseExportService.exportDatabaseWithCustomTables(outputPath, tableList);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            ExportResult errorResult = new ExportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }


    /**
     * 获取当前配置的导出表列表
     * @return 配置信息
     */
    @GetMapping("/export-config")
    public ResponseEntity<Map<String, Object>> getExportConfig() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("tables", exportConfig.getTables());
        configInfo.put("outputPath", exportConfig.getOutputPath());
        configInfo.put("tempPath", exportConfig.getTempPath());
        return ResponseEntity.ok(configInfo);
    }
    
    /**
     * 测试数据库连接
     * @return 连接结果
     */
    @GetMapping("/test-mysql-connection")
    public ResponseEntity<Map<String, Object>> testMysqlConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简单的数据库连接测试
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + mysqlConfig.getHost() + ":" + mysqlConfig.getPort() + "/" + mysqlConfig.getDatabase();
            Connection connection = DriverManager.getConnection(url, mysqlConfig.getUserName(), mysqlConfig.getPassword());
            connection.close();
            result.put("success", true);
            result.put("message", "数据库连接成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    /**
     * 获取所有可用表
     * @return 表名列表
     */
    @GetMapping("/available-tables")
    public ResponseEntity<List<String>> getAllAvailableTables() {
        try {
            List<String> tables = databaseExportService.getAllAvailableTables();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
}
