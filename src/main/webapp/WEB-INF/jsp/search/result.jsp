<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<%-- 使用EL表达式从URL参数获取搜索关键词 --%>
<c:set var="keyword" value="${param.keyword}" />
<c:set var="pageTitle" value="搜索结果" />
<c:set var="pageSize" value="10" />
<div class="main-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
        <div class="layui-inline">
            <input type="text" id="searchKeyword" placeholder="输入关键词搜索物品..."
                   class="layui-input" style="width:360px;" autocomplete="off"
                   value="${fn:escapeXml(keyword)}">
        </div>
        <button class="layui-btn layui-btn-normal" id="btnSearch">
            <i class="layui-icon layui-icon-search"></i> 搜索
        </button>
        <c:choose>
            <c:when test="${not empty keyword}">
                <span class="search-info">
                    搜索关键词：<strong>${fn:escapeXml(keyword)}</strong>
                </span>
            </c:when>
            <c:otherwise>
                <span class="search-info">请输入关键词进行搜索</span>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- 搜索结果区域 -->
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-search"></i>搜索结果</span>
            <c:if test="${not empty keyword}">
                <span id="resultCount" style="font-size:13px;color:var(--text-tertiary);">加载中...</span>
            </c:if>
        </div>

        <!-- 结果列表 -->
        <div class="item-grid" id="searchResultList">
            <c:choose>
                <c:when test="${empty keyword}">
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-search"></i>
                        <p>请输入关键词开始搜索</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-loading"></i>
                        <p>正在搜索"${fn:escapeXml(keyword)}"...</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- 分页 -->
    <div id="searchPagination"></div>
</div>

<%-- 搜索结果高亮样式 --%>
<style>
    .search-info {
        font-size: 13px;
        color: var(--text-secondary);
        margin-left: 8px;
    }
    .search-info strong {
        color: var(--accent);
    }
    /* 高亮关键词样式 */
    .search-result-card em {
        font-style: normal;
        color: var(--danger);
        background: var(--warning-light);
        padding: 0 2px;
        border-radius: 2px;
        font-weight: 600;
    }
    .search-result-card .item-desc em {
        font-style: normal;
        color: var(--warning);
        font-weight: 600;
    }
    /* 类型徽章 */
    .type-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        font-weight: 600;
        letter-spacing: 0.3px;
    }
    .type-badge-lost {
        background: var(--danger-light);
        color: var(--danger);
    }
    .type-badge-found {
        background: var(--success-light);
        color: var(--success);
    }
    .category-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        font-weight: 500;
        background: var(--accent-light);
        color: var(--accent);
    }
    .item-desc {
        font-size: 12px;
        color: var(--text-tertiary);
        margin-bottom: 8px;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }
