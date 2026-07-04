package com.campus.lostfound.controller;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 智能匹配控制器
 */
@RestController
@RequestMapping("/api/match")
public class MatchController {

    @Autowired
    private MatchService matchService;

    /**
     * 触发智能匹配，返回匹配结果列表
     */
    @GetMapping("/{lostItemId}")
    public Result findMatches(@PathVariable Long lostItemId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        return matchService.findMatches(lostItemId, currentUserId);
    }
}
