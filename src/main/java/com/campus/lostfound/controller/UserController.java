package com.campus.lostfound.controller;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.FileUploadUtil;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userService.logout(userId);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result info(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userService.getUserInfo(userId);
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result profile(HttpServletRequest request, @RequestBody RegisterDTO dto) {
        Long userId = getCurrentUserId(request);
        return userService.updateProfile(userId, dto);
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public Result avatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId(request);
        String avatarUrl = fileUploadUtil.upload(file);
        userService.updateAvatar(userId, avatarUrl);
        return Result.success(avatarUrl);
    }

    /**
     * 用户列表（管理员）
     */
    @GetMapping("/list")
    public Result userList(@RequestParam(required = false) String keyword) {
        return userService.getUserList(keyword);
    }

    /**
     * 更新用户状态（管理员）
     */
    @PutMapping("/status/{id}")
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "状态值非法");
        }
        return userService.updateUserStatus(id, status);
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
