package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全文检索服务实现
 * <p>
 * 优先使用 Elasticsearch（{@code ik_max_word} 中文分词 + 高亮）进行全文检索；
 * 当 ES 模板未注入或查询异常时，自动降级为 MySQL LIKE 模糊查询，
 * 检索 lost_item 与 found_item 两张表的 title / description / location 字段，
 * 并对命中文本进行手动高亮包装。
 * </p>
 */
@Slf4j
@Service
public class EsSearchServiceImpl implements SearchService {

    /** ES 索引名（丢失与拾取物品统一索引） */
    private static final String INDEX_NAME = "lost_found_item";

    /** 高亮前缀标签 */
    private static final String HL_PRE = "<em>";

    /** 高亮后缀标签 */
    private static final String HL_POST = "</em>";

    /**
     * 可选注入：当 ES 未启用或客户端不可创建时为 null，触发降级
     */
    @Autowired(required = false)
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Override
    public List<Map<String, Object>> search(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }

        // ES 不可用 -> 直接走 MySQL 降级
        if (elasticsearchTemplate == null) {
            log.debug("ElasticsearchRestTemplate 未注入，降级 MySQL 模糊检索: keyword={}", keyword);
            return searchFromMysql(keyword, page, size);
        }

        try {
            return searchFromEs(keyword, page, size);
        } catch (Exception e) {
            log.warn("Elasticsearch 检索异常，降级 MySQL 模糊检索: keyword={}, 错误={}", keyword, e.getMessage());
            return searchFromMysql(keyword, page, size);
        }
    }

    @Override
    public long count(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0;
        }
        String kw = keyword.trim();

        // ES 不可用 -> 直接走 MySQL 降级
        if (elasticsearchTemplate == null) {
            log.debug("ElasticsearchRestTemplate 未注入，降级 MySQL 统计: keyword={}", kw);
            return countFromMysql(kw);
        }

        try {
            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.multiMatchQuery(kw, "title", "description", "location")
                            .analyzer("ik_max_word"))
                    .build();
            SearchHits<Map> hits = elasticsearchTemplate.search(query, Map.class,
                    IndexCoordinates.of(INDEX_NAME));
            long total = hits.getTotalHits();
            log.info("ES 统计完成: keyword={}, total={}", kw, total);
            return total;
        } catch (Exception e) {
            log.warn("Elasticsearch 统计异常，降级 MySQL 统计: keyword={}, 错误={}", kw, e.getMessage());
            return countFromMysql(kw);
        }
    }

    /**
     * MySQL 降级统计：分别查询 lost_item 与 found_item 两表匹配总数后求和。
     * QueryWrapper 条件与 {@link #searchFromMysql} 中的 LIKE 条件保持一致。
     */
    private long countFromMysql(String keyword) {
        long total = 0;

        // 丢失物品
        try {
            QueryWrapper<LostItem> lostWrapper = new QueryWrapper<>();
            lostWrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword));
            total += lostItemMapper.selectCount(lostWrapper);
        } catch (Exception e) {
            log.warn("MySQL 统计 lost_item 失败: keyword={}, 错误={}", keyword, e.getMessage());
        }

        // 拾到物品
        try {
            QueryWrapper<FoundItem> foundWrapper = new QueryWrapper<>();
            foundWrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword));
            total += foundItemMapper.selectCount(foundWrapper);
        } catch (Exception e) {
            log.warn("MySQL 统计 found_item 失败: keyword={}, 错误={}", keyword, e.getMessage());
        }

        log.info("MySQL 降级统计完成: keyword={}, total={}", keyword, total);
        return total;
    }

    @Override
    public List<String> suggest(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String kw = prefix.trim();
        Set<String> suggestions = new LinkedHashSet<>();

        // 优先尝试 ES 的 prefix / suggest 能力，失败则走 MySQL
        if (elasticsearchTemplate != null) {
            try {
                NativeSearchQuery query = new NativeSearchQueryBuilder()
                        .withQuery(QueryBuilders.prefixQuery("title", kw))
                        .withPageable(PageRequest.of(0, 10))
                        .build();
                SearchHits<Map> hits = elasticsearchTemplate.search(query, Map.class,
                        IndexCoordinates.of(INDEX_NAME));
                for (SearchHit<Map> hit : hits.getSearchHits()) {
                    Object title = hit.getContent().get("title");
                    if (title != null) {
                        suggestions.add(title.toString());
                    }
                    if (suggestions.size() >= 10) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("ES suggest 异常，降级 MySQL: prefix={}, 错误={}", kw, e.getMessage());
            }
        }

        // MySQL 兜底：从两张表查询以 prefix 开头的标题
        if (suggestions.isEmpty()) {
            suggestions.addAll(queryTitlePrefix(lostItemMapper, kw, "lost"));
            suggestions.addAll(queryTitlePrefix(foundItemMapper, kw, "found"));
        }
        // 最多返回 10 条建议
        return new ArrayList<>(suggestions).subList(0, Math.min(10, suggestions.size()));
    }

    // ==================== Elasticsearch 检索 ====================

    /**
     * 使用 ES multi_match + ik_max_word 分词检索，并附带高亮
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map<String, Object>> searchFromEs(String keyword, int page, int size) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "description", "location")
                        .analyzer("ik_max_word"))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags(HL_PRE).postTags(HL_POST),
                        new HighlightBuilder.Field("description").preTags(HL_PRE).postTags(HL_POST),
                        new HighlightBuilder.Field("location").preTags(HL_PRE).postTags(HL_POST))
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<Map> hits = elasticsearchTemplate.search(query, Map.class,
                IndexCoordinates.of(INDEX_NAME));

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchHit<Map> hit : hits.getSearchHits()) {
            Map<String, Object> source = hit.getContent();
            Map<String, Object> item = new HashMap<>(source);
            item.put("dataSource", "elasticsearch");

            // 覆盖高亮字段
            Map<String, List<String>> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                List<String> titleHl = highlightFields.get("title");
                if (titleHl != null && !titleHl.isEmpty()) {
                    item.put("titleHl", titleHl.get(0));
                }
                List<String> descHl = highlightFields.get("description");
                if (descHl != null && !descHl.isEmpty()) {
                    item.put("descriptionHl", descHl.get(0));
                }
                List<String> locHl = highlightFields.get("location");
                if (locHl != null && !locHl.isEmpty()) {
                    item.put("locationHl", locHl.get(0));
                }
            }
            results.add(item);
        }
        log.info("ES 检索完成: keyword={}, 命中={}", keyword, results.size());
        return results;
    }

    // ==================== MySQL 降级检索 ====================

    /**
     * 使用 MySQL LIKE 检索两张表，手动高亮命中关键词
     */
    private List<Map<String, Object>> searchFromMysql(String keyword, int page, int size) {
        List<Map<String, Object>> results = new ArrayList<>();
        int offset = (page - 1) * size;

        // 丢失物品
        try {
            QueryWrapper<LostItem> lostWrapper = new QueryWrapper<>();
            lostWrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword))
                    .orderByDesc("create_time");
            Page<LostItem> lostPage = lostItemMapper.selectPage(new Page<>(page, size), lostWrapper);
            for (LostItem item : lostPage.getRecords()) {
                results.add(toMap(item, "lost", keyword));
            }
        } catch (Exception e) {
            log.warn("MySQL 检索 lost_item 失败: keyword={}, 错误={}", keyword, e.getMessage());
        }

        // 拾到物品
        try {
            QueryWrapper<FoundItem> foundWrapper = new QueryWrapper<>();
            foundWrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("location", keyword))
                    .orderByDesc("create_time");
            Page<FoundItem> foundPage = foundItemMapper.selectPage(new Page<>(page, size), foundWrapper);
            for (FoundItem item : foundPage.getRecords()) {
                results.add(toMap(item, "found", keyword));
            }
        } catch (Exception e) {
            log.warn("MySQL 检索 found_item 失败: keyword={}, 错误={}", keyword, e.getMessage());
        }

        // 两表合并后可能超过 size，截断到请求条数
        if (results.size() > size) {
            results = new ArrayList<>(results.subList(0, size));
        }
        log.info("MySQL 降级检索完成: keyword={}, 命中={}", keyword, results.size());
        return results;
    }

    /**
     * 将 LostItem 转换为前端友好的 Map，并生成高亮字段
     */
    private Map<String, Object> toMap(LostItem item, String itemType, String keyword) {
        Map<String, Object> map = new HashMap<>(16);
        map.put("id", item.getId());
        map.put("itemType", itemType);
        map.put("title", item.getTitle());
        map.put("description", item.getDescription());
        map.put("category", item.getCategory());
        map.put("location", item.getLocation());
        map.put("images", item.getImages());
        map.put("status", item.getStatus());
        map.put("eventTime", item.getEventTime());
        map.put("createTime", item.getCreateTime());
        map.put("dataSource", "mysql");
        // 手动高亮
        map.put("titleHl", highlight(item.getTitle(), keyword));
        map.put("descriptionHl", highlight(item.getDescription(), keyword));
        map.put("locationHl", highlight(item.getLocation(), keyword));
        return map;
    }

    /**
     * 将 FoundItem 转换为前端友好的 Map，并生成高亮字段
     */
    private Map<String, Object> toMap(FoundItem item, String itemType, String keyword) {
        Map<String, Object> map = new HashMap<>(16);
        map.put("id", item.getId());
        map.put("itemType", itemType);
        map.put("title", item.getTitle());
        map.put("description", item.getDescription());
        map.put("category", item.getCategory());
        map.put("location", item.getLocation());
        map.put("images", item.getImages());
        map.put("status", item.getStatus());
        map.put("eventTime", item.getEventTime());
        map.put("createTime", item.getCreateTime());
        map.put("dataSource", "mysql");
        map.put("titleHl", highlight(item.getTitle(), keyword));
        map.put("descriptionHl", highlight(item.getDescription(), keyword));
        map.put("locationHl", highlight(item.getLocation(), keyword));
        return map;
    }

    /**
     * 手动高亮：将文本中匹配关键词的部分用 <em> 包裹（大小写不敏感）
     */
    private String highlight(String text, String keyword) {
        if (text == null || text.isEmpty() || keyword == null || keyword.isEmpty()) {
            return text;
        }
        String lowerText = text.toLowerCase();
        String lowerKw = keyword.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int idx;
        while ((idx = lowerText.indexOf(lowerKw, start)) >= 0) {
            sb.append(text, start, idx);
            sb.append(HL_PRE);
            sb.append(text, idx, idx + keyword.length());
            sb.append(HL_POST);
            start = idx + keyword.length();
        }
        sb.append(text, start, text.length());
        return sb.toString();
    }

    /**
     * 通用方法：从 BaseMapper 查询标题以 prefix 开头的记录（用于 suggest 兜底）
     */
    private List<String> queryTitlePrefix(LostItemMapper mapper, String prefix, String itemType) {
        List<String> titles = new ArrayList<>();
        try {
            QueryWrapper<LostItem> wrapper = new QueryWrapper<>();
            wrapper.likeRight("title", prefix).last("LIMIT 5");
            List<LostItem> list = mapper.selectList(wrapper);
            for (LostItem item : list) {
                if (item.getTitle() != null) {
                    titles.add(item.getTitle());
                }
            }
        } catch (Exception e) {
            log.warn("MySQL suggest 查询 lost_item 失败: prefix={}, 错误={}", prefix, e.getMessage());
        }
        return titles;
    }

    /**
     * 重载：从 FoundItemMapper 查询标题前缀
     */
    private List<String> queryTitlePrefix(FoundItemMapper mapper, String prefix, String itemType) {
        List<String> titles = new ArrayList<>();
        try {
            QueryWrapper<FoundItem> wrapper = new QueryWrapper<>();
            wrapper.likeRight("title", prefix).last("LIMIT 5");
            List<FoundItem> list = mapper.selectList(wrapper);
            for (FoundItem item : list) {
                if (item.getTitle() != null) {
                    titles.add(item.getTitle());
                }
            }
        } catch (Exception e) {
            log.warn("MySQL suggest 查询 found_item 失败: prefix={}, 错误={}", prefix, e.getMessage());
        }
        return titles;
    }
}
