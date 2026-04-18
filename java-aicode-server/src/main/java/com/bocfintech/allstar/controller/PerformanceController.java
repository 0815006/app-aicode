package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.PerformanceResourceInfo;
import com.bocfintech.allstar.service.PerformanceResourceInfoService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bocfintech.allstar.bean.ResultBean.error;
import static com.bocfintech.allstar.bean.ResultBean.success;

@Slf4j
@RestController
@Api(tags = "性能测试管理")
@RequestMapping("/api/performance/")
public class PerformanceController {
    @Resource
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
                                      @RequestParam("file") MultipartFile file,
                                      @RequestHeader(value = "token", required = false) String token) throws Exception {
        String empNo = getEmpNoFromToken(token);
        //上传文件到本地
        Map<String,Object> map = productResourceInfoService.uploadExcel(file);
        //导入到表
        productResourceInfoService.importExcel(file,
                Objects.toString(map.get("originalFileName"),"no originalFileName"),
                Objects.toString(map.get("fileName"),"no fileName"),productId,empNo);
        return success();
    }

    @ApiOperation(value = "编辑信息")
    @PostMapping(value = "editResource")
    public ResultBean edit(@RequestBody @NotNull(message = "建议信息为空！") PerformanceResourceInfo productResourceInfo,
                           @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNoFromToken(token);
        Date date = new Date();

        productResourceInfo.setLastTime(date);
        productResourceInfo.setLastOperator(empNo);

        LambdaQueryWrapper<PerformanceResourceInfo> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotEmpty(productResourceInfo.getId().toString())){
            wrapper.eq(PerformanceResourceInfo::getId,productResourceInfo.getId().toString());
            if(productResourceInfoService.list(wrapper).size() == 0){
                return error(ErrorEnum.参数异常,"无此记录，无法编辑");
            }else{
                productResourceInfoService.update(productResourceInfo,wrapper);
            }
        }else{
            productResourceInfo.setCreateTime(date);
            productResourceInfo.setCreateOperator(empNo);
            productResourceInfoService.save(productResourceInfo);
        }
        return success();
    }

    @ApiOperation(value = "删除信息")
    @PostMapping(value = "deleteResource")
    public ResultBean delete(@RequestBody @NotNull(message = "建议信息为空！") PerformanceResourceInfo productResourceInfo) {
        // String empNo = getEmpNoFromToken(); // 如果需要校验权限可以加上

        LambdaQueryWrapper<PerformanceResourceInfo> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotEmpty(productResourceInfo.getId().toString())){
            wrapper.eq(PerformanceResourceInfo::getId,productResourceInfo.getId().toString());
            if(productResourceInfoService.list(wrapper).size() == 0){
                return error(ErrorEnum.参数异常,"无此记录，无法删除");
            }else{
                productResourceInfoService.remove(wrapper);
            }
        }else{
            return error(ErrorEnum.参数异常,"主键没传，删除失败");
        }
        return success();
    }

    @ApiOperation(value = "从新一代查询上海测试部的性能测试任务")
    @GetMapping("/queryPerformanceTask")
    public ResultBean queryPerformanceTask(@RequestParam(value = "pageNum", required = true, defaultValue = "1") int pageNum,
                            @RequestParam(value = "pageSize", required = true, defaultValue = "10") int pageSize) throws Exception {
        PageHelper.startPage(pageNum, pageSize);

//        List<Map<String, Object>> list = callThirdUrlService.queryPerformanceTestList(1,3);
//        if(list == null || list.size() == 0) return success();
//        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
//        return success(pageInfo);
        return success();
    }

    // 工具方法
    private String getEmpNoFromToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return token.trim();
    }
}
