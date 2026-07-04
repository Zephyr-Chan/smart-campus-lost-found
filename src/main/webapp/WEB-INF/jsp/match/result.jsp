<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <!-- 丢失物品摘要信息 -->
    <div class="custom-card" id="lostSummary">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-list"></i>丢失物品信息</span>
            <a href="/lost/list" class="layui-btn layui-btn-sm layui-btn-primary">返回列表</a>
        </div>
        <div class="empty-state">
            <div class="loading-spinner" style="margin:0 auto 16px;"></div>
            <p>正在加载...</p>
        </div>
    </div>

    <!-- 匹配结果列表 -->
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-search"></i>TF-IDF 智能匹配结果</span>
            <span style="font-size:13px;color:var(--text-tertiary);" id="matchCount"></span>
        </div>
        <div id="matchResultList">
            <div class="empty-state">
                <p>正在计算匹配结果，请稍候...</p>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'element'], function () {
        var layer = layui.layer;
        var element = layui.element;

        var lostItemId = getQueryParam('lostItemId');
        var currentUser = getCurrentUser();

        if (!lostItemId) {
            $('#lostSummary').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>参数错误：缺少 lostItemId</p></div>');
            $('#matchResultList').html('');
            return;
        }

        // 加载丢失物品详情
        function loadLostDetail() {
            ajaxRequest('/api/lost/get/' + lostItemId, 'GET', null, function (data) {
                renderLostSummary(data);
            });
        }

        // 渲染丢失物品摘要
        function renderLostSummary(item) {
            var html = '';
            html += '<div class="card-title"><span><i class="layui-icon layui-icon-list"></i>丢失物品信息</span>';
            html += '<a href="/lost/list" class="layui-btn layui-btn-sm layui-btn-primary">返回列表</a></div>';
            html += '<div class="layui-row layui-col-space20">';
            html += '  <div class="layui-col-md8">';
            html += '    <h2 style="font-size:20px;margin-bottom:12px;color:#0F172A;">' + escapeHtml(item.title) + '</h2>';
            html += '    <div class="layui-row layui-col-space10">';
            html += '      <div class="layui-col-md6"><span style="color:#94A3B8;">分类：</span>' + getCategoryText(item.category) + '</div>';
            html += '      <div class="layui-col-md6"><span style="color:#94A3B8;">地点：</span>' + escapeHtml(item.location) + '</div>';
            html += '      <div class="layui-col-md6"><span style="color:#94A3B8;">丢失时间：</span>' + formatDate(item.eventTime) + '</div>';
            html += '      <div class="layui-col-md6"><span style="color:#94A3B8;">发布者：</span>' + (item.publisherName || '匿名') + '</div>';
            html += '    </div>';
            html += '    <p style="color:#475569;margin-top:8px;line-height:1.6;">' + escapeHtml(item.description) + '</p>';
            html += '  </div>';
            html += '  <div class="layui-col-md4" style="text-align:right;">';
            html += '    <span class="status-tag ' + getStatusClass(item.status) + '">' + getStatusText(item.status) + '</span>';
            html += '  </div>';
            html += '</div>';
            $('#lostSummary').html(html);
        }

        // 加载匹配结果
        function loadMatchResult() {
            showLoading();
            ajaxRequest('/api/match/' + lostItemId, 'GET', null, function (data) {
                hideLoading();
                renderMatchResult(data);
            }, function () {
                hideLoading();
                $('#matchResultList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-crying"></i><p>匹配失败，请稍后重试</p></div>');
            });
        }

        // 渲染匹配结果
        function renderMatchResult(list) {
            if (!list || list.length === 0) {
                $('#matchCount').text('');
                $('#matchResultList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无匹配的拾到物品</p></div>');
                return;
            }

            $('#matchCount').text('共匹配到 ' + list.length + ' 个结果');

            var html = '';
            list.forEach(function (item, index) {
                var score = (item.matchScore * 100).toFixed(1);
                var scoreColor = score >= 70 ? '#16A34A' : (score >= 40 ? '#D97706' : '#DC2626');
                var img = getFirstImage(item.images);
                var imgHtml = img ? '<img src="' + escapeHtml(getImageUrl(img)) + '" style="width:120px;height:120px;object-fit:cover;border-radius:8px;">' : '<div style="width:120px;height:120px;display:flex;align-items:center;justify-content:center;background:#F1F5F9;border-radius:8px;"><i class="layui-icon layui-icon-picture" style="font-size:40px;color:#94A3B8;"></i></div>';

                // 进度条颜色
                var progressColor = score >= 70 ? '#16A34A' : (score >= 40 ? '#D97706' : '#DC2626');

                html += '<div class="match-item" onclick="window.location.href=\'/item/detail?type=found&id=' + item.foundItemId + '\'" style="cursor:pointer;">';
                html += '  <div class="layui-row layui-col-space15">';
                html += '    <div class="layui-col-md2">' + imgHtml + '</div>';
                html += '    <div class="layui-col-md6">';
                html += '      <h4 style="font-size:16px;margin-bottom:8px;color:#0F172A;">' + escapeHtml(item.title) + '</h4>';
                html += '      <p style="color:#475569;font-size:13px;margin-bottom:4px;"><i class="layui-icon layui-icon-note"></i> 分类：' + getCategoryText(item.category) + '</p>';
                html += '      <p style="color:#475569;font-size:13px;margin-bottom:4px;"><i class="layui-icon layui-icon-location"></i> 地点：' + escapeHtml(item.location) + '</p>';
                html += '      <p style="color:#475569;font-size:13px;margin-bottom:4px;"><i class="layui-icon layui-icon-time"></i> 时间：' + formatDate(item.eventTime) + '</p>';
                html += '      <p style="color:#475569;font-size:13px;"><i class="layui-icon layui-icon-username"></i> 拾取者：' + (item.finderName || '匿名') + '</p>';
                html += '      <p style="color:#94A3B8;font-size:12px;margin-top:4px;">' + escapeHtml(truncateStr(item.description, 60)) + '</p>';
                html += '    </div>';
                html += '    <div class="layui-col-md4" style="text-align:center;">';
                html += '      <div style="font-size:32px;font-weight:bold;color:' + scoreColor + ';">' + score + '%</div>';
                html += '      <div style="font-size:13px;color:#94A3B8;margin-bottom:10px;">相似度</div>';
                // 进度条
                html += '      <div class="layui-progress" lay-filter="progress' + index + '" style="margin-bottom:10px;">';
                html += '        <div class="layui-progress-bar" lay-percent="' + score + '%" style="background-color:' + progressColor + ';"></div>';
                html += '      </div>';
                if (isLogin()) {
                    html += '      <button class="layui-btn layui-btn-sm layui-btn-normal" onclick="event.stopPropagation();initiateClaim(' + lostItemId + ',' + item.foundItemId + ',' + item.matchScore + ')">发起认领</button>';
                }
                html += '    </div>';
                html += '  </div>';
                html += '</div>';
            });
            $('#matchResultList').html(html);

            // 重新渲染进度条
            list.forEach(function (item, index) {
                element.render('progress', 'progress' + index);
            });
        }

        // 发起认领
        window.initiateClaim = function (lostItemId, foundItemId, matchScore) {
            if (!isLogin()) {
                layer.msg('请先登录', { icon: 2, time: 1500 }, function () {
                    window.location.href = '/login';
                });
                return;
            }
            layer.prompt({
                title: '请输入认领备注（选填）',
                formType: 2,
                area: ['400px', '120px']
            }, function (value, index) {
                $.ajax({
                    url: '/api/claim',
                    type: 'POST',
                    headers: { 'Authorization': 'Bearer ' + getToken() },
                    data: {
                        lostItemId: lostItemId,
                        foundItemId: foundItemId,
                        matchScore: matchScore,
                        remark: value
                    },
                    success: function (res) {
                        if (res.code === 200) {
                            layer.close(index);
                            layer.msg('认领申请已提交', { icon: 1, time: 1500 }, function () {
                                window.location.href = '/claim/my';
                            });
                        } else {
                            layer.msg(res.msg || '认领失败', { icon: 2 });
                        }
                    },
                    error: function () {
                        layer.msg('网络错误', { icon: 2 });
                    }
                });
            });
        };

        // 加载数据
        loadLostDetail();
        loadMatchResult();
    });
</script>
