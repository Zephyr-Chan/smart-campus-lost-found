<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-notice"></i>公告管理</span>
            <button class="layui-btn layui-btn-normal layui-btn-sm" id="btnAdd">
                <i class="layui-icon layui-icon-add-1"></i> 新增公告
            </button>
        </div>
        <table class="layui-table" id="annTable">
            <thead>
                <tr>
                    <th width="60">ID</th>
                    <th>标题</th>
                    <th>内容</th>
                    <th width="100">状态</th>
                    <th width="170">创建时间</th>
                    <th width="170">操作</th>
                </tr>
            </thead>
            <tbody id="annList">
                <tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-tertiary);">加载中...</td></tr>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'form'], function () {
        var layer = layui.layer;
        var form = layui.form;

        // 检查登录和管理员权限
        if (!checkLogin(true)) return;
        if (!isAdmin()) {
            layer.msg('无管理员权限', { icon: 2, time: 1500 }, function () {
                window.location.href = '/';
            });
            return;
        }

        // 加载公告列表（H-2修复：管理员接口返回全部状态）
        function loadList() {
            ajaxRequest('/api/announcement/admin/list', 'GET', null, function (data) {
                renderList(data);
            }, function () {
                $('#annList').html('<tr><td colspan="6" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-crying" style="font-size:36px;display:block;margin-bottom:8px;"></i>加载失败，请刷新重试</td></tr>');
            });
        }

        // 公告数据缓存（供编辑使用）
        var annDataMap = {};

        // 渲染列表
        function renderList(list) {
            if (!list || list.length === 0) {
                $('#annList').html('<tr><td colspan="6" style="text-align:center;padding:40px;color:#94A3B8;"><i class="layui-icon layui-icon-face-surprised" style="font-size:36px;display:block;margin-bottom:8px;"></i>暂无公告</td></tr>');
                return;
            }
            var html = '';
            list.forEach(function (item) {
                annDataMap[item.id] = item;
                var statusBadge = '<span class="status-tag status-approved">已发布</span>';
                if (item.status === 0) statusBadge = '<span class="status-tag status-pending">草稿</span>';
                if (item.status === 2) statusBadge = '<span class="status-tag status-closed">已下架</span>';

                // H-3修复：使用安全的属性传递方式，避免XSS
                html += '<tr>';
                html += '  <td>' + item.id + '</td>';
                html += '  <td style="font-weight:600;">' + escapeHtmlContent(item.title) + '</td>';
                html += '  <td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#475569;">' + escapeHtmlContent(item.content || '-') + '</td>';
                html += '  <td>' + statusBadge + '</td>';
                html += '  <td>' + formatDate(item.createTime) + '</td>';
                html += '  <td>';
                html += '    <button class="layui-btn layui-btn-sm layui-btn-normal" onclick="editAnn(' + item.id + ')"><i class="layui-icon layui-icon-edit"></i> 编辑</button>';
                html += '    <button class="layui-btn layui-btn-sm layui-btn-danger" onclick="deleteAnn(' + item.id + ')"><i class="layui-icon layui-icon-delete"></i> 删除</button>';
                html += '  </td>';
                html += '</tr>';
            });
            $('#annList').html(html);
        }

        // HTML转义委托到全局escapeHtml函数（common.js）
        var escapeHtmlContent = escapeHtml;

        // 新增公告
        $('#btnAdd').on('click', function () {
            openEditForm(null);
        });

        // 编辑公告（H-3修复：通过ID从列表数据获取，避免XSS）
        window.editAnn = function (id) {
            var item = annDataMap[id];
            if (!item) {
                layer.msg('数据加载中，请稍后', { icon: 2 });
                return;
            }
            openEditForm(item);
        };

        // 打开编辑弹窗
        function openEditForm(data) {
            var isEdit = data !== null;
            var html = '';
            html += '<form class="layui-form" id="annForm" lay-filter="annForm" style="padding:20px;">';
            html += '  <div class="layui-form-item">';
            html += '    <label class="layui-form-label">公告标题</label>';
            html += '    <div class="layui-input-block">';
            html += '      <input type="text" name="title" lay-verify="required" placeholder="请输入公告标题" class="layui-input" value="' + (isEdit ? escapeHtmlContent(data.title) : '') + '">';
            html += '    </div>';
            html += '  </div>';
            html += '  <div class="layui-form-item">';
            html += '    <label class="layui-form-label">公告内容</label>';
            html += '    <div class="layui-input-block">';
            html += '      <textarea name="content" lay-verify="required" placeholder="请输入公告内容" class="layui-textarea" style="height:150px;">' + (isEdit ? escapeHtmlContent(data.content) : '') + '</textarea>';
            html += '    </div>';
            html += '  </div>';
            html += '  <div class="layui-form-item">';
            html += '    <label class="layui-form-label">状态</label>';
            html += '    <div class="layui-input-block">';
            html += '      <select name="status">';
            html += '        <option value="1"' + (isEdit && data.status === 1 ? ' selected' : '') + '>已发布</option>';
            html += '        <option value="0"' + (isEdit && data.status === 0 ? ' selected' : '') + '>草稿</option>';
            html += '        <option value="2"' + (isEdit && data.status === 2 ? ' selected' : '') + '>已下架</option>';
            html += '      </select>';
            html += '    </div>';
            html += '  </div>';
            html += '  <div class="layui-form-item" style="text-align:center;">';
            html += '    <button type="submit" class="layui-btn layui-btn-normal" lay-submit lay-filter="doSubmit">' + (isEdit ? '保存修改' : '确认新增') + '</button>';
            html += '    <button type="button" class="layui-btn layui-btn-primary" onclick="layer.closeAll()">取消</button>';
            html += '  </div>';
            html += '</form>';

            layer.open({
                type: 1,
                title: isEdit ? '编辑公告' : '新增公告',
                area: ['600px', '420px'],
                content: html,
                success: function () {
                    form.render('select');
                    form.on('submit(doSubmit)', function (formData) {
                        var submitData = {
                            title: formData.field.title,
                            content: formData.field.content,
                            status: parseInt(formData.field.status)
                        };
                        if (isEdit) {
                            submitData.id = data.id;
                            ajaxRequest('/api/announcement', 'PUT', submitData, function () {
                                layer.msg('修改成功', { icon: 1, time: 1500 });
                                layer.closeAll();
                                loadList();
                            });
                        } else {
                            ajaxRequest('/api/announcement', 'POST', submitData, function () {
                                layer.msg('新增成功', { icon: 1, time: 1500 });
                                layer.closeAll();
                                loadList();
                            });
                        }
                        return false;
                    });
                }
            });
        }

        // 删除公告
        window.deleteAnn = function (id) {
            layer.confirm('确定删除此公告吗？删除后不可恢复。', { icon: 3, title: '确认删除' }, function (index) {
                layer.close(index);
                ajaxRequest('/api/announcement/' + id, 'DELETE', null, function () {
                    layer.msg('删除成功', { icon: 1, time: 1500 });
                    loadList();
                });
            });
        };

        // 加载
        loadList();
    });
</script>
