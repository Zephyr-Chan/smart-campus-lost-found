package com.campus.lostfound.common.utils;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传工具类
 */
@Component
public class FileUploadUtil {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-path}")
    private String accessPath;

    /**
     * 上传文件
     *
     * @param file MultipartFile文件
     * @return 文件访问URL
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }
        // 校验文件类型（H3修复：处理无扩展名文件）
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件缺少扩展名，仅支持jpg/jpeg/png/gif/webp");
        }
        String suffix = originalFilename.substring(dotIndex).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "不支持的文件类型，仅支持jpg/jpeg/png/gif/webp");
        }
        // 按日期分目录
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String relativePath = datePath + "/" + fileName;
        // 转为绝对路径，确保 mkdirs 和 transferTo 能正确创建文件
        java.nio.file.Path baseDir = java.nio.file.Paths.get(uploadPath).toAbsolutePath();
        File dest = new File(baseDir.toString(), relativePath);
        // 确保目录存在（多次重试，防止并发或权限问题）
        File parentDir = dest.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "无法创建上传目录: " + parentDir.getAbsolutePath());
        }
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
        return accessPath + relativePath;
    }
}
