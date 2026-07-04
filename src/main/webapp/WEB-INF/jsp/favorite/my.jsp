<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-star"></i>我的收藏</span>
            <span style="font-size:13px;color:var(--text-tertiary);">点击卡片查看详情，点击取消收藏移除记录</span>
        </div>
        <div class="item-grid" id="favList">
            <div class="empty-state">
                <div class="loading-spinner" style="margin:0 auto 10px;"></div>
                <p>正在加载...</p>
            </div>
        </div>
    </div>

    <!-- 分页 -->
    <div id="pagination"></div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'laypage'], function () {
        var layer = layui.layer;
        var laypage = layui.laypage;

        // 校验登录状态
        if (!checkLogin(true)) return;

        var currentPage = 1;
        var pageSize = 12;
        var totalCount = 0;

        // 加载收藏列表
        function loadFavorites(page) {
            currentPage = page || 1;
            ajaxRequest('/api/favorite/my', 'GET', { page: currentPage, size: pageSize }, function (data) {
                // 估算总数边界：当总数恰好是 pageSize 的整数倍时，下一页可能返回空
                if (data.length === 0 && currentPage > 1) {
                    layer.msg('没有更多收藏', { icon: 0, time: 1200 });
                    loadFavorites(currentPage - 1);
                    return;
                }
                renderList(data);
                // 接口返回当前页数据列表（无 total 字段），通过本页数据量估算总数以驱动分页
                if (data.length === pageSize) {
                    // 本页满载，可能还存在下一页
                    totalCount = currentPage * pageSize + 1;
                } else {
                    // 本页未满载，说明已是最后一页
                    totalCount = (currentPage - 1) * pageSize + data.length;
                }
                renderPagination();
            }, function () {
                $('#favList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-crying"></i><p>加载失败，请刷新重试</p></div>');
                $('#pagination').empty();
            });
        }

        // 渲染卡片列表
        function renderList(list) {
            if (!list || list.length === 0) {
                $('#favList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无收藏记录</p></div>');
                return;
            }
            var html = '';
            list.forEach(function (item) {
                var img = getFirstImage(item.images);
                var imgHtml = img
                    ? '<img src="' + escapeHtml(getImageUrl(img)) + '" alt="' + escapeHtml(item.title || '') + '">'
                    : '<i class="layui-icon layui-icon-picture no-image"></i>';
                var detailUrl = '/item/detail?type=' + encodeURIComponent(item.itemType || '') + '&id=' + item.itemId;
                var title = escapeHtml(item.title || '未命名物品');
                var location = escapeHtml(truncateStr(item.location, 18));
                var category = getCategoryText(item.category);
                var statusText = getStatusText(item.status);
                var statusClass = getStatusClass(item.status);
                var time = formatDate(item.createTime);

                html += '<div class="item-card">';
                html += '    <div class="item-image" onclick="window.location.href=\'' + detailUrl + '\'">' + imgHtml + '</div>';
                html += '    <div class="item-body">';
                html += '      <div class="item-title" style="cursor:pointer;" onclick="window.location.href=\'' + detailUrl + '\'">' + title + '</div>';
                html += '      <div class="item-meta"><span class="layui-badge layui-bg-orange">' + category + '</span></div>';
                html += '      <div class="item-meta"><i class="layui-icon layui-icon-location"></i>' + (location || '-') + '</div>';
                html += '      <div class="item-meta"><i class="layui-icon layui-icon-time"></i>' + time + '</div>';
                html += '      <div style="margin-top:6px;display:flex;justify-content:space-between;align-items:center;">';
                html += '        <span class="status-tag ' + statusClass + '">' + statusText + '</span>';
                html += '        <button class="layui-btn layui-btn-sm layui-btn-danger" onclick="unfavorite(' + item.itemId + ',\'' + (item.itemType || '') + '\')"><i class="layui-icon layui-icon-star"></i>取消收藏</button>';
                html += '      </div>';
                html += '    </div>';
                html += '</div>';
            });
            $('#favList').html(html);
        }

        // 渲染分页
        function renderPagination() {
            if (totalCount <= 0) {
                $('#pagination').empty();
                return;
            }
            laypage.render({
                elem: 'pagination',
                count: totalCount,
                limit: pageSize,
                curr: currentPage,
                jump: function (obj, first) {
                    if (!first) {
                        loadFavorites(obj.curr);
                    }
                }
            });
        }

        // 取消收藏
        window.unfavorite = function (itemId, itemType) {
            layer.confirm('确定取消收藏该物品吗？', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/favorite/toggle', 'POST', { itemId: itemId, itemType: itemType }, function () {
                    layer.msg('已取消收藏', { icon: 1, time: 1500 });
                    loadFavorites(currentPage);
                });
            });
        };

        // 初始加载
        loadFavorites(1);
    });
</script>
