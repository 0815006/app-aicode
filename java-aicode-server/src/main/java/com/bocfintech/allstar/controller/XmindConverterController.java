package com.bocfintech.allstar.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bocfintech.allstar.bean.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Api(tags = "XMind 转换接口")
@RestController
@Slf4j
@RequestMapping("/api/xmind")
public class XmindConverterController {

    @ApiOperation("上传并转换 XMind 文件 (原生 Zip 解析)")
    @PostMapping("/convert")
    public ResultBean<Map<String, Object>> convertXmind(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            String contentJson = null;
            while ((entry = zis.getNextEntry()) != null) {
                if ("content.json".equals(entry.getName())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    contentJson = sb.toString();
                    break;
                }
            }

            if (contentJson == null) {
                return ResultBean.error("不支持旧版 Xmind (XML) 或文件格式错误，未找到 content.json");
            }

            // 解析 content.json
            JSONArray rawData = JSON.parseArray(contentJson);
            if (rawData == null || rawData.isEmpty()) {
                return ResultBean.error("解析失败：内容为空");
            }

            JSONObject rootTopic = rawData.getJSONObject(0).getJSONObject("rootTopic");
            Map<String, Object> result = transform(rootTopic);

            return ResultBean.success(result);
        } catch (Exception e) {
            log.error("XMind 转换失败", e);
            return ResultBean.error("转换失败: " + e.getMessage());
        }
    }

    /**
     * 递归转换：将 Xmind 结构转为 mind-map 组件需要的 data + children 结构
     */
    private Map<String, Object> transform(JSONObject topic) {
        Map<String, Object> node = new HashMap<>();
        
        // 构造 data 部分
        Map<String, Object> data = new HashMap<>();
        data.put("text", topic.getString("title"));
        data.put("expand", true);
        
        // 获取备注 (Notes)
        String note = "";
        JSONObject notes = topic.getJSONObject("notes");
        if (notes != null) {
            JSONObject plain = notes.getJSONObject("plain");
            if (plain != null) {
                note = plain.getString("content");
            }
        }
        data.put("note", note != null ? note : "");
        
        node.put("data", data);

        // 递归处理子节点
        List<Map<String, Object>> children = new ArrayList<>();
        JSONObject childrenObj = topic.getJSONObject("children");
        if (childrenObj != null) {
            JSONArray attached = childrenObj.getJSONArray("attached");
            if (attached != null) {
                for (int i = 0; i < attached.size(); i++) {
                    children.add(transform(attached.getJSONObject(i)));
                }
            }
        }
        node.put("children", children);

        return node;
    }
}
