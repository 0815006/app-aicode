package com.bocfintech.allstar.entity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceSummaryDTO {
    private String deploymentLocation;
    private String systemPlatform;
    private Integer hostCount;
    private Integer totalCpu;
    private Integer totalMemoryGb;
    private Long totalStorageGb; // 使用 Long 防止溢出
}
