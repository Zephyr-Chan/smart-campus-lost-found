package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 站内消息控制器
 */
@RestController
@RequestMapping("/api/message")
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    /**
     * 分页查询消息列表
     */
    @GetMapping("/list")
    public Result list(HttpServletRequest request,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) Integer isRead) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        Long userId = requireUserId(request);
        return messageService.listMessages(userId, page, size, isRead);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return messageService.getUnreadCount(userId);
    }

    /**
     * 标记单条消息为已读
     */
    @PutMapping("/read/{id}")
    public Result<Boolean> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = requireUserId(request);
        return messageService.markAsRead(id, userId);
    }

    /**
     * 标记全部消息为已读
     */
    @PutMapping("/read-all")
    public Result<Boolean> markAllAsRead(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return messageService.markAllAsRead(userId);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = requireUserId(request);
        return messageService.deleteMessage(id, userId);
    }
}
