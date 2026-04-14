package com.bocfintech.allstar.entity;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FileSummaryGroup {
    private String originalFileName;                    // 原始文件名
    private Integer uploadCount;            // ✅ 新增：上传次数
    private List<ResourceSummaryDTO> summary;           // 汇总数据（格式同 product level）
}