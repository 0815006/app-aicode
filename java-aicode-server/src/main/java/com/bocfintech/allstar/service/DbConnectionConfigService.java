package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.entity.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 数据库连接配置服务接口
 * 提供数据库配置的增删改查、连接测试、表信息查询、迁移操作等能力
 */
public interface DbConnectionConfigService extends IService<DbConnectionConfig> {

    /**
     * 查询指定用户的所有有效数据库配置（状态为 ACTIVE）
     * @param createdBy 创建人工号（7位）
     * @return 配置列表，按创建时间倒序排列
     */
    List<DbConnectionConfig> listByUser(String createdBy);

    /**
     * 保存新的数据库配置
     * @param config 配置对象
     * @param operator 操作人工号
     * @return true 保存成功，false 失败
     */
    boolean saveConfig(DbConnectionConfig config, String operator);

    /**
     * 更新数据库配置（仅允许修改非敏感字段，密码可选更新）
     * @param config 配置对象（必须包含 id）
     * @param operator 操作人工号
     * @return true 更新成功，false 失败
     */
    boolean updateConfig(DbConnectionConfig config, String operator);

    /**
     * 测试数据库连接是否可用
     * @param config 数据库配置（含 host、port、username、password、databaseName）
     * @return true 连接成功，false 连接失败
     */
    boolean testConnection(DbConnectionConfig config);

    /**
     * 获取指定数据库的所有表信息
     * @param config 数据库配置
     * @return 表信息列表，包含：表名、中文名、记录数、存储空间(MB)
     */
    List<TableInfo> getTableInfoList(DbConnectionConfig config);

    /**
     * 迁移单个表的结构（不含数据）
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableName 表名
     */
    void migrateTableStructure(DbConnectionConfig sourceConfig,
                               DbConnectionConfig targetConfig,
                               String tableName);

    /**
     * 迁移单个表的结构和数据
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableName 表名
     */
    void migrateTableData(DbConnectionConfig sourceConfig,
                          DbConnectionConfig targetConfig,
                          String tableName);

    /**
     * 仅迁移数据（要求字段名一致）
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableName 表名
     */
    void migrateTableDataOnly(DbConnectionConfig sourceConfig,
                              DbConnectionConfig targetConfig,
                              String tableName);

    /**
     * 清空目标库中的指定表数据（TRUNCATE TABLE）
     * @param config 目标库配置
     * @param tableName 表名
     */
    void truncateTable(DbConnectionConfig config, String tableName);

    /**
     * 删除目标库中的指定表（DROP TABLE）
     * @param config 目标库配置
     * @param tableName 表名
     */
    void dropTable(DbConnectionConfig config, String tableName);
}
