<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-file"></i>我的认领</span>
        </div>
        <table class="layui-table" id="claimTable">
            <thead>
                <tr>
                    <th>认领ID</th>
                    <th>失物ID</th>
                    <th>拾物ID</th>
                    <th>匹配分数</th>
                    <th>备注</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody id="claimList">
                <tr>
                    <td colspan="8" style="text-align:center;padding:40px;">
                        <div class="loading-spinner" style="margin:0 auto 10px;"></div>
                        正在加载...
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer'], function () {
        var layer = layui.layer;
        var currentUser = getCurrentUser();

        // 检查登录状态
        if (!checkLogin(true)) return;

        // 加载认领列表
        function loadClaimList() {
            ajaxRequest('/api/claim/my', 'GET', null, function (data) {
                renderClaimList(data);
            }, function () {
                $('#claimList').html('<tr><td colspan="8" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-crying"></i>加载失败，请刷新重试</td></tr>');
            });
        }

        // 渲染认领列表
        function renderClaimList(list) {
            if (!list || list.length === 0) {
                $('#claimList').html('<tr><td colspan="8" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-surprised"></i>暂无认领记录</td></tr>');
                return;
            }

            var html = '';
            list.forEach(function (item) {
                var statusText = getClaimStatusText(item.status);
                var statusClass = getClaimStatusClass(item.status);
                var score = item.matchScore ? parseFloat(item.matchScore).toFixed(2) : '-';

                // 判断当前用户是认领人还是被认领人
                var isClaimant = currentUser && item.claimantId === currentUser.id;
                var isRespondent = currentUser && item.respondentId === currentUser.id;

                // 操作按钮
                var actions = '';
                if (isRespondent && item.status === 0) {
                    // 被认领人（拾物发布者）可以审核
                    actions += '<button class="layui-btn layui-btn-sm layui-btn-normal" onclick="approveClaim(' + item.id + ')">通过</button>';
                    actions += '<button class="layui-btn layui-btn-sm layui-btn-danger" onclick="rejectClaim(' + item.id + ')">拒绝</button>';
                }
                if (isClaimant && item.status === 1) {
                    // 认领人（失物发布者）审核通过后可以确认完成
                    actions += '<button class="layui-btn layui-btn-sm" onclick="completeClaim(' + item.id + ')">确认完成</button>';
                }
                if (!actions) {
                    actions = '<span style="color:var(--gray-300);">-</span>';
                }

                html += '<tr>';
                html += '  <td>#' + item.id + '</td>';
                html += '  <td>' + (item.lostItemId ? '<a href="/item/detail?type=lost&id=' + item.lostItemId + '">查看失物</a>' : '-') + '</td>';
                html += '  <td>' + (item.foundItemId ? '<a href="/item/detail?type=found&id=' + item.foundItemId + '">查看拾物</a>' : '-') + '</td>';
                html += '  <td>' + score + '</td>';
                html += '  <td style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + escapeHtml(item.remark || '-') + '</td>';
                html += '  <td><span class="status-tag ' + statusClass + '">' + statusText + '</span></td>';
                html += '  <td>' + formatDate(item.createTime) + '</td>';
                html += '  <td>' + actions + '</td>';
                html += '</tr>';
            });
            $('#claimList').html(html);
        }

        // 审核通过
        window.approveClaim = function (id) {
            layer.confirm('确定通过此认领申请吗？', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/claim/approve/' + id, 'PUT', null, function () {
                    layer.msg('已通过', { icon: 1, time: 1500 });
                    loadClaimList();
                });
            });
        };

        // 审核拒绝
        window.rejectClaim = function (id) {
            layer.confirm('确定拒绝此认领申请吗？', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/claim/reject/' + id, 'PUT', null, function () {
                    layer.msg('已拒绝', { icon: 1, time: 1500 });
                    loadClaimList();
                });
            });
        };

        // 确认完成
        window.completeClaim = function (id) {
            layer.confirm('确认完成此认领吗？完成后物品状态将变为已关闭。', { icon: 3, title: '确认' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/claim/complete/' + id, 'PUT', null, function () {
                    layer.msg('已完成', { icon: 1, time: 1500 });
                    loadClaimList();
                });
            });
        };

        // 加载
        loadClaimList();
    });
</script>
