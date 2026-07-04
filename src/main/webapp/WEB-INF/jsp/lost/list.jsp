<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
        <div class="layui-inline">
            <select name="category" id="searchCategory" lay-filter="searchCategory">
                <option value="">全部分类</option>
                <option value="electronics">电子产品</option>
                <option value="certificate">证件</option>
                <option value="book">书籍</option>
                <option value="clothing">衣物</option>
                <option value="other">其他</option>
            </select>
        </div>
        <div class="layui-inline">
            <input type="text" name="keyword" id="searchKeyword" placeholder="输入关键词搜索..."
                   class="layui-input" style="width:240px;" autocomplete="off">
        </div>
        <button class="layui-btn layui-btn-normal" id="btnSearch">
            <i class="layui-icon layui-icon-search"></i> 搜索
        </button>
        <button class="layui-btn layui-btn-primary" id="btnReset">
            <i class="layui-icon layui-icon-refresh"></i> 重置
        </button>
    </div>

    <!-- 物品列表 -->
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-list"></i>丢失物品列表</span>
            <c:if test="${not empty param.fromPublish}">
                <span style="color:var(--success);font-size:14px;"><i class="layui-icon layui-icon-ok-circle"></i> 发布成功！</span>
            </c:if>
        </div>
        <div class="item-grid" id="itemList">
            <div class="empty-state">
                <i class="layui-icon layui-icon-face-surprised"></i>
                <p>暂无数据，正在加载...</p>
            </div>
        </div>
    </div>

    <!-- 分页 -->
    <div id="pagination"></div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'laypage', 'form'], function () {
        var layer = layui.layer;
        var laypage = layui.laypage;
        var form = layui.form;

        var currentPage = 1;
        var pageSize = 10;

        // 加载列表数据
        function loadData(page) {
            currentPage = page || 1;
            var category = $('#searchCategory').val();
            var keyword = $('#searchKeyword').val();

            var params = { page: currentPage, size: pageSize };
            if (category) params.category = category;
            if (keyword) params.keyword = keyword;

            showLoading();
            ajaxRequest('/api/lost/list', 'GET', params, function (data) {
                hideLoading();
                renderList(data.records);
                renderPagination(data.total);
            }, function () {
                hideLoading();
            });
        }

        // 渲染列表
        function renderList(list) {
            if (!list || list.length === 0) {
                $('#itemList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无丢失物品记录</p></div>');
                return;
            }
            var html = '';
            list.forEach(function (item) {
                var img = getFirstImage(item.images);
                var imgHtml = img
                    ? '<img src="' + escapeHtml(getImageUrl(img)) + '" alt="' + escapeHtml(item.title) + '">'
                    : '<i class="layui-icon layui-icon-picture no-image"></i>';
                html += '<div class="item-card" onclick="window.location.href=\'/item/detail?type=lost&id=' + item.id + '\'">';
                html += '    <div class="item-image">' + imgHtml + '</div>';
                html += '    <div class="item-body">';
                html += '      <div class="item-title">' + escapeHtml(item.title) + '</div>';
                html += '      <div class="item-meta"><i class="layui-icon layui-icon-note"></i>' + getCategoryText(item.category) + '</div>';
                html += '      <div class="item-meta"><i class="layui-icon layui-icon-location"></i>' + escapeHtml(truncateStr(item.location, 20)) + '</div>';
                html += '      <div class="item-meta"><i class="layui-icon layui-icon-time"></i>' + formatDate(item.eventTime) + '</div>';
                html += '      <div style="margin-top:6px;"><span class="status-tag ' + getStatusClass(item.status) + '">' + getStatusText(item.status) + '</span></div>';
                html += '    </div>';
                html += '</div>';
            });
            $('#itemList').html(html);
        }

        // 渲染分页
        function renderPagination(total) {
            if (total <= 0) {
                $('#pagination').empty();
                return;
            }
            laypage.render({
                elem: 'pagination',
                count: total,
                limit: pageSize,
                curr: currentPage,
                jump: function (obj, first) {
                    if (!first) {
                        loadData(obj.curr);
                    }
                }
            });
        }

        // 搜索
        $('#btnSearch').on('click', function () {
            loadData(1);
        });

        // 回车搜索
        $('#searchKeyword').on('keydown', function (e) {
            if (e.keyCode === 13) {
                loadData(1);
            }
        });

        // 重置
        $('#btnReset').on('click', function () {
            $('#searchCategory').val('');
            $('#searchKeyword').val('');
            form.render('select');
            loadData(1);
        });

        // 初始加载
        loadData(1);
    });
</script>
