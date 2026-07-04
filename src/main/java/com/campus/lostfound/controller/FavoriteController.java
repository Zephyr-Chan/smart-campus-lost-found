package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController extends BaseController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 切换收藏状态（需登录）
     * POST /api/favorite/toggle，请求体：{itemId, itemType}
     */
    @PostMapping("/toggle")
    public Result<Boolean> toggle(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long itemId = parseLong(body.get("itemId"));
        String itemType = parseString(body.get("itemType"));
        if (itemId == null || itemId <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "物品ID非法");
        }
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "物品类型不能为空");
        }
        return favoriteService.toggleFavorite(userId, itemId, itemType);
    }

    /**
     * 检查是否已收藏（需登录）
     * GET /api/favorite/check?itemId=1&type=lost
     */
    @GetMapping("/check")
    public Result<Boolean> check(HttpServletRequest request,
                                 @RequestParam Long itemId,
                                 @RequestParam("type") String itemType) {
        Long userId = requireUserId(request);
        return favoriteService.checkFavorite(userId, itemId, itemType);
    }

    /**
     * 我的收藏列表（需登录）
     * GET /api/favorite/my?page=1&size=10
     */
    @GetMapping("/my")
    public Result myFavorites(HttpServletRequest request,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        Long userId = requireUserId(request);
        return favoriteService.myFavorites(userId, page, size);
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
