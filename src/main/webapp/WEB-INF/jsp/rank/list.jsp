<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<%-- 服务端时间：用于榜单更新时间展示 --%>
<jsp:useBean id="rankTime" class="java.util.Date" />
<fmt:formatDate value="${rankTime}" pattern="yyyy-MM-dd HH:mm" var="rankUpdateTime" />
<%-- 从URL参数读取初始榜单类型 --%>
<c:set var="initType" value="${empty param.type ? 'credit' : param.type}" />

<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-chart"></i> 贡献排行榜</span>
            <span style="font-size:14px;color:var(--text-tertiary);">
                <i class="layui-icon layui-icon-time"></i> 更新于 ${rankUpdateTime}
                <c:if test="${not empty currentYear}">
                    <span style="margin-left:10px;">&copy; ${currentYear}</span>
                </c:if>
            </span>
        </div>

        <!-- 榜单切换Tab -->
        <div class="layui-tab layui-tab-brief" lay-filter="rankTab">
            <ul class="layui-tab-title">
                <li data-type="credit" <c:if test="${initType == 'credit'}">class="layui-this"</c:if><c:if test="${initType != 'credit'}">class=""</c:if>><i class="layui-icon layui-icon-diamond"></i> 积分榜</li>
                <li data-type="recovery" <c:if test="${initType == 'recovery'}">class="layui-this"</c:if><c:if test="${initType != 'recovery'}">class=""</c:if>><i class="layui-icon layui-icon-refresh-3"></i> 找回率榜</li>
                <li data-type="contribution" <c:if test="${initType == 'contribution'}">class="layui-this"</c:if><c:if test="${initType != 'contribution'}">class=""</c:if>><i class="layui-icon layui-icon-template-1"></i> 贡献榜</li>
            </ul>
            <div class="layui-tab-content">
                <!-- 前三名领奖台 -->
                <div class="rank-podium" id="rankPodium" style="display:flex;justify-content:center;align-items:flex-end;gap:20px;padding:30px 10px 20px;flex-wrap:wrap;">
                    <div class="loading-spinner"></div>
                </div>

                <!-- 第四名及以后列表 -->
                <table class="layui-table" id="rankTable" style="margin-top:10px;">
                    <thead id="rankThead">
                        <tr>
                            <th width="70">排名</th>
                            <th>用户</th>
                            <th width="120">数值</th>
                        </tr>
                    </thead>
                    <tbody id="rankList">
                        <tr><td colspan="3" style="text-align:center;padding:30px;color:var(--text-tertiary);">正在加载...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<style>
    /* 领奖台卡片样式 */
    .podium-item { text-align:center; width:160px; transition:all .3s; }
    .podium-item .podium-avatar { width:64px;height:64px;border-radius:50%;object-fit:cover;margin:0 auto 8px;display:block;border:3px solid var(--gray-200);background:var(--gray-100); }
    .podium-item .podium-name { font-size:14px;font-weight:600;color:var(--text-primary);margin-bottom:4px; }
    .podium-item .podium-score { font-size:13px;color:var(--text-tertiary); }
    .podium-item .podium-no { display:inline-block;width:28px;height:28px;line-height:28px;border-radius:50%;color:#fff;font-weight:700;margin-bottom:8px; }
    .podium-rank1 .podium-avatar { border-color:#FFD700; box-shadow:0 0 12px rgba(255,215,0,.5); }
    .podium-rank1 .podium-no { background:#FFD700; }
    .podium-rank2 .podium-avatar { border-color:#C0C0C0; box-shadow:0 0 10px rgba(192,192,192,.5); }
    .podium-rank2 .podium-no { background:#C0C0C0; }
    .podium-rank3 .podium-avatar { border-color:#CD7F32; box-shadow:0 0 10px rgba(205,127,50,.5); }
    .podium-rank3 .podium-no { background:#CD7F32; }
    .podium-rank1 .podium-block { height:90px;background:linear-gradient(180deg,#FFF8DC,#FFD700); }
    .podium-rank2 .podium-block { height:66px;background:linear-gradient(180deg,#F5F5F5,#C0C0C0); }
    .podium-rank3 .podium-block { height:46px;background:linear-gradient(180deg,#FBE9D0,#CD7F32); }
    .podium-block { border-radius:6px 6px 0 0;display:flex;align-items:flex-start;justify-content:center;padding-top:6px;font-weight:700;color:#fff;font-size:18px; }
    .rank-row td { vertical-align:middle; }
    .rank-no { display:inline-block;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;background:var(--accent);color:#fff;font-size:12px;font-weight:600; }
    .rank-avatar { width:32px;height:32px;border-radius:50%;object-fit:cover;vertical-align:middle;margin-right:8px;background:var(--gray-100); }
    .rank-avatar-default { display:inline-block;width:32px;height:32px;line-height:32px;text-align:center;border-radius:50%;background:var(--accent);color:#fff;margin-right:8px;font-size:14px; }
</style>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'element'], function () {
        var layer = layui.layer;
        var element = layui.element;

        // 当前榜单类型：由服务端EL注入初始值
        var currentType = '${initType}' || 'credit';
        var rankLimit = 20;

        // 各榜单表头与数值字段配置
        var typeConfig = {
            credit: {
                scoreKey: 'score',
                scoreLabel: '信用积分',
                unit: '分',
                columns: ['排名', '用户', '信用积分']
            },
            recovery: {
                scoreKey: 'recoveryRate',
                scoreLabel: '找回率',
                unit: '%',
                columns: ['排名', '用户', '找回率', '找回/丢失']
            },
            contribution: {
                scoreKey: 'contribution',
                scoreLabel: '贡献值',
                unit: '',
                columns: ['排名', '用户', '贡献值', '拾到/通过']
            }
        };

        // 头像处理
        function getAvatarHtml(item) {
            var avatar = item.avatar || '';
            var name = item.realName || item.username || '用户';
            if (avatar) {
                var url = getImageUrl(avatar);
                return '<img class="rank-avatar" src="' + url + '" onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'inline-block\'"><span class="rank-avatar-default" style="display:none;">' + escapeHtml(name.charAt(0)) + '</span>';
            }
            return '<span class="rank-avatar-default">' + escapeHtml(name.charAt(0)) + '</span>';
        }

        // 渲染表头
        function renderThead(type) {
            var cols = typeConfig[type].columns;
            var html = '<tr>';
            cols.forEach(function (col) {
                html += '<th>' + col + '</th>';
            });
            html += '</tr>';
            $('#rankThead').html(html);
        }

        // 加载榜单数据
        function loadRankList(type) {
            currentType = type;
            renderThead(type);
            $('#rankPodium').html('<div class="loading-spinner"></div>');
            $('#rankList').html('<tr><td colspan="4" style="text-align:center;padding:30px;color:#94A3B8;">正在加载...</td></tr>');

            ajaxRequest('/api/rank/list', 'GET', { type: type, limit: rankLimit }, function (list) {
                renderPodium(list, type);
                renderRankTable(list, type);
            }, function () {
                $('#rankPodium').html('<div style="color:#94A3B8;text-align:center;width:100%;"><i class="layui-icon layui-icon-face-crying"></i>加载失败，请刷新重试</div>');
                $('#rankList').html('<tr><td colspan="4" style="text-align:center;padding:30px;color:#94A3B8;">加载失败</td></tr>');
            });
        }

        // 渲染前三名领奖台
        function renderPodium(list, type) {
            if (!list || list.length === 0) {
                $('#rankPodium').html('<div style="color:#94A3B8;text-align:center;width:100%;"><i class="layui-icon layui-icon-face-surprised"></i>暂无排行数据</div>');
                return;
            }
            var cfg = typeConfig[type];
            // 取前三名，按 2-1-3 顺序展示（中间最高）
            var top3 = list.slice(0, 3);
            var order = [];
            if (top3[1]) order.push({ data: top3[1], no: 2, cls: 'podium-rank2' });
            if (top3[0]) order.push({ data: top3[0], no: 1, cls: 'podium-rank1' });
            if (top3[2]) order.push({ data: top3[2], no: 3, cls: 'podium-rank3' });

            var html = '';
            order.forEach(function (p) {
                var name = p.data.realName || p.data.username || '用户';
                var avatar = p.data.avatar ? '<img class="podium-avatar" src="' + getImageUrl(p.data.avatar) + '" onerror="this.src=\'\'">'
                    : '<div class="podium-avatar" style="display:flex;align-items:center;justify-content:center;font-size:24px;color:var(--accent);">' + escapeHtml(name.charAt(0)) + '</div>';
                var scoreVal = p.data[cfg.scoreKey];
                var scoreText = scoreVal !== undefined && scoreVal !== null ? scoreVal + cfg.unit : '-';
                html += '<div class="podium-item ' + p.cls + '">';
                html += '  <span class="podium-no">' + p.no + '</span>';
                html += '  ' + avatar;
                html += '  <div class="podium-name">' + escapeHtml(name) + '</div>';
                html += '  <div class="podium-score">' + cfg.scoreLabel + '：' + scoreText + '</div>';
                html += '  <div class="podium-block">' + p.no + '</div>';
                html += '</div>';
            });
            $('#rankPodium').html(html);
        }

        // 渲染第四名及以后表格
        function renderRankTable(list, type) {
            if (!list || list.length <= 3) {
                $('#rankList').html('<tr><td colspan="4" style="text-align:center;padding:20px;color:#94A3B8;">暂无更多排名</td></tr>');
                return;
            }
            var cfg = typeConfig[type];
            var html = '';
            for (var i = 3; i < list.length; i++) {
                var item = list[i];
                var name = item.realName || item.username || '用户';
                var scoreVal = item[cfg.scoreKey];
                var scoreText = scoreVal !== undefined && scoreVal !== null ? scoreVal + cfg.unit : '-';

                html += '<tr class="rank-row">';
                html += '  <td><span class="rank-no">' + (i + 1) + '</span></td>';
                html += '  <td>' + getAvatarHtml(item) + escapeHtml(name) + '</td>';
                html += '  <td><strong style="color:var(--accent);">' + scoreText + '</strong></td>';
                // 扩展列
                if (type === 'recovery') {
                    html += '<td style="color:#94A3B8;">' + (item.recovered || 0) + '/' + (item.totalLost || 0) + '</td>';
                } else if (type === 'contribution') {
                    html += '<td style="color:#94A3B8;">' + (item.foundCount || 0) + '/' + (item.approvedCount || 0) + '</td>';
                }
                html += '</tr>';
            }
            $('#rankList').html(html);
        }

        // Tab切换事件
        element.on('tab(rankTab)', function (data) {
            var type = $(data.elem).data('type');
            if (type && type !== currentType) {
                loadRankList(type);
            }
        });

        // 初始化加载
        loadRankList(currentType);
    });
</script>
