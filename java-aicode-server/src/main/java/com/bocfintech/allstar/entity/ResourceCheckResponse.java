package com.bocfintech.allstar.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResourceCheckResponse {
    private List<ResourceSummaryDTO> summaryList;        // 产品级汇总
    private List<FileSummaryGroup> fileSummaryList;      // 新增：文件级汇总
    private List<ResourceDetailDTO> detailList;          // 明细聚合
}