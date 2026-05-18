package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.service.PerformanceDocService;
import com.bocfintech.allstar.service.PerformanceDocService.DocFileInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/api/performance/doc")
@Api(tags = "性能测试方案文档接口")
@Slf4j
public class PerformanceDocController {

    @Value("${performance.doc-path}")
    private String docPath;

    @Autowired
    private PerformanceDocService docService;

    @PostMapping("/generate")
    @ApiOperation("生成方案文档")
    public ResultBean<String> generate(@RequestParam Long taskId) {
        try {
            String filename = docService.generateDoc(taskId);
            return ResultBean.success(filename);
        } catch (Exception e) {
            log.error("方案文档生成失败, taskId={}", taskId, e);
            return ResultBean.error("文档生成失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取方案文档列表")
    public ResultBean<List<DocFileInfo>> list() {
        return ResultBean.success(docService.listDocs());
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除方案文档")
    public ResultBean<String> delete(@RequestParam String fileName) {
        if (docService.deleteDoc(fileName)) {
            return ResultBean.success("删除成功");
        }
        return ResultBean.error("文件不存在");
    }

    @GetMapping("/download")
    @ApiOperation("下载方案文档")
    public void download(@RequestParam String fileName, HttpServletResponse response) {
        File file = new File(docPath + fileName);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("方案文档下载失败", e);
        }
    }
}
