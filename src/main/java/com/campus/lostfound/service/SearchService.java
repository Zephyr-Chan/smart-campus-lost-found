package com.campus.lostfound.service;

import java.util.List;
import java.util.Map;

/**
 * 全文检索服务接口
 * <p>
 * 优先使用 Elasticsearch 进行中文分词全文检索，当 ES 不可用时
 * 自动降级为 MySQL LIKE 模糊查询，保证检索能力始终可用。
 * </p>
 */
public interface SearchService {

    /**
     * 全文检索物品
     *
     * @param keyword 关键词
     * @param page    页码（从 1 开始）
     * @param size     每页条数
     * @return 命中结果列表，每条包含物品信息与高亮字段
     */
    List<Map<String, Object>> search(String keyword, int page, int size);

    /**
     * 统计搜索结果总数
     *
     * @param keyword 关键词
     * @return 命中结果总数（keyword 为空时返回 0）
     */
    long count(String keyword);

    /**
     * 关键词联想 / 搜索建议
     *
     * @param prefix 输入前缀
     * @return 建议关键词列表
     */
    List<String> suggest(String prefix);
}
