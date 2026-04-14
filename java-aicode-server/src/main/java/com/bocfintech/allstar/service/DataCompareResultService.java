package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.entity.CompareResultItem;

import java.util.List;

/**
 * 数据比对服务接口
 * 提供表结构、行数、完整数据等维度的比对能力
 */
public interface DataCompareResultService {

    /**
     * 执行表结构比对
     * 比较源库与目标库指定表的字段定义（列名、类型、是否为空、主键等）
     *
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableNames 表名列表
     * @return 比对结果列表
     */
    List<CompareResultItem> compareTableStructure(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames);

    /**
     * 执行数据量（行数）比对
     * 统计各表记录数并对比
     *
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableNames 表名列表
     * @return 比对结果列表
     */
    List<CompareResultItem> compareRowCount(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames);

    /**
     * 执行完整数据比对
     * 逐行比对表中所有数据（基于主键或首列排序）
     *
     * @param sourceConfig 源库配置
     * @param targetConfig 目标库配置
     * @param tableNames 表名列表
     * @return 比对结果列表
     */
    List<CompareResultItem> compareFullData(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames);
}
