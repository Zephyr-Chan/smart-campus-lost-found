package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 推荐控制器
 * 基于 Item-CF 协同过滤的个性化推荐
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController extends BaseController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 个性化推荐（需登录）
     */
    @GetMapping("/user")
    public Result<List<Map<String, Object>>> recommendForUser(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return recommendService.recommendForUser(userId, limit);
    }

    /**
     * 游客推荐（公开）
     */
    @GetMapping("/guest")
    public Result<List<Map<String, Object>>> recommendForGuest(
            @RequestParam(defaultValue = "10") int limit) {
        return recommendService.recommendForGuest(limit);
    }

    /**
     * 相似物品推荐（公开）
     */
    @GetMapping("/similar/{itemId}")
    public Result<List<Map<String, Object>>> similarItems(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "lost") String type,
            @RequestParam(defaultValue = "5") int limit) {
        return recommendService.similarItems(itemId, type, limit);
    }

    /**
     * 记录浏览历史（需登录）
     */
    @PostMapping("/view")
    public Result<Void> recordViewHistory(
            @RequestParam Long itemId,
            @RequestParam String itemType,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        recommendService.recordViewHistory(userId, itemId, itemType);
        return Result.success();
    }
}
