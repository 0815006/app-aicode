package com.bocfintech.allstar.entity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceDetailDTO {
    private String deploymentLocation;
    private String partitionUsage;
    private String systemPlatform;
    private Integer cpuCores;
    private Integer memoryGb;
    private Integer dedicatedStorageGb;
    private Integer sanStorageGb;
    private Integer nasStorageGb;
    private Integer count;
}