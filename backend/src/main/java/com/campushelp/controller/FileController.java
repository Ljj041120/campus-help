package com.campushelp.controller;

import com.campushelp.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /** 最大文件大小 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传单个图片
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        validateFile(file);

        String url = saveFile(file);
        return Result.success(Map.of("url", url));
    }

    /**
     * 上传多个图片（最多3张）
     */
    @PostMapping("/uploads")
    public Result<List<Map<String, String>>> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        if (files.length > 3) {
            return Result.error("最多上传3张图片");
        }

        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            String url = saveFile(file);
            results.add(Map.of("url", url));
        }
        return Result.success(results);
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new com.campushelp.common.BusinessException("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new com.campushelp.common.BusinessException("文件大小不能超过5MB");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new com.campushelp.common.BusinessException("仅支持 JPG/PNG/GIF/WebP/BMP 格式");
        }
    }

    /**
     * 保存文件到本地
     */
    private String saveFile(MultipartFile file) {
        try {
            // 按日期分目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dir = Paths.get(uploadPath, dateDir);
            Files.createDirectories(dir);

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());

            // 返回可访问的URL
            String url = "/uploads/" + dateDir + "/" + fileName;
            log.info("文件上传成功: {}", url);
            return url;

        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new com.campushelp.common.BusinessException("文件上传失败");
        }
    }
}
