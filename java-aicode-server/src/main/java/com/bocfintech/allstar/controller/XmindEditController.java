package com.bocfintech.allstar.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bocfintech.allstar.bean.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * XMind 编辑器接口（独立于旧的 /api/xmind，纯文件存储，不落库）。
 * 每个用户（工号）在 root-path 下拥有独立子目录，目录名即工号。
 */
@Api(tags = "XMind 编辑器接口")
@RestController
@Slf4j
@RequestMapping("/api/xmind-edit")
public class XmindEditController {

    /** 工号：7 位数字 */
    private static final Pattern EMP_NO_PATTERN = Pattern.compile("^\\d{7}$");

    /** 文件名非法字符 */
    private static final Pattern ILLEGAL_NAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    @Value("${xmind-edit.root-path}")
    private String rootPath;

    // ------------------------------------------------------------------
    // 接口
    // ------------------------------------------------------------------

    @ApiOperation("列出当前用户的 XMind 文件")
    @GetMapping("/list")
    public ResultBean<List<Map<String, Object>>> list(@RequestHeader(value = "token", required = false) String token) {
        String empNo = resolveEmpNo(token);
        if (empNo == null) {
            return ResultBean.error("未登录或工号无效");
        }
        File dir = userDir(empNo, false);
        List<Map<String, Object>> result = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".xmind"));
            if (files != null) {
                for (File f : files) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", f.getName());
                    item.put("size", f.length());
                    item.put("lastModified", f.lastModified());
                    result.add(item);
                }
            }
        }
        // 按修改时间倒序
        result.sort(Comparator.comparingLong(m -> -((Long) m.get("lastModified"))));
        return ResultBean.success(result);
    }

    @ApiOperation("新建空白 XMind 文件")
    @PostMapping("/create")
    public ResultBean<String> create(@RequestBody Map<String, String> body,
                                     @RequestHeader(value = "token", required = false) String token) {
        String empNo = resolveEmpNo(token);
        if (empNo == null) {
            return ResultBean.error("未登录或工号无效");
        }
        String rawName = body == null ? null : body.get("fileName");
        if (!StringUtils.hasText(rawName)) {
            return ResultBean.error("文件名不能为空");
        }
        String fileName = rawName.trim();
        if (!fileName.toLowerCase().endsWith(".xmind")) {
            fileName = fileName + ".xmind";
        }
        if (!isSafeFileName(fileName)) {
            return ResultBean.error("文件名不合法");
        }

        File dir = userDir(empNo, true);
        File target = new File(dir, fileName);
        if (target.exists()) {
            return ResultBean.error("同名文件已存在：" + fileName);
        }

        String title = fileName.substring(0, fileName.length() - ".xmind".length());
        try {
            byte[] bytes = buildBlankXmind(title);
            Files.write(target.toPath(), bytes);
            log.info("XMind 新建成功：empNo={}, fileName={}", empNo, fileName);
            return ResultBean.success(fileName);
        } catch (IOException e) {
            log.error("XMind 新建失败 empNo={}, fileName={}", empNo, fileName, e);
            return ResultBean.error("新建失败：" + e.getMessage());
        }
    }

    @ApiOperation("上传 XMind 文件到当前用户目录")
    @PostMapping("/upload")
    public ResultBean<String> upload(@RequestParam("file") MultipartFile file,
                                     @RequestHeader(value = "token", required = false) String token) {
        String empNo = resolveEmpNo(token);
        if (empNo == null) {
            return ResultBean.error("未登录或工号无效");
        }
        if (file == null || file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? new File(file.getOriginalFilename()).getName() : "未命名.xmind";
        if (!originalName.toLowerCase().endsWith(".xmind")) {
            return ResultBean.error("仅支持 .xmind 文件");
        }
        if (!isSafeFileName(originalName)) {
            return ResultBean.error("文件名不合法");
        }

        File dir = userDir(empNo, true);
        String fileName = uniqueFileName(dir, originalName);
        File target = new File(dir, fileName);
        try {
            file.transferTo(target);
            log.info("XMind 上传成功：empNo={}, fileName={}", empNo, fileName);
            return ResultBean.success(fileName);
        } catch (IOException e) {
            log.error("XMind 上传失败 empNo={}", empNo, e);
            return ResultBean.error("上传失败：" + e.getMessage());
        }
    }

    @ApiOperation("下载当前用户的 XMind 文件")
    @GetMapping("/download")
    public void download(@RequestParam("fileName") String fileName,
                         @RequestHeader(value = "token", required = false) String token,
                         HttpServletResponse response) {
        String empNo = resolveEmpNo(token);
        if (empNo == null || !isSafeFileName(fileName)) {
            writeError(response, "参数不合法");
            return;
        }
        File target = new File(userDir(empNo, false), fileName.trim());
        if (!target.exists() || !target.isFile()) {
            writeError(response, "文件不存在");
            return;
        }
        try {
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(target.getName(), "UTF-8"));
            response.setContentLengthLong(target.length());
            Files.copy(target.toPath(), response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("XMind 下载失败 empNo={}, fileName={}", empNo, fileName, e);
        }
    }

    @ApiOperation("保存（覆盖）当前用户的 XMind 文件")
    @PostMapping("/save")
    public ResultBean<String> save(@RequestParam("file") MultipartFile file,
                                   @RequestParam("fileName") String fileName,
                                   @RequestHeader(value = "token", required = false) String token) {
        String empNo = resolveEmpNo(token);
        if (empNo == null) {
            return ResultBean.error("未登录或工号无效");
        }
        if (file == null || file.isEmpty()) {
            return ResultBean.error("文件内容不能为空");
        }
        if (!isSafeFileName(fileName)) {
            return ResultBean.error("文件名不合法");
        }
        File target = new File(userDir(empNo, true), fileName.trim());
        try {
            file.transferTo(target);
            log.info("XMind 保存成功：empNo={}, fileName={}", empNo, fileName);
            return ResultBean.success(fileName);
        } catch (IOException e) {
            log.error("XMind 保存失败 empNo={}, fileName={}", empNo, fileName, e);
            return ResultBean.error("保存失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除当前用户的 XMind 文件")
    @PostMapping("/delete")
    public ResultBean<String> delete(@RequestParam("fileName") String fileName,
                                     @RequestHeader(value = "token", required = false) String token) {
        String empNo = resolveEmpNo(token);
        if (empNo == null) {
            return ResultBean.error("未登录或工号无效");
        }
        if (!isSafeFileName(fileName)) {
            return ResultBean.error("文件名不合法");
        }
        File target = new File(userDir(empNo, false), fileName.trim());
        if (!target.exists() || !target.isFile()) {
            return ResultBean.error("文件不存在");
        }
        if (target.delete()) {
            log.info("XMind 删除成功：empNo={}, fileName={}", empNo, fileName);
            return ResultBean.success(fileName);
        }
        return ResultBean.error("删除失败");
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    /**
     * 从 token 解析工号，仅接受 7 位数字，避免路径穿越。
     */
    private String resolveEmpNo(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String empNo = token.trim();
        return EMP_NO_PATTERN.matcher(empNo).matches() ? empNo : null;
    }

    /**
     * 用户专属子目录，目录名即工号。
     */
    private File userDir(String empNo, boolean create) {
        File dir = new File(rootPath, empNo);
        if (create && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 文件名安全校验：必须以 .xmind 结尾，且不含路径分隔符与非法字符。
     */
    private boolean isSafeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String name = fileName.trim();
        if (!name.toLowerCase().endsWith(".xmind")) {
            return false;
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            return false;
        }
        return !ILLEGAL_NAME_PATTERN.matcher(name).find();
    }

    /**
     * 同名文件自动追加序号。
     */
    private String uniqueFileName(File dir, String fileName) {
        File target = new File(dir, fileName);
        if (!target.exists()) {
            return fileName;
        }
        String base = fileName.substring(0, fileName.length() - ".xmind".length());
        int index = 1;
        while (true) {
            String candidate = base + "(" + index + ").xmind";
            if (!new File(dir, candidate).exists()) {
                return candidate;
            }
            index++;
        }
    }

    /**
     * 构造最小可用的 XMind 压缩包（content.json + metadata.json + manifest.json）。
     */
    private byte[] buildBlankXmind(String title) throws IOException {
        String topicId = UUID.randomUUID().toString().replace("-", "");
        String sheetId = UUID.randomUUID().toString().replace("-", "");

        JSONObject rootTopic = new JSONObject();
        rootTopic.put("id", topicId);
        rootTopic.put("title", title);
        JSONObject children = new JSONObject();
        children.put("attached", new JSONArray());
        rootTopic.put("children", children);

        JSONObject sheet = new JSONObject();
        sheet.put("id", sheetId);
        sheet.put("class", "sheet");
        sheet.put("title", "画布 1");
        sheet.put("rootTopic", rootTopic);

        JSONArray contentArray = new JSONArray();
        contentArray.add(sheet);

        JSONObject creator = new JSONObject();
        creator.put("name", "aicode");
        creator.put("version", "1.0");
        JSONObject metadata = new JSONObject();
        metadata.put("creator", creator);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            writeEntry(zos, "content.json", JSON.toJSONBytes(contentArray));
            writeEntry(zos, "metadata.json", JSON.toJSONBytes(metadata));
            writeEntry(zos, "manifest.json",
                    "{\"file-entries\":{\"content.json\":{},\"metadata.json\":{}}}"
                            .getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }

    private void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    /**
     * 下载失败时以 JSON 形式返回错误信息（前端按 blob 接收，需自行解析）。
     */
    private void writeError(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"" + message + "\"}");
            response.flushBuffer();
        } catch (IOException e) {
            log.error("写出错误响应失败", e);
        }
    }
}
