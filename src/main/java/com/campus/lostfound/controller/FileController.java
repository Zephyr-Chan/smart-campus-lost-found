package com.campus.lostfound.controller;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.utils.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileUploadUtil fileUploadUtil;

    /**
     * 通用文件上传
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        String url = fileUploadUtil.upload(file);
        return Result.success(url);
    }

    /**
     * 头像上传（同upload，可复用）
     */
    @PostMapping("/avatar")
    public Result avatar(@RequestParam("file") MultipartFile file) {
        String url = fileUploadUtil.upload(file);
        return Result.success(url);
    }
}
