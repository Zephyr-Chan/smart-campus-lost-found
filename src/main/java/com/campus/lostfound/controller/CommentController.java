package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController extends BaseController {

    @Autowired
    private CommentService commentService;

    /**
     * 评论列表（公开接口，已由 LoginInterceptor 排除）
     * GET /api/comment/list?itemId=1&type=lost&page=1&size=10
     */
    @GetMapping("/list")
    public Result list(@RequestParam Long itemId,
                       @RequestParam("type") String itemType,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        return commentService.listComments(itemId, itemType, page, size);
    }

    /**
     * 发表评论（需登录）
     * POST /api/comment，请求体：{itemId, itemType, content, parentId}
     */
    @PostMapping
    public Result<Boolean> add(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long itemId = parseLong(body.get("itemId"));
        String itemType = parseString(body.get("itemType"));
        String content = parseString(body.get("content"));
        if (itemId == null || itemId <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "物品ID非法");
        }
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "物品类型不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "评论内容不能为空");
        }
        Long parentId = parseLong(body.get("parentId"));
        return commentService.addComment(userId, itemId, itemType, content, parentId);
    }

    /**
     * 删除评论（需登录，仅作者可删）
     * DELETE /api/comment/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = requireUserId(request);
        return commentService.deleteComment(id, userId);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseString(Object value) {
        return value == null ? null : value.toString();
    }
}
