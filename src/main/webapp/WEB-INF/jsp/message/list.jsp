<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<%-- 服务端时间：用于页面数据更新时间展示 --%>
<jsp:useBean id="serverTime" class="java.util.Date" />
<fmt:formatDate value="${serverTime}" pattern="yyyy-MM-dd HH:mm" var="updateTime" />
<%-- 从URL参数读取初始页码与已读筛选 --%>
<c:set var="initPage" value="${empty param.page ? 1 : param.page}" />
<c:set var="initIsRead" value="${param.isRead}" />

<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-notice"></i> 站内消息</span>
            <span style="font-size:14px;color:var(--text-tertiary);">
                <i class="layui-icon layui-icon-time"></i> 更新于 ${updateTime}
                <c:if test="${not empty currentYear}">
                    <span style="margin-left:10px;">&copy; ${currentYear}</span>
                </c:if>
            </span>
        </div>

        <!-- 顶部操作栏：未读铃铛 + 筛选 + 全部已读 -->
        <div class="msg-toolbar" style="display:flex;align-items:center;justify-content:space-between;margin-bottom:15px;flex-wrap:wrap;gap:10px;">
            <div style="display:flex;align-items:center;gap:12px;">
                <span class="msg-bell" id="msgBell" style="position:relative;cursor:pointer;font-size:22px;color:var(--accent);">
                    <i class="layui-icon layui-icon-notice"></i>
                    <span class="msg-badge" id="unreadBadge" style="display:none;position:absolute;top:-6px;right:-8px;background:var(--danger);color:#fff;font-size:11px;min-width:16px;height:16px;line-height:16px;text-align:center;border-radius:8px;padding:0 4px;">0</span>
                </span>
                <div class="layui-btn-group" id="filterGroup">
                    <button class="layui-btn layui-btn-sm layui-btn-primary layui-btn-active" data-isread="">全部</button>
                    <button class="layui-btn layui-btn-sm layui-btn-primary" data-isread="0">未读</button>
                    <button class="layui-btn layui-btn-sm layui-btn-primary" data-isread="1">已读</button>
                </div>
            </div>
            <button class="layui-btn layui-btn-sm layui-btn-normal" id="btnReadAll">
                <i class="layui-icon layui-icon-ok"></i> 全部标为已读
            </button>
        </div>

        <!-- 消息列表表格 -->
        <table class="layui-table" id="msgTable" style="margin-top:0;">
            <thead>
                <tr>
                    <th width="60">类型</th>
                    <th>标题</th>
                    <th width="150">时间</th>
                    <th width="80">状态</th>
                    <th width="160">操作</th>
                </tr>
            </thead>
            <tbody id="msgList">
                <tr>
                    <td colspan="5" style="text-align:center;padding:40px;">
                        <div class="loading-spinner" style="margin:0 auto 10px;"></div>
                        正在加载...
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- 分页容器 -->
    <div id="msgPager" style="text-align:center;margin-top:15px;"></div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'laypage'], function () {
        var layer = layui.layer;
        var laypage = layui.laypage;

        // 检查登录状态
        if (!checkLogin(true)) return;

        // 当前查询状态：初始页码与筛选由服务端EL注入
        var currentPage = parseInt('${initPage}') || 1;
        var currentIsRead = '${initIsRead}' === '' ? '' : '${initIsRead}';
        var pageSize = 10;

        // 消息类型图标映射
        var typeIconMap = {
            'match': { icon: 'layui-icon-search', color: '#7C6B9E', text: '匹配' },
            'claim': { icon: 'layui-icon-file', color: '#D97706', text: '认领' },
            'credit': { icon: 'layui-icon-diamond', color: '#16A34A', text: '积分' },
            'system': { icon: 'layui-icon-notice', color: '#DC2626', text: '系统' }
        };

        function getTypeMeta(type) {
            return typeIconMap[type] || { icon: 'layui-icon-notice', color: '#94A3B8', text: '消息' };
        }

        // 加载消息列表
        function loadMessageList() {
            var params = { page: currentPage, size: pageSize };
            if (currentIsRead !== '') {
                params.isRead = currentIsRead;
            }
            $('#msgList').html('<tr><td colspan="5" style="text-align:center;padding:40px;"><div class="loading-spinner" style="margin:0 auto 10px;"></div>正在加载...</td></tr>');
            ajaxRequest('/api/message/list', 'GET', params, function (data) {
                renderMessageList(data);
                renderPager(data);
            }, function () {
                $('#msgList').html('<tr><td colspan="5" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-crying"></i>加载失败，请刷新重试</td></tr>');
            });
        }

        // 渲染消息列表
        function renderMessageList(data) {
            var list = data.list || [];
            if (list.length === 0) {
                $('#msgList').html('<tr><td colspan="5" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-surprised"></i>暂无消息</td></tr>');
                return;
            }
            var html = '';
            list.forEach(function (item) {
                var meta = getTypeMeta(item.type);
                var isRead = item.isRead === 1;
                var titleStyle = isRead ? 'color:#475569;' : 'color:#0F172A;font-weight:600;';
                var statusBadge = isRead
                    ? '<span class="status-tag status-closed">已读</span>'
                    : '<span class="status-tag status-pending">未读</span>';

                var actions = '';
                if (!isRead) {
                    actions += '<button class="layui-btn layui-btn-xs layui-btn-normal" onclick="markRead(' + item.id + ')">标为已读</button>';
                }
                actions += '<button class="layui-btn layui-btn-xs layui-btn-danger" onclick="deleteMsg(' + item.id + ')">删除</button>';

                html += '<tr style="' + (isRead ? '' : 'background:#F8FAFC;') + '">';
                html += '  <td style="text-align:center;"><i class="layui-icon ' + meta.icon + '" style="font-size:20px;color:' + meta.color + ';" title="' + meta.text + '"></i></td>';
                html += '  <td><div style="' + titleStyle + '">' + escapeHtml(item.title || '') + '</div>';
                if (item.content) {
                    html += '<div style="font-size:12px;color:#94A3B8;margin-top:4px;">' + escapeHtml(truncateStr(item.content, 60)) + '</div>';
                }
                html += '  </td>';
                html += '  <td style="color:#94A3B8;font-size:13px;">' + formatDate(item.createTime) + '</td>';
                html += '  <td>' + statusBadge + '</td>';
                html += '  <td>' + actions + '</td>';
                html += '</tr>';
            });
            $('#msgList').html(html);
        }

        // 渲染分页
        function renderPager(data) {
            var total = data.total || 0;
            if (total <= pageSize) {
                $('#msgPager').html('');
                return;
            }
            laypage.render({
                elem: 'msgPager',
                count: total,
                limit: pageSize,
                curr: currentPage,
                layout: ['prev', 'page', 'next', 'count'],
                jump: function (obj, first) {
                    currentPage = obj.curr;
                    if (!first) {
                        loadMessageList();
                    }
                }
            });
        }

        // 加载未读数
        function loadUnreadCount() {
            ajaxRequest('/api/message/unread-count', 'GET', null, function (count) {
                updateBell(count || 0);
            });
        }

        // 更新铃铛角标
        function updateBell(count) {
            var badge = $('#unreadBadge');
            if (count > 0) {
                badge.text(count > 99 ? '99+' : count).show();
            } else {
                badge.hide();
            }
        }

        // 标记单条已读
        window.markRead = function (id) {
            ajaxRequest('/api/message/read/' + id, 'PUT', null, function () {
                layer.msg('已标记为已读', { icon: 1, time: 1200 });
                loadMessageList();
                loadUnreadCount();
            });
        };

        // 删除消息
        window.deleteMsg = function (id) {
            layer.confirm('确定删除此消息吗？', { icon: 3, title: '确认删除' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/message/' + id, 'DELETE', null, function () {
                    layer.msg('已删除', { icon: 1, time: 1200 });
                    loadMessageList();
                    loadUnreadCount();
                });
            });
        };

        // 全部已读
        $('#btnReadAll').on('click', function () {
            layer.confirm('确定将所有未读消息标为已读吗？', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/message/read-all', 'PUT', null, function () {
                    layer.msg('已全部标为已读', { icon: 1, time: 1500 });
                    loadMessageList();
                    loadUnreadCount();
                });
            });
        });

        // 筛选切换
        $('#filterGroup button').on('click', function () {
            $('#filterGroup button').removeClass('layui-btn-active');
            $(this).addClass('layui-btn-active');
            currentIsRead = $(this).data('isread');
            currentPage = 1;
            loadMessageList();
        });

        // 铃铛点击跳转全部
        $('#msgBell').on('click', function () {
            $('#filterGroup button[data-isread=""]').click();
        });

        // 初始化加载
        loadMessageList();
        loadUnreadCount();

        // 自动刷新未读数（每30秒）
        setInterval(function () {
            loadUnreadCount();
        }, 30000);
    });
</script>
