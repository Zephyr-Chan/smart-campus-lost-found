package com.campus.lostfound.controller;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.dto.FoundItemDTO;
import com.campus.lostfound.service.FoundItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 拾到物品控制器
 */
@RestController
@RequestMapping("/api/found")
public class FoundItemController {

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发布拾到物品
     */
    @PostMapping("/publish")
    public Result publish(HttpServletRequest request, @Valid @RequestBody FoundItemDTO dto) {
        Long userId = getCurrentUserId(request);
        return foundItemService.publish(userId, dto);
    }

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String keyword) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        return foundItemService.getList(page, size, category, keyword);
    }

    /**
     * 详情（公开接口，路径/get/{id}与拦截器排除模式匹配）
     */
    @GetMapping("/get/{id}")
    public Result detail(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        return foundItemService.getById(id);
    }

    /**
     * 编辑
     */
    @PutMapping("/{id}")
    public Result update(HttpServletRequest request, @PathVariable Long id,
                         @Valid @RequestBody FoundItemDTO dto) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return foundItemService.update(userId, id, dto);
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result delete(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return foundItemService.delete(userId, id);
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
