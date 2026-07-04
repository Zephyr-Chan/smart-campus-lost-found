<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- 使用JSTL fmt标签格式化当前年份 --%>
<jsp:useBean id="now" class="java.util.Date" />
<fmt:formatDate value="${now}" pattern="yyyy" var="currentYear" />
<c:set var="techStack" value="Spring Boot 2.7 + MyBatis-Plus + JSP + layui + ECharts" />
<!-- 页脚 -->
<div class="site-footer">
    <div class="layui-container">
        <p>Copyright &copy; ${currentYear} 校园失物招领系统 | 基于${techStack}</p>
        <p style="margin-top:4px;">技术支持：智能TF-IDF匹配 &middot; JWT认证 &middot; WebSocket实时通知 &middot; Cookie状态管理</p>
    </div>
</div>

<!-- jQuery -->
<script src="https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js"></script>
<!-- layui JS -->
<script src="https://cdn.jsdelivr.net/npm/layui@2.9.21/dist/layui.js"></script>
<!-- 公共JS（带版本号防缓存） -->
<script src="${ctx}/static/js/common.js?v=7.0"></script>
<script>
    // 初始化layui模块
    layui.use(['layer', 'form', 'laydate', 'upload', 'table', 'laypage', 'element'], function () {
        window.layer = layui.layer;
        window.form = layui.form;
        window.laydate = layui.laydate;
        window.upload = layui.upload;
        window.table = layui.table;
        window.laypage = layui.laypage;
        window.element = layui.element;

        // 导航栏根据登录状态显示不同菜单
        (function initNav() {
            var token = getToken();
            var user = getCurrentUser();
            if (token && user) {
                // 已登录
                $('#navLogin').hide();
                $('#navRegister').hide();
                $('#navUser').show();
                $('#navPublishLost').show();
                $('#navMessage').show();
                $('#navFavorite').show();
                $('#navUsername').text(user.realName || user.username || '用户');
                // 管理员菜单
                if (user.role === 1) {
                    $('#navAdmin').show();
                }
                // 初始化WebSocket连接（带token认证）
                initWebSocket(user.id, token);
            } else {
                // 未登录
                $('#navLogin').show();
                $('#navRegister').show();
                $('#navUser').hide();
                $('#navPublishLost').hide();
                $('#navMessage').hide();
                $('#navFavorite').hide();
                $('#navAdmin').hide();
            }

            // 导航高亮
            var path = window.location.pathname;
            $('.nav-header .layui-nav .layui-nav-item').each(function () {
                var $a = $(this).children('a').first();
                var href = $a.attr('href');
                if (href && href !== 'javascript:;' && path === href) {
                    $(this).addClass('layui-this');
                }
            });
        })();

        // 汉堡菜单切换（移动端）
        $('#navToggle').on('click', function () {
            $('#mainNav').toggleClass('nav-open');
            $('.nav-right-group').toggleClass('nav-open');
        });

        // WebSocket连接（带token认证）
        function initWebSocket(userId, token) {
            var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            var wsUrl = protocol + '//' + window.location.host + '/ws/' + userId + '?token=' + token;
            try {
                var socket = new WebSocket(wsUrl);
                socket.onmessage = function (event) {
                    try {
                        var msg = JSON.parse(event.data);
                        if (msg.type === 'match') {
                            layer.msg('发现匹配物品：' + msg.title + '（相似度：' +
                                    (parseFloat(msg.score) * 100).toFixed(1) + '%）', {
                                icon: 1, time: 8000, area: ['360px', 'auto']
                            });
                        } else if (msg.type === 'claim') {
                            if (msg.action === 'new') {
                                layer.msg('收到新的认领申请', { icon: 1, time: 5000 });
                            } else if (msg.action === 'approved') {
                                layer.msg('您的认领申请已通过', { icon: 1, time: 5000 });
                            } else if (msg.action === 'rejected') {
                                layer.msg('您的认领申请被拒绝', { icon: 2, time: 5000 });
                            }
                        }
                    } catch (e) {
                        console.log('WebSocket消息解析失败:', e);
                    }
                };
                socket.onclose = function () {
                    console.log('WebSocket连接已关闭');
                };
                socket.onerror = function () {
                    console.log('WebSocket连接失败');
                };
            } catch (e) {
                console.log('WebSocket初始化失败:', e);
            }
        }
    });
</script>
</body>
</html>
