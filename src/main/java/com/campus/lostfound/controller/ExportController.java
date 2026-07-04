package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据导出控制器
 * 所有接口直接将 Excel 流写入 HttpServletResponse
 */
@RestController
@RequestMapping("/api/export")
public class ExportController extends BaseController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 导出物品列表（管理员）
     * GET /api/export/items?type=lost&status=
     */
    @GetMapping("/items")
    public void exportItems(HttpServletRequest request, HttpServletResponse response,
                            @RequestParam String type,
                            @RequestParam(required = false) String status) {
        checkAdmin(request);
        exportService.exportItems(response, type, status);
    }

    /**
     * 导出认领记录（管理员）
     * GET /api/export/claims?status=
     */
    @GetMapping("/claims")
    public void exportClaims(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(required = false) String status) {
        checkAdmin(request);
        exportService.exportClaims(response, status);
    }

    /**
     * 导出操作日志（管理员）
     * GET /api/export/logs
     */
    @GetMapping("/logs")
    public void exportLogs(HttpServletRequest request, HttpServletResponse response) {
        checkAdmin(request);
        exportService.exportLogs(response);
    }

    /**
     * 导出我的认领记录（需登录）
     * GET /api/export/my-claims
     */
    @GetMapping("/my-claims")
    public void exportMyClaims(HttpServletRequest request, HttpServletResponse response) {
        Long userId = requireUserId(request);
        exportService.exportMyClaims(response, userId);
    }

    /**
     * 校验当前登录用户是否为管理员
     */
    private void checkAdmin(HttpServletRequest request) {
        Long userId = requireUserId(request);
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != Constants.ROLE_ADMIN) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }
}
