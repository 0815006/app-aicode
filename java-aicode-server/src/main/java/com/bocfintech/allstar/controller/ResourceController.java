package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.PerformanceResourceInfo;
import com.bocfintech.allstar.entity.ResourceCheckResponse;
import com.bocfintech.allstar.service.PerformanceResourceInfoService;
import com.bocfintech.allstar.service.ResourceService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bocfintech.allstar.bean.ResultBean.success;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private PerformanceResourceInfoService productResourceInfoService;

    @ApiOperation(value = "查询环境资源清单明细")
    @GetMapping("/queryResourceDetail")
    public ResultBean query(@RequestParam(value = "pageNum", required = true, defaultValue = "1") int pageNum,
                            @RequestParam(value = "pageSize", required = true, defaultValue = "10") int pageSize,
                            @RequestParam(value = "productId", required = false) String productId,
                            @RequestParam(value = "fileName", required = false) String fileName){
        PageHelper.startPage(pageNum, pageSize);
        LambdaQueryWrapper<PerformanceResourceInfo> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotEmpty(productId))wrapper.eq(PerformanceResourceInfo::getProductId,productId);
        if(StringUtils.isNotEmpty(fileName))wrapper.eq(PerformanceResourceInfo::getFileName,fileName);
        wrapper.orderByDesc(PerformanceResourceInfo::getCreateTime);
        List<PerformanceResourceInfo> list = productResourceInfoService.list(wrapper);
        if(list == null || list.size() == 0) return success();
        PageInfo<PerformanceResourceInfo> pageInfo = new PageInfo<>(list);
        return success(pageInfo);
    }


    @ApiOperation(value = "上传环境资源清单")
    @PostMapping("/uploadResource")
    public ResultBean uploadExcelFile(@RequestParam(value = "productId") @NotBlank(message = "产品标识不能为空") String productId,
                                      @RequestParam(value = "fileSource", required = false, defaultValue = "部署方案") String fileSource,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestHeader(value = "token", required = false) String token) throws Exception {
        String empNo = getEmpNoFromToken(token);
        //上传文件到本地
        Map<String,Object> map = productResourceInfoService.uploadExcel(file);
        //导入到表
        productResourceInfoService.importExcel(file,
                Objects.toString(map.get("originalFileName"),"no originalFileName"),
                Objects.toString(map.get("fileName"),"no fileName"),productId,empNo, fileSource);
        return success();
    }

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

        // 工具方法
    private String getEmpNoFromToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return token.trim();
    }
}
