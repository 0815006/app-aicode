package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.ResourceCheckResponse;
import com.bocfintech.allstar.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    /**
     * 获取所有产品ID列表
     */
    @GetMapping("/productIds")
    public ResultBean<List<String>> getProductIds() {
        try {
            return ResultBean.success(resourceService.getAllProductIds());
        } catch (Exception e) {
            return ResultBean.error("获取产品列表失败：" + e.getMessage());
        }
    }

    /**
     * 核查资源：按产品ID返回总量汇总 + 明细聚合
     */
    @GetMapping("/check")
    public ResultBean<ResourceCheckResponse> checkResources(
            @RequestParam("productId") @NotBlank(message = "productId 不能为空") String productId,
            @RequestParam(value = "fileSource", required = false) String fileSource) {
        try {
            ResourceCheckResponse response = resourceService.getResourceCheckByProduct(productId, fileSource);
            return ResultBean.success(response);
        } catch (Exception e) {
            return ResultBean.error("查询资源信息失败：" + e.getMessage());
        }
    }

    /**
     * 删除指定产品下、指定原始文件名的资源数据
     */
    @DeleteMapping("/deleteByFile")
    public ResultBean deleteByOriginalFileName(
            @RequestParam("productId") @NotBlank String productId,
            @RequestParam("originalFileName") @NotBlank String originalFileName) {
        try {
            int count = resourceService.deleteByOriginalFileName(productId, originalFileName);
            return ResultBean.success("删除成功，共删除 " + count + " 条记录");
        } catch (Exception e) {
            return ResultBean.error("删除失败：" + e.getMessage());
        }
    }

}
