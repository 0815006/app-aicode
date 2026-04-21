package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.mapper.PerformanceResourceInfoMapper;
import com.bocfintech.allstar.service.ResourceService;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private PerformanceResourceInfoMapper resourceMapper;

//    @Override
//    public ResourceCheckResponse getResourceCheckByProduct(String productId) {
//        List<PerformanceResourceInfo> list = resourceMapper.selectList(
//                Wrappers.<PerformanceResourceInfo>lambdaQuery()
//                        .eq(PerformanceResourceInfo::getProductId, productId)
//                        .isNotNull(PerformanceResourceInfo::getDeploymentLocation)
//        );
//
//        if (list.isEmpty()) {
//            return ResourceCheckResponse.builder()
//                    .summaryList(new ArrayList<>())
//                    .detailList(new ArrayList<>())
//                    .build();
//        }
//
//        // 1\. 汇总统计：按 deploymentLocation + systemPlatform
//        List<ResourceSummaryDTO> summaryList = list.stream()
//                .collect(Collectors.groupingBy(
//                        PerformanceResourceInfo::getDeploymentLocation,
//                        Collectors.groupingBy(
//                                PerformanceResourceInfo::getSystemPlatform
//                        )
//                ))
//                .entrySet().stream()
//                .flatMap(outer -> outer.getValue().entrySet().stream()
//                        .map(inner -> {
//                            List<PerformanceResourceInfo> group = inner.getValue();
//                            return ResourceSummaryDTO.builder()
//                                    .deploymentLocation(outer.getKey())
//                                    .systemPlatform(inner.getKey())
//                                    .hostCount(group.size())
//                                    .totalCpu(group.stream().mapToInt(r -> r.getCpuCores() == null ? 0 : r.getCpuCores()).sum())
//                                    .totalMemoryGb(group.stream().mapToInt(r -> r.getMemoryGb() == null ? 0 : r.getMemoryGb()).sum())
//                                    .totalStorageGb(group.stream().mapToLong(r -> {
//                                        Integer d = r.getDedicatedStorageGb() == null ? 0 : r.getDedicatedStorageGb();
//                                        Integer s = r.getSanStorageGb() == null ? 0 : r.getSanStorageGb();
//                                        Integer n = r.getNasStorageGb() == null ? 0 : r.getNasStorageGb();
//                                        return (long) d + s + n;
//                                    }).sum())
//                                    .build();
//                        }))
//                .sorted(Comparator.comparing(ResourceSummaryDTO::getDeploymentLocation)
//                        .thenComparing(ResourceSummaryDTO::getSystemPlatform))
//                .collect(Collectors.toList());
//
//        // 2\. 明细聚合：使用自定义 Key 类
//        List<ResourceDetailDTO> detailList = list.stream()
//                .filter(r -> r.getPartitionUsage() != null)
//                .collect(Collectors.groupingBy(DetailKey::new))
//                .entrySet().stream()
//                .map(entry -> {
//                    DetailKey key = entry.getKey();
//                    List<PerformanceResourceInfo> group = entry.getValue();
//                    return ResourceDetailDTO.builder()
//                            .deploymentLocation(key.deploymentLocation)
//                            .partitionUsage(key.partitionUsage)
//                            .systemPlatform(key.systemPlatform)
//                            .cpuCores(key.cpuCores)
//                            .memoryGb(key.memoryGb)
//                            .dedicatedStorageGb(key.dedicatedStorageGb)
//                            .sanStorageGb(key.sanStorageGb)
//                            .nasStorageGb(key.nasStorageGb)
//                            .count(group.size())
//                            .build();
//                })
//                .sorted(Comparator.comparing(ResourceDetailDTO::getDeploymentLocation)
//                        .thenComparing(ResourceDetailDTO::getPartitionUsage)
//                        .thenComparing(ResourceDetailDTO::getSystemPlatform))
//                .collect(Collectors.toList());
//
//        return ResourceCheckResponse.builder()
//                .summaryList(summaryList)
//                .detailList(detailList)
//                .build();
//    }
    @Override
    public ResourceCheckResponse getResourceCheckByProduct(String productId, String fileSource) {
        List<PerformanceResourceInfo> list = resourceMapper.selectList(
                Wrappers.<PerformanceResourceInfo>lambdaQuery()
                        .eq(PerformanceResourceInfo::getProductId, productId)
                        .eq(fileSource != null, PerformanceResourceInfo::getFileSource, fileSource)
                        .isNotNull(PerformanceResourceInfo::getDeploymentLocation)
        );

        if (list.isEmpty()) {
            return ResourceCheckResponse.builder()
                    .summaryList(new ArrayList<>())
                    .detailList(new ArrayList<>())
                    .fileSummaryList(new ArrayList<>())
                    .build();
        }

        // 1\. 产品级汇总：按 deploymentLocation + systemPlatform
        List<ResourceSummaryDTO> summaryList = list.stream()
                .collect(Collectors.groupingBy(
                        PerformanceResourceInfo::getDeploymentLocation,
                        Collectors.groupingBy(
                                PerformanceResourceInfo::getSystemPlatform
                        )
                ))
                .entrySet().stream()
                .flatMap(outer -> outer.getValue().entrySet().stream()
                        .map(inner -> {
                            List<PerformanceResourceInfo> group = inner.getValue();
                            return ResourceSummaryDTO.builder()
                                    .deploymentLocation(outer.getKey())
                                    .systemPlatform(inner.getKey())
                                    .hostCount(group.size())
                                    .totalCpu(group.stream().mapToInt(r -> r.getCpuCores() == null ? 0 : r.getCpuCores()).sum())
                                    .totalMemoryGb(group.stream().mapToInt(r -> r.getMemoryGb() == null ? 0 : r.getMemoryGb()).sum())
                                    .totalStorageGb(group.stream().mapToLong(r -> {
                                        Integer d = r.getDedicatedStorageGb() == null ? 0 : r.getDedicatedStorageGb();
                                        Integer s = r.getSanStorageGb() == null ? 0 : r.getSanStorageGb();
                                        Integer n = r.getNasStorageGb() == null ? 0 : r.getNasStorageGb();
                                        return (long) d + s + n;
                                    }).sum())
                                    .build();
                        }))
                .sorted(Comparator.comparing(ResourceSummaryDTO::getDeploymentLocation)
                        .thenComparing(ResourceSummaryDTO::getSystemPlatform))
                .collect(Collectors.toList());

        // 2\. 明细聚合：按 DetailKey
        List<ResourceDetailDTO> detailList = list.stream()
                .filter(r -> r.getPartitionUsage() != null)
                .collect(Collectors.groupingBy(DetailKey::new))
                .entrySet().stream()
                .map(entry -> {
                    DetailKey key = entry.getKey();
                    List<PerformanceResourceInfo> group = entry.getValue();
                    return ResourceDetailDTO.builder()
                            .deploymentLocation(key.deploymentLocation)
                            .partitionUsage(key.partitionUsage)
                            .systemPlatform(key.systemPlatform)
                            .cpuCores(key.cpuCores)
                            .memoryGb(key.memoryGb)
                            .dedicatedStorageGb(key.dedicatedStorageGb)
                            .sanStorageGb(key.sanStorageGb)
                            .nasStorageGb(key.nasStorageGb)
                            .count(group.size())
                            .build();
                })
                .sorted(Comparator.comparing(ResourceDetailDTO::getDeploymentLocation)
                        .thenComparing(ResourceDetailDTO::getPartitionUsage)
                        .thenComparing(ResourceDetailDTO::getSystemPlatform))
                .collect(Collectors.toList());

        // 3\. 新增：按 original_file_name 分组汇总
        List<FileSummaryGroup> fileSummaryList = list.stream()
                .filter(r -> r.getOriginalFileName() != null && !r.getOriginalFileName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        PerformanceResourceInfo::getOriginalFileName  // 按原始文件名分组
                ))
                .entrySet().stream()
                .map(entry -> {
                    String originalFileName = entry.getKey();
                    List<PerformanceResourceInfo> fileGroup = entry.getValue();

                    // ✅ 计算上传次数 = 不同 file_name 的数量
                    long uploadCount = fileGroup.stream()
                            .map(PerformanceResourceInfo::getFileName)
                            .distinct()
                            .count();

                    // 在该文件名的所有记录中，按 deploymentLocation + systemPlatform 汇总
                    List<ResourceSummaryDTO> summary = fileGroup.stream()
                            .collect(Collectors.groupingBy(
                                    PerformanceResourceInfo::getDeploymentLocation,
                                    Collectors.groupingBy(
                                            PerformanceResourceInfo::getSystemPlatform
                                    )
                            ))
                            .entrySet().stream()
                            .flatMap(outer -> outer.getValue().entrySet().stream()
                                    .map(inner -> {
                                        List<PerformanceResourceInfo> group = inner.getValue();
                                        return ResourceSummaryDTO.builder()
                                                .deploymentLocation(outer.getKey())
                                                .systemPlatform(inner.getKey())
                                                .hostCount(group.size())
                                                .totalCpu(group.stream().mapToInt(r -> r.getCpuCores() == null ? 0 : r.getCpuCores()).sum())
                                                .totalMemoryGb(group.stream().mapToInt(r -> r.getMemoryGb() == null ? 0 : r.getMemoryGb()).sum())
                                                .totalStorageGb(group.stream().mapToLong(r -> {
                                                    Integer d = r.getDedicatedStorageGb() == null ? 0 : r.getDedicatedStorageGb();
                                                    Integer s = r.getSanStorageGb() == null ? 0 : r.getSanStorageGb();
                                                    Integer n = r.getNasStorageGb() == null ? 0 : r.getNasStorageGb();
                                                    return (long) d + s + n;
                                                }).sum())
                                                .build();
                                    }))
                            .sorted(Comparator.comparing(ResourceSummaryDTO::getDeploymentLocation)
                                    .thenComparing(ResourceSummaryDTO::getSystemPlatform))
                            .collect(Collectors.toList());

                    return FileSummaryGroup.builder()
                            .originalFileName(originalFileName)
                            .uploadCount((int) uploadCount)  // ✅ 设置上传次数
                            .summary(summary)
                            .build();
                })
                .sorted(Comparator.comparing(FileSummaryGroup::getOriginalFileName))
                .collect(Collectors.toList());

        // 构建最终响应
        return ResourceCheckResponse.builder()
                .summaryList(summaryList)
                .detailList(detailList)
                .fileSummaryList(fileSummaryList)
                .build();
    }

    // ResourceServiceImpl.java
    @Override
    public int deleteByOriginalFileName(String productId, String originalFileName) {
        return resourceMapper.delete(
                Wrappers.<PerformanceResourceInfo>lambdaQuery()
                        .eq(PerformanceResourceInfo::getProductId, productId)
                        .eq(PerformanceResourceInfo::getOriginalFileName, originalFileName)
        );
    }

    @Override
    public List<String> getAllProductIds() {
        List<PerformanceResourceInfo> list = resourceMapper.selectList(
                Wrappers.<PerformanceResourceInfo>lambdaQuery()
                        .select(PerformanceResourceInfo::getProductId)
                        .isNotNull(PerformanceResourceInfo::getProductId)
                        .groupBy(PerformanceResourceInfo::getProductId)
        );
        return list.stream()
                .map(PerformanceResourceInfo::getProductId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toList());
    }


    // 自定义分组键类
    @EqualsAndHashCode
    @ToString
    private static class DetailKey {
        private final String deploymentLocation;
        private final String partitionUsage;
        private final String systemPlatform;
        private final Integer cpuCores;
        private final Integer memoryGb;
        private final Integer dedicatedStorageGb;
        private final Integer sanStorageGb;
        private final Integer nasStorageGb;

        public DetailKey(PerformanceResourceInfo r) {
            this.deploymentLocation = r.getDeploymentLocation();
            this.partitionUsage = r.getPartitionUsage();
            this.systemPlatform = r.getSystemPlatform();
            this.cpuCores = r.getCpuCores();
            this.memoryGb = r.getMemoryGb();
            this.dedicatedStorageGb = r.getDedicatedStorageGb();
            this.sanStorageGb = r.getSanStorageGb();
            this.nasStorageGb = r.getNasStorageGb();
        }
    }
}
