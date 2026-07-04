package com.campus.lostfound.controller;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.entity.Announcement;
import com.campus.lostfound.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 公告控制器
 */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 公告列表（公开）
     */
    @GetMapping("/list")
    public Result list() {
        return announcementService.getList();
    }

    /**
     * 公告列表（管理员，返回全部状态）
     */
    @GetMapping("/admin/list")
    public Result adminList() {
        return announcementService.getAdminList();
    }

    /**
     * 公告详情（公开接口）
     */
    @GetMapping("/get/{id}")
    public Result detail(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        return announcementService.getById(id);
    }

    /**
     * 创建公告（管理员）
     */
    @PostMapping
    public Result create(HttpServletRequest request, @RequestBody Announcement ann) {
        Long userId = getCurrentUserId(request);
        return announcementService.create(userId, ann);
    }

    /**
     * 更新公告（管理员）
     */
    @PutMapping
    public Result update(HttpServletRequest request, @RequestBody Announcement ann) {
        Long userId = getCurrentUserId(request);
        return announcementService.update(userId, ann);
    }

    /**
     * 删除公告（管理员）
     */
    @DeleteMapping("/{id}")
    public Result delete(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return announcementService.delete(userId, id);
    }

    /**
     * 从请求头获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}
