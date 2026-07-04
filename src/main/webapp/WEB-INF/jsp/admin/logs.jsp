<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-log"></i>操作日志</span>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar" style="margin-bottom:16px;">
            <div class="layui-inline">
                <input type="text" id="searchKeyword" placeholder="操作描述/用户名" class="layui-input" style="width:220px;">
            </div>
            <button class="layui-btn layui-btn-normal" id="btnSearch"><i class="layui-icon layui-icon-search"></i> 搜索</button>
            <button class="layui-btn layui-btn-primary" id="btnResetSearch"><i class="layui-icon layui-icon-refresh"></i> 重置</button>
        </div>

        <!-- 日志表格 -->
        <table class="layui-table" id="logTable">
            <thead>
                <tr>
                    <th width="60">ID</th>
                    <th width="120">操作用户</th>
                    <th>操作描述</th>
                    <th>请求方法</th>
                    <th width="120">IP地址</th>
                    <th width="170">操作时间</th>
                </tr>
            </thead>
            <tbody id="logList">
                <tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-tertiary);">加载中...</td></tr>
            </tbody>
        </table>

        <div style="text-align:center;margin-top:16px;color:var(--text-tertiary);font-size:13px;">
            最多显示最近200条记录
        </div>
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

        // 加载日志列表
        function loadLogList(keyword) {
            var url = '/api/log/list';
            if (keyword) {
                url += '?keyword=' + encodeURIComponent(keyword);
            }
            $('#logList').html('<tr><td colspan="6" style="text-align:center;padding:40px;color:#94A3B8;">加载中...</td></tr>');
            ajaxRequest(url, 'GET', null, function (data) {
                renderLogList(data);
            }, function () {
                $('#logList').html('<tr><td colspan="6" style="text-align:center;padding:40px;color:#94A3B8;">加载失败</td></tr>');
            });
        }

        // 渲染日志列表
        function renderLogList(list) {
            if (!list || list.length === 0) {
                $('#logList').html('<tr><td colspan="6" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-surprised" style="font-size:36px;display:block;margin-bottom:8px;"></i>暂无操作日志</td></tr>');
                return;
            }
            var html = '';
            list.forEach(function (log) {
                html += '<tr>';
                html += '  <td>' + log.id + '</td>';
                html += '  <td>' + escapeText(log.username || '-') + '</td>';
                html += '  <td>' + escapeText(log.operation || '-') + '</td>';
                html += '  <td style="color:#475569;font-size:12px;">' + escapeText(log.method || '-') + '</td>';
                html += '  <td>' + escapeText(log.ip || '-') + '</td>';
                html += '  <td>' + formatDate(log.createTime) + '</td>';
                html += '</tr>';
            });
            $('#logList').html(html);
        }

        // 文本转义
        function escapeText(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }

        // 搜索
        $('#btnSearch').on('click', function () {
            loadLogList($('#searchKeyword').val());
        });

        $('#btnResetSearch').on('click', function () {
            $('#searchKeyword').val('');
            loadLogList();
        });

        $('#searchKeyword').on('keydown', function (e) {
            if (e.keyCode === 13) {
                loadLogList($('#searchKeyword').val());
            }
        });

        // 加载
        loadLogList();
    });
</script>
