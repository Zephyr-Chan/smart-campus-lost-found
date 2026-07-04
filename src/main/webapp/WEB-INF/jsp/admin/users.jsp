<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-username"></i>用户管理</span>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar" style="margin-bottom:16px;">
            <div class="layui-inline">
                <input type="text" id="searchKeyword" placeholder="用户名/姓名/手机号/邮箱" class="layui-input" style="width:220px;">
            </div>
            <button class="layui-btn layui-btn-normal" id="btnSearch"><i class="layui-icon layui-icon-search"></i> 搜索</button>
            <button class="layui-btn layui-btn-primary" id="btnResetSearch"><i class="layui-icon layui-icon-refresh"></i> 重置</button>
        </div>

        <table class="layui-table" id="userTable">
            <thead>
                <tr>
                    <th width="60">ID</th>
                    <th>用户名</th>
                    <th>真实姓名</th>
                    <th>手机号</th>
                    <th>邮箱</th>
                    <th width="80">角色</th>
                    <th width="80">状态</th>
                    <th width="120">操作</th>
                </tr>
            </thead>
            <tbody id="userList">
                <tr><td colspan="8" style="text-align:center;padding:40px;color:var(--text-tertiary);">加载中...</td></tr>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer'], function () {
        var layer = layui.layer;

        // 检查登录和管理员权限
        if (!checkLogin(true)) return;
        if (!isAdmin()) {
            layer.msg('无管理员权限', { icon: 2, time: 1500 }, function () {
                window.location.href = '/';
            });
            return;
        }

        // 加载用户列表
        function loadUserList(keyword) {
            var url = '/api/user/list';
            if (keyword) {
                url += '?keyword=' + encodeURIComponent(keyword);
            }
            $('#userList').html('<tr><td colspan="8" style="text-align:center;padding:40px;color:#94A3B8;">加载中...</td></tr>');
            ajaxRequest(url, 'GET', null, function (data) {
                renderUserList(data);
            }, function () {
                $('#userList').html('<tr><td colspan="8" style="text-align:center;padding:40px;color:#94A3B8;">加载失败</td></tr>');
            });
        }

        // 渲染用户列表
        function renderUserList(list) {
            if (!list || list.length === 0) {
                $('#userList').html('<tr><td colspan="8" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-surprised" style="font-size:36px;display:block;margin-bottom:8px;"></i>暂无用户数据</td></tr>');
                return;
            }
            var html = '';
            list.forEach(function (user) {
                var statusText = getUserStatusText(user.status);
                var statusClass = user.status === 1 ? 'status-approved' : 'status-closed';
                var btnText = user.status === 1 ? '禁用' : '启用';
                var btnClass = user.status === 1 ? 'layui-btn-danger' : 'layui-btn-normal';
                var btnDisabled = user.role === 1 ? ' disabled' : '';
                var btnOnClick = user.role === 1 ? '' : 'onclick="toggleStatus(' + user.id + ',' + user.status + ')"';

                html += '<tr>';
                html += '  <td>' + user.id + '</td>';
                html += '  <td style="font-weight:600;">' + escapeText(user.username) + '</td>';
                html += '  <td>' + escapeText(user.realName || '-') + '</td>';
                html += '  <td>' + escapeText(user.phone || '-') + '</td>';
                html += '  <td>' + escapeText(user.email || '-') + '</td>';
                html += '  <td><span class="status-tag ' + (user.role === 1 ? 'status-matched' : 'status-pending') + '">' + getRoleText(user.role) + '</span></td>';
                html += '  <td><span class="status-tag ' + statusClass + '">' + statusText + '</span></td>';
                html += '  <td><button class="layui-btn layui-btn-sm ' + btnClass + '"' + btnDisabled + ' ' + btnOnClick + '>' + btnText + '</button></td>';
                html += '</tr>';
            });
            $('#userList').html(html);
        }

        // 文本转义
        function escapeText(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }

        // 切换用户状态
        window.toggleStatus = function (userId, currentStatus) {
            var newStatus = currentStatus === 1 ? 0 : 1;
            var action = newStatus === 0 ? '禁用' : '启用';
            layer.confirm('确定要' + action + '该用户吗？', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                $.ajax({
                    url: '/api/user/status/' + userId,
                    type: 'PUT',
                    headers: { 'Authorization': 'Bearer ' + getToken() },
                    data: { status: newStatus },
                    success: function (res) {
                        if (res.code === 200) {
                            layer.msg(action + '成功', { icon: 1, time: 1500 });
                            loadUserList($('#searchKeyword').val());
                        } else {
                            layer.msg(res.msg || '操作失败', { icon: 2 });
                        }
                    },
                    error: function () {
                        layer.msg('网络错误', { icon: 2 });
                    }
                });
            });
        };

        // 搜索
        $('#btnSearch').on('click', function () {
            loadUserList($('#searchKeyword').val());
        });

        $('#btnResetSearch').on('click', function () {
            $('#searchKeyword').val('');
            loadUserList();
        });

        // 回车搜索
        $('#searchKeyword').on('keydown', function (e) {
            if (e.keyCode === 13) {
                loadUserList($('#searchKeyword').val());
            }
        });

        // 加载
        loadUserList();
    });
</script>
