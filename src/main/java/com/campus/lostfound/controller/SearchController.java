package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全文检索控制器
 * <p>
 * 公开接口（无需登录），优先走 Elasticsearch，ES 不可用时由
 * {@link com.campus.lostfound.service.impl.EsSearchServiceImpl} 自动降级为 MySQL LIKE。
 * </p>
 */
@Tag(name = "全文检索", description = "物品全文检索与关键词联想")
@RestController
@RequestMapping("/api/search")
public class SearchController extends BaseController {

    @Autowired
    private SearchService searchService;

    /**
     * 全文检索物品
     */
    @Operation(summary = "全文检索物品", description = "支持中文分词检索标题/描述/地点，ES 不可用时降级 MySQL")
    @GetMapping
    public Result search(@Parameter(description = "关键词") @RequestParam(required = false) String keyword,
                         @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
                         @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        List<Map<String, Object>> list = searchService.search(keyword, page, size);
        Map<String, Object> data = new HashMap<>(4);
        data.put("records", list);
        data.put("total", searchService.count(keyword));
        data.put("page", page);
        data.put("size", size);
        return Result.success(data);
    }

    /**
     * 关键词联想
     */
    @Operation(summary = "搜索关键词联想", description = "根据输入前缀返回标题建议列表")
    @GetMapping("/suggest")
    public Result suggest(@Parameter(description = "输入前缀") @RequestParam(required = false) String prefix) {
        List<String> suggestions = searchService.suggest(prefix);
        return Result.success(suggestions);
    }
}
