package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 部署方案环境资源清单表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("performance_resource_info")
public class PerformanceResourceInfo {
    // 主键ID
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 序号
    @TableField("serial_number")
    private Integer serialNumber;

    // 任务名称
    @TableField("task_name")
    private String taskName;

    // 任务编号
    @TableField("task_num")
    private String taskNum;

    // 服务名称
    @TableField("service_name")
    private String serviceName;

    // 英文简称
    @TableField("english_short_name")
    private String englishShortName;

    // 批次名称
    @TableField("batch_name")
    private String batchName;

    // 业务部门
    @TableField("business_dept")
    private String businessDept;

    // 项目类型
    @TableField("project_type")
    private String projectType;

    // 灾备等级
    @TableField("disaster_backup_level")
    private String disasterBackupLevel;

    // 可用性等级
    @TableField("availability_level")
    private String availabilityLevel;

    // 部署地点
    @TableField("deployment_location")
    private String deploymentLocation;

    // 网络部署
    @TableField("network_deployment")
    private String networkDeployment;

    // 系统平台
    @TableField("system_platform")
    private String systemPlatform;

    // PAAS平台类型
    @TableField("paas_platform_type")
    private String paasPlatformType;

    // 主题数量
    @TableField("theme_count")
    private Integer themeCount;

    // 队列数量
    @TableField("queue_count")
    private Integer queueCount;

    // 分片数量
    @TableField("shard_count")
    private Integer shardCount;

    // 每分片容量（G）
    @TableField("per_shard_capacity_gb")
    private Integer perShardCapacityGb;

    // 冗余方式
    @TableField("redundancy_method")
    private String redundancyMethod;

    // 操作系统
    @TableField("operating_system")
    private String operatingSystem;

    // 中间件
    @TableField("middleware")
    private String middleware;

    // 分区用途
    @TableField("partition_usage")
    private String partitionUsage;

    // 分区用途名称
    @TableField("partition_usage_name")
    private String partitionUsageName;

    // 主机名
    @TableField("hostname")
    private String hostname;

    // IP地址
    @TableField("ip_address")
    private String ipAddress;

    // 数据备份IP
    @TableField("backup_ip")
    private String backupIp;

    // CPU核心数
    @TableField("cpu_cores")
    private Integer cpuCores;

    // 内存（GB）
    @TableField("memory_gb")
    private Integer memoryGb;

    // 独占存储（GB）
    @TableField("dedicated_storage_gb")
    private Integer dedicatedStorageGb;

    // 共享存储编号
    @TableField("shared_storage_id")
    private String sharedStorageId;

    // SAN存储（GB）
    @TableField("san_storage_gb")
    private Integer sanStorageGb;

    // NAS存储（GB）
    @TableField("nas_storage_gb")
    private Integer nasStorageGb;

    // 是否有签名服务器
    @TableField("signature_server")
    private String signatureServer;

    // 是否有加密机
    @TableField("encryption_device")
    private String encryptionDevice;

    // 是否有负载均衡器
    @TableField("load_balancer")
    private String loadBalancer;

    // 是否有SSL加速器
    @TableField("ssl_accelerator")
    private String sslAccelerator;

    // 备注（外设型号）
    @TableField("remarks")
    private String remarks;

    // 分区角色
    @TableField("partition_role")
    private String partitionRole;

    // 修订时间
    @TableField("revision_time")
    private LocalDateTime revisionTime;

    // 中间件使用低于基线版本原因
    @TableField("middleware_reason_below_baseline")
    private String middlewareReasonBelowBaseline;

    // 操作系统使用低于基线版本原因
    @TableField("os_reason_below_baseline")
    private String osReasonBelowBaseline;

    // 资源池
    @TableField("resource_pool")
    private String resourcePool;

    // 原始文件名
    @TableField("original_file_name")
    private String originalFileName;

    // 上传文件名
    @TableField("file_name")
    private String fileName;

    // 产品标识
    @TableField("product_id")
    private String productId;

    // 文件来源
    @TableField("file_source")
    private String fileSource;

    // 创建时间
    @TableField("create_time")
    private Date createTime;

    // 创建人
    @TableField("create_operator")
    private String createOperator;

    // 更新时间
    @TableField("last_time")
    private Date lastTime;

    // 更新人
    @TableField("last_operator")
    private String lastOperator;
}

