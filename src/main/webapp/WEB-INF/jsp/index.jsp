<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <!-- Hero 区域 -->
    <div class="hero-section fade-in-up">
        <h1>校园失物招领智能匹配系统</h1>
        <p>基于 TF-IDF 算法的智能匹配，让每一件失物都能找到回家的路</p>
        <div style="display:flex;gap:12px;justify-content:center;flex-wrap:wrap;">
            <a href="/lost/publish" class="layui-btn layui-btn-lg hero-btn-lost">
                <i class="layui-icon layui-icon-add-1"></i> 我丢了东西
            </a>
            <a href="/found/publish" class="layui-btn layui-btn-lg hero-btn-found">
                <i class="layui-icon layui-icon-add-1"></i> 我捡到东西
            </a>
        </div>
    </div>

    <!-- KPI 统计卡片 -->
    <div class="kpi-grid">
        <div class="kpi-card kpi-danger fade-in-up">
            <div class="kpi-label"><i class="layui-icon layui-icon-about" style="color:var(--danger);"></i> 丢失总数</div>
            <div class="kpi-value" id="totalLost">0<span class="kpi-suffix">件</span></div>
        </div>
        <div class="kpi-card kpi-primary fade-in-up">
            <div class="kpi-label"><i class="layui-icon layui-icon-template-1" style="color:var(--accent);"></i> 拾到总数</div>
            <div class="kpi-value" id="totalFound">0<span class="kpi-suffix">件</span></div>
        </div>
        <div class="kpi-card kpi-success fade-in-up">
            <div class="kpi-label"><i class="layui-icon layui-icon-ok-circle" style="color:var(--success);"></i> 已找回</div>
            <div class="kpi-value" id="totalRecovered">0<span class="kpi-suffix">件</span></div>
        </div>
        <div class="kpi-card kpi-warning fade-in-up">
            <div class="kpi-label"><i class="layui-icon layui-icon-time" style="color:var(--warning);"></i> 待处理</div>
            <div class="kpi-value" id="totalPending">0<span class="kpi-suffix">件</span></div>
        </div>
    </div>

    <!-- 最新公告 + 快捷入口 -->
    <div class="layui-row layui-col-space20">
        <div class="layui-col-md8">
            <div class="custom-card">
                <div class="card-title">
                    <span><i class="layui-icon layui-icon-notice"></i>最新公告</span>
                </div>
                <div id="announcementList">
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-notice"></i>
                        <p>暂无公告</p>
                    </div>
                </div>
            </div>
        </div>
        <div class="layui-col-md4">
            <div class="custom-card">
                <div class="card-title">
                    <span><i class="layui-icon layui-icon-app"></i>快捷入口</span>
                </div>
                <div class="layui-row layui-col-space15">
                    <div class="layui-col-xs6">
                        <a href="/lost/publish" class="layui-btn layui-btn-fluid layui-btn-normal" style="height:56px;line-height:56px;font-size:15px;border-radius:var(--radius-md);">
                            <i class="layui-icon layui-icon-add-1"></i> 发布丢失
                        </a>
                    </div>
                    <div class="layui-col-xs6">
                        <a href="/found/publish" class="layui-btn layui-btn-fluid layui-btn-warm" style="height:56px;line-height:56px;font-size:15px;border-radius:var(--radius-md);">
                            <i class="layui-icon layui-icon-add-1"></i> 发布拾到
                        </a>
                    </div>
                    <div class="layui-col-xs6">
                        <a href="/lost/list" class="layui-btn layui-btn-fluid" style="height:56px;line-height:56px;font-size:15px;border-radius:var(--radius-md);">
                            <i class="layui-icon layui-icon-list"></i> 丢失列表
                        </a>
                    </div>
                    <div class="layui-col-xs6">
                        <a href="/found/list" class="layui-btn layui-btn-fluid" style="height:56px;line-height:56px;font-size:15px;border-radius:var(--radius-md);">
                            <i class="layui-icon layui-icon-template-1"></i> 拾到列表
                        </a>
                    </div>
                    <div class="layui-col-xs12">
                        <a href="/dashboard" class="layui-btn layui-btn-fluid layui-btn-primary" style="height:48px;line-height:48px;font-size:14px;border-radius:var(--radius-md);">
                            <i class="layui-icon layui-icon-chart"></i> 数据看板
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 最新丢失物品 -->
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-list"></i>最新丢失物品</span>
            <a href="/lost/list" style="font-size:14px;">查看更多 &gt;&gt;</a>
        </div>
        <div class="item-grid" id="lostList">
            <div class="empty-state">
                <i class="layui-icon layui-icon-face-surprised"></i>
                <p>暂无丢失物品</p>
            </div>
        </div>
    </div>

    <!-- 最新拾到物品 -->
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-template-1"></i>最新拾到物品</span>
            <a href="/found/list" style="font-size:14px;">查看更多 &gt;&gt;</a>
        </div>
        <div class="item-grid" id="foundList">
            <div class="empty-state">
                <i class="layui-icon layui-icon-face-surprised"></i>
                <p>暂无拾到物品</p>
            </div>
        </div>
    </div>

    <!-- 猜你喜欢 -->
    <div class="custom-card" id="recommendCard" style="display:none;">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-fire"></i>猜你喜欢</span>
        </div>
        <div class="item-grid" id="recommendList">
            <div class="empty-state">
                <i class="layui-icon layui-icon-loading"></i>
                <p>加载中...</p>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer'], function () {
        var layer = layui.layer;

        // 加载统计数据
        function loadOverview() {
            ajaxRequest('/api/dashboard/overview', 'GET', null, function (data) {
                $('#totalLost').html((data.totalLost || 0) + '<span class="kpi-suffix">件</span>');
                $('#totalFound').html((data.totalFound || 0) + '<span class="kpi-suffix">件</span>');
                $('#totalRecovered').html((data.totalRecovered || 0) + '<span class="kpi-suffix">件</span>');
                $('#totalPending').html((data.totalPending || 0) + '<span class="kpi-suffix">件</span>');
            });
        }

        // 加载公告
        function loadAnnouncements() {
            ajaxRequest('/api/announcement/list', 'GET', null, function (data) {
                if (!data || data.length === 0) return;
                var html = '';
                data.forEach(function (item, index) {
                    if (index >= 5) return;
                    html += '<div style="padding:12px 0;border-bottom:1px solid var(--border-subtle);">';
                    html += '  <div style="display:flex;justify-content:space-between;align-items:center;">';
                    html += '    <span style="font-weight:600;color:var(--text-primary);"><i class="layui-icon layui-icon-notice" style="color:var(--accent);margin-right:6px;"></i>' + escapeHtml(item.title) + '</span>';
                    html += '    <span style="color:var(--text-tertiary);font-size:12px;">' + formatDate(item.createTime) + '</span>';
                    html += '  </div>';
                    if (item.content) {
                        html += '  <p style="color:var(--text-secondary);font-size:13px;margin-top:4px;">' + escapeHtml(truncateStr(item.content, 80)) + '</p>';
                    }
                    html += '</div>';
                });
                $('#announcementList').html(html);
            });
        }

        // 加载丢失物品列表
        function loadLostList() {
            ajaxRequest('/api/lost/list', 'GET', { page: 1, size: 6 }, function (data) {
                renderItemList('#lostList', data.records, 'lost');
            });
        }

        // 加载拾到物品列表
        function loadFoundList() {
            ajaxRequest('/api/found/list', 'GET', { page: 1, size: 6 }, function (data) {
                renderItemList('#foundList', data.records, 'found');
            });
        }

        // 加载推荐物品
        function loadRecommendations() {
            var user = getCurrentUser();
            var recommendUrl = user ? '/api/recommend/user?limit=6' : '/api/recommend/guest?limit=6';
            ajaxRequest(recommendUrl, 'GET', null, function (data) {
                if (!data || data.length === 0) return;
                $('#recommendCard').show();
                // 推荐数据可能不区分 lost/found，统一用 itemType 字段
                data.forEach(function(item) {
                    if (!item.itemType) item.itemType = 'lost';
                });
                renderItemList('#recommendList', data, 'lost');
            }, function () {
                // 推荐失败时静默处理
            });
        }

        // 渲染物品列表（Bento Grid卡片）
        function renderItemList(container, list, type) {
            if (!list || list.length === 0) return;
            var html = '';
            list.forEach(function (item) {
                var img = getFirstImage(item.images);
                var imgHtml = img
                    ? '<img class="item-card-image" src="' + escapeHtml(getImageUrl(img)) + '" alt="' + escapeHtml(item.title) + '">'
                    : '<div class="item-card-image" style="display:flex;align-items:center;justify-content:center;"><i class="layui-icon layui-icon-picture" style="font-size:48px;color:var(--gray-300);"></i></div>';
                var statusBadge = '<span class="status-tag ' + getStatusClass(item.status) + '">' + getStatusText(item.status) + '</span>';
                var detailUrl = '/item/detail?type=' + type + '&id=' + (item.id || item.itemId);
                html += '<div class="item-card" onclick="window.location.href=\'' + detailUrl + '\'">';
                html += '  ' + imgHtml;
                html += '  <div class="item-card-body">';
                html += '    <div class="item-card-title">' + escapeHtml(item.title) + '</div>';
                html += '    <div class="item-card-desc">' + escapeHtml(truncateStr(item.description || '', 60)) + '</div>';
                html += '    <div class="item-card-meta">';
                html += '      <span class="meta-location"><i class="layui-icon layui-icon-location"></i>' + escapeHtml(truncateStr(item.location || '未知', 15)) + '</span>';
                html += '      ' + statusBadge;
                html += '    </div>';
                html += '  </div>';
                html += '</div>';
            });
            $(container).html(html);
        }

        // 页面加载
        $(document).ready(function () {
            loadOverview();
            loadAnnouncements();
            loadLostList();
            loadFoundList();
            loadRecommendations();
        });
    });
</script>
