package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 贡献排行榜控制器（公开接口）
 */
@RestController
@RequestMapping("/api/rank")
public class RankController extends BaseController {

    @Autowired
    private RankService rankService;

    /**
     * 获取排行榜列表
     *
     * @param type  榜单类型：credit/recovery/contribution
     * @param limit 返回条数（默认20）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "credit") String type,
                       @RequestParam(defaultValue = "20") int limit) {
        // 限制最大查询条数，防止恶意拉取
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
        return rankService.getRankList(type, limit);
    }
}