</style>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'laypage'], function () {
        var layer = layui.layer;
        var laypage = layui.laypage;

        var currentPage = 1;
        var pageSize = ${pageSize};

        // 加载搜索结果
        function loadSearchResult(page) {
            currentPage = page || 1;
            var keyword = $('#searchKeyword').val().trim();

            if (!keyword) {
                $('#searchResultList').html(
                    '<div class="empty-state">' +
                    '<i class="layui-icon layui-icon-search"></i>' +
                    '<p>请输入关键词开始搜索</p></div>'
                );
                $('#searchPagination').empty();
                $('#resultCount').text('');
                return;
            }

            // 更新URL（不刷新页面）
            var newUrl = window.location.pathname + '?keyword=' + encodeURIComponent(keyword);
            window.history.replaceState(null, '', newUrl);

            // 显示加载中
            $('#searchResultList').html(
                '<div class="empty-state">' +
                '<i class="layui-icon layui-icon-loading"></i>' +
                '<p>正在搜索"' + escapeHtml(keyword) + '"...</p></div>'
            );

            var params = {
                keyword: keyword,
                page: currentPage,
                size: pageSize
            };

            ajaxRequest('/api/search', 'GET', params, function (data) {
                renderResults(data);
                renderPagination(data.total);
            }, function (err) {
                $('#searchResultList').html(
                    '<div class="empty-state">' +
                    '<i class="layui-icon layui-icon-face-surprised"></i>' +
                    '<p>搜索失败：' + escapeHtml(err.msg || '服务异常') + '</p>' +
                    '<p style="font-size:12px;margin-top:8px;">已尝试降级到数据库查询，请稍后重试</p></div>'
                );
                $('#searchPagination').empty();
                $('#resultCount').text('');
            });
        }

        // 渲染搜索结果
        function renderResults(data) {
            var records = data.records || [];
            var total = data.total || 0;

            // 更新结果计数
            $('#resultCount').text('共找到 ' + total + ' 条结果');

            if (records.length === 0) {
                $('#searchResultList').html(
                    '<div class="empty-state">' +
                    '<i class="layui-icon layui-icon-face-surprised"></i>' +
                    '<p>未找到相关物品</p>' +
                    '<p style="font-size:12px;margin-top:8px;color:var(--text-tertiary);">' +
                    '试试更换关键词或减少搜索条件</p></div>'
                );
                return;
            }

            var html = '';
            records.forEach(function (item) {
                var img = getFirstImage(item.images);
                var imgHtml = img
                    ? '<img src="' + escapeHtml(getImageUrl(img)) + '" alt="' + escapeHtml(item.title || '') + '">'
                    : '<i class="layui-icon layui-icon-picture no-image"></i>';

                // 类型徽章
                var typeText = item.itemType === 'lost' ? '丢失' : '拾到';
                var typeClass = item.itemType === 'lost' ? 'type-badge-lost' : 'type-badge-found';

                // 分类徽章
                var categoryText = getCategoryText(item.category);

                // 详情链接
                var detailUrl = '/item/detail?type=' + (item.itemType || 'lost') + '&id=' + item.id;

                html += '<div class="item-card search-result-card" onclick="window.location.href=\'' + detailUrl + '\'">';
                html += '    <div class="item-image">' + imgHtml + '</div>';
                html += '    <div class="item-body">';
                // 标题（含高亮，渲染HTML）
                html += '      <div class="item-title">' + (item.title || '无标题') + '</div>';
                // 描述（含高亮，渲染HTML）
                var desc = item.description || '';
                if (desc) {
                    html += '      <div class="item-desc">' + escapeHtml(desc).replace(/&lt;em&gt;/g, '<em>').replace(/&lt;\/em&gt;/g, '</em>') + '</div>';
                }
                // 徽章行
                html += '      <div style="margin-bottom:6px;">';
                html += '        <span class="type-badge ' + typeClass + '">' + typeText + '</span> ';
                html += '        <span class="category-badge">' + escapeHtml(categoryText) + '</span>';
                html += '      </div>';
                // 地点
                if (item.location) {
                    html += '      <div class="item-meta"><i class="layui-icon layui-icon-location"></i>' + escapeHtml(truncateStr(item.location, 20)) + '</div>';
                }
                // 时间
                if (item.eventTime) {
                    html += '      <div class="item-meta"><i class="layui-icon layui-icon-time"></i>' + formatDate(item.eventTime) + '</div>';
                }
                html += '    </div>';
                html += '</div>';
            });
            $('#searchResultList').html(html);
        }

        // 渲染分页
        function renderPagination(total) {
            if (total <= 0) {
                $('#searchPagination').empty();
                return;
            }
            laypage.render({
                elem: 'searchPagination',
                count: total,
                limit: pageSize,
                curr: currentPage,
                layout: ['count', 'prev', 'page', 'next', 'skip'],
                jump: function (obj, first) {
                    if (!first) {
                        loadSearchResult(obj.curr);
                    }
                }
            });
        }

        // 搜索按钮点击
        $('#btnSearch').on('click', function () {
            loadSearchResult(1);
        });

        // 回车搜索
        $('#searchKeyword').on('keydown', function (e) {
            if (e.keyCode === 13) {
                loadSearchResult(1);
            }
        });

        // 页面加载时，如果URL有keyword参数则自动搜索
        var initKeyword = '${fn:escapeXml(keyword)}';
        if (initKeyword) {
            loadSearchResult(1);
        }
    });
</script>
