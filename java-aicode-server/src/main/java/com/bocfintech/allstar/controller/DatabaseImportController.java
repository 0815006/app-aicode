package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ImportResult;
import com.bocfintech.allstar.config.ImportConfig;
import com.bocfintech.allstar.config.MysqlConfig;
import com.bocfintech.allstar.service.DatabaseImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/database")
public class DatabaseImportController {

    @Autowired
    private DatabaseImportService databaseImportService;

    @Autowired
    private ImportConfig importConfig;

    @Autowired
    private MysqlConfig mysqlConfig;

    /**
     * 导入SQL文件到TDSQL数据库（使用默认配置）
     * @param sqlFilePath SQL文件路径
     * @return 导入结果
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResult> importDatabaseDefault(@RequestParam String sqlFilePath) {
        try {
            ImportResult result = databaseImportService.importDatabase(sqlFilePath);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            ImportResult errorResult = new ImportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 导入指定目录下所有SQL文件
     * @return 导入结果
     */
    @PostMapping("/import-all")
    public ResponseEntity<ImportResult> importAllSqlFiles() {
        try {
            ImportResult result = databaseImportService.importAllSqlFiles();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ImportResult errorResult = new ImportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 导入指定表的SQL文件
     * @param tableNames 逗号分隔的表名列表
     * @return 导入结果
     */
    @PostMapping("/import-tables")
    public ResponseEntity<ImportResult> importTables(@RequestParam String tableNames) {
        try {
            List<String> tables = Arrays.asList(tableNames.split(","));
            ImportResult result = databaseImportService.importTables(tables);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ImportResult errorResult = new ImportResult(false, "服务器内部错误: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 获取当前导入配置信息
     * @return 配置信息
     */
    @GetMapping("/import-config")
    public ResponseEntity<Map<String, Object>> getImportConfig() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("tables", importConfig.getTables());
        configInfo.put("inputPath", importConfig.getInputPath());
        configInfo.put("tempPath", importConfig.getTempPath());
        return ResponseEntity.ok(configInfo);
    }

    /**
     * 测试数据库连接
     * @return 连接结果
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
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

}
