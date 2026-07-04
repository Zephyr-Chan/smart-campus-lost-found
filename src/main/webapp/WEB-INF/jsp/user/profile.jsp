<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="layui-row layui-col-space20">
        <!-- 左侧：头像+基本信息 -->
        <div class="layui-col-md4">
            <div class="custom-card" style="text-align:center;">
                <div style="position:relative;display:inline-block;margin-bottom:16px;">
                    <img id="avatarImg" src="https://cdn.jsdelivr.net/gh/layui/layui@2.9.x/src/css/modules/layer/default/avatar.png"
                         style="width:120px;height:120px;border-radius:50%;border:4px solid var(--accent-light);object-fit:cover;"
                         alt="头像">
                    <button type="button" id="btnChangeAvatar"
                        style="position:absolute;bottom:0;right:0;width:36px;height:36px;border-radius:50%;background:var(--accent);color:#fff;border:none;cursor:pointer;box-shadow:0 2px 6px rgba(0,0,0,0.3);">
                        <i class="layui-icon layui-icon-camera"></i>
                    </button>
                    <input type="file" id="avatarInput" accept="image/*" style="display:none;">
                </div>
                <h3 id="profileUsername" style="font-size:18px;color:var(--text-primary);">用户名</h3>
                <p id="profileRealName" style="color:var(--text-tertiary);margin-top:4px;">真实姓名</p>
                <div style="margin-top:12px;">
                    <span class="status-tag status-approved" id="profileRole">学生</span>
                    <span class="status-tag status-matched" id="profileStatus">正常</span>
                </div>
                <div style="margin-top:16px;padding-top:16px;border-top:1px solid var(--border-subtle);">
                    <p style="color:var(--text-tertiary);font-size:13px;margin-bottom:4px;">注册时间</p>
                    <p id="profileCreateTime" style="font-size:14px;">-</p>
                </div>
            </div>
            <!-- 信用积分 -->
            <div class="custom-card" id="creditCard" style="margin-top:20px;">
                <div class="card-title">
                    <span><i class="layui-icon layui-icon-diamond"></i>信用积分</span>
                </div>
                <div id="creditInfo">
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-loading"></i>
                        <p>加载中...</p>
                    </div>
                </div>
            </div>
        </div>

        <!-- 右侧：编辑表单 -->
        <div class="layui-col-md8">
            <div class="custom-card">
                <div class="card-title">
                    <span><i class="layui-icon layui-icon-edit"></i>编辑个人信息</span>
                </div>
                <form class="layui-form" id="profileForm" lay-filter="profileForm">
                    <div class="layui-form-item">
                        <label class="layui-form-label">用户名</label>
                        <div class="layui-input-block">
                            <input type="text" name="username" class="layui-input" disabled style="background:var(--gray-100);">
                        </div>
                    </div>
                    <div class="layui-form-item">
                        <label class="layui-form-label">真实姓名</label>
                        <div class="layui-input-block">
                            <input type="text" name="realName" lay-verify="required"
                                   placeholder="请输入真实姓名" class="layui-input">
                        </div>
                    </div>
                    <div class="layui-form-item">
                        <label class="layui-form-label">手机号</label>
                        <div class="layui-input-block">
                            <input type="tel" name="phone" lay-verify="required|phone"
                                   placeholder="请输入手机号" class="layui-input">
                        </div>
                    </div>
                    <div class="layui-form-item">
                        <label class="layui-form-label">邮箱</label>
                        <div class="layui-input-block">
                            <input type="email" name="email" lay-verify="required|email"
                                   placeholder="请输入邮箱" class="layui-input">
                        </div>
                    </div>
                    <div class="layui-form-item">
                        <div class="layui-input-block">
                            <button type="submit" class="layui-btn layui-btn-normal" lay-submit lay-filter="doSave">
                                <i class="layui-icon layui-icon-ok"></i> 保存修改
                            </button>
                            <button type="reset" class="layui-btn layui-btn-primary">
                                <i class="layui-icon layui-icon-refresh"></i> 重置
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['form', 'layer', 'upload'], function () {
        var form = layui.form;
        var layer = layui.layer;
        var upload = layui.upload;

        // 检查登录状态
        if (!checkLogin(true)) return;

        var currentUser = getCurrentUser();

        // 加载用户信息
        function loadUserInfo() {
            ajaxRequest('/api/user/info', 'GET', null, function (data) {
                // 渲染左侧信息
                $('#profileUsername').text(data.username);
                $('#profileRealName').text(data.realName || '未设置');
                $('#profileRole').text(getRoleText(data.role));
                $('#profileStatus').text(getUserStatusText(data.status));
                $('#profileCreateTime').text(formatDate(data.createTime));

                if (data.avatar) {
                    $('#avatarImg').attr('src', getImageUrl(data.avatar));
                }

                // 填充表单
                form.val('profileForm', {
                    username: data.username,
                    realName: data.realName,
                    phone: data.phone,
                    email: data.email
                });
            });
        }

        // 头像上传
        $('#btnChangeAvatar').on('click', function () {
            $('#avatarInput').click();
        });

        $('#avatarInput').on('change', function (e) {
            var file = e.target.files[0];
            if (!file) return;

            // 预览
            var reader = new FileReader();
            reader.onload = function (ev) {
                $('#avatarImg').attr('src', ev.target.result);
            };
            reader.readAsDataURL(file);

            // 上传
            var formData = new FormData();
            formData.append('file', file);

            var loadIndex = layer.load(2);
            ajaxUpload('/api/user/avatar', formData, function (data) {
                layer.close(loadIndex);
                layer.msg('头像更新成功', { icon: 1 });
                // 更新localStorage中的用户信息
                if (currentUser) {
                    currentUser.avatar = data;
                    localStorage.setItem('userInfo', JSON.stringify(currentUser));
                }
            }, function () {
                layer.close(loadIndex);
            });
        });

        // 监听保存
        form.on('submit(doSave)', function (data) {
            var formData = data.field;
            var btnElem = $(data.elem);
            btnElem.attr('disabled', true).text('保存中...');

            $.ajax({
                url: '/api/user/profile',
                type: 'PUT',
                contentType: 'application/json;charset=UTF-8',
                headers: { 'Authorization': 'Bearer ' + getToken() },
                data: JSON.stringify({
                    username: formData.username,
                    realName: formData.realName,
                    phone: formData.phone,
                    email: formData.email
                }),
                success: function (res) {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-ok"></i> 保存修改');
                    if (res.code === 200) {
                        layer.msg('保存成功', { icon: 1, time: 1500 });
                        // 更新localStorage
                        if (currentUser) {
                            currentUser.realName = formData.realName;
                            currentUser.phone = formData.phone;
                            currentUser.email = formData.email;
                            localStorage.setItem('userInfo', JSON.stringify(currentUser));
                        }
                        $('#profileRealName').text(formData.realName || '未设置');
                        $('#navUsername').text(formData.realName || formData.username || '用户');
                        loadUserInfo();
                    } else {
                        layer.msg(res.msg || '保存失败', { icon: 2 });
                    }
                },
                error: function () {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-ok"></i> 保存修改');
                    layer.msg('网络错误', { icon: 2 });
                }
            });
            return false;
        });

        // 加载信用积分
        function loadCredit() {
            ajaxRequest('/api/credit/my', 'GET', null, function (data) {
                var html = '';
                html += '<div style="text-align:center;padding:8px 0 16px;">';
                html += '  <div style="font-family:Poppins,sans-serif;font-size:36px;font-weight:700;color:var(--accent);letter-spacing:-0.02em;">' + (data.creditScore || 0) + '</div>';
                html += '  <div style="font-size:12px;color:var(--text-tertiary);margin-top:4px;">当前积分</div>';
                html += '</div>';
                html += '<div style="display:flex;justify-content:space-around;padding:12px 0;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle);">';
                html += '  <div style="text-align:center;">';
                html += '    <div style="font-size:18px;font-weight:600;color:var(--text-primary);">' + (data.rank || '-') + '</div>';
                html += '    <div style="font-size:11px;color:var(--text-tertiary);">排名</div>';
                html += '  </div>';
                html += '  <div style="text-align:center;">';
                html += '    <div style="font-size:18px;font-weight:600;color:var(--text-primary);">' + (data.totalUsers || '-') + '</div>';
                html += '    <div style="font-size:11px;color:var(--text-tertiary);">总用户数</div>';
                html += '  </div>';
                html += '</div>';
                html += '<div id="creditLogList" style="margin-top:12px;"></div>';
                $('#creditInfo').html(html);
                loadCreditLogs();
            }, function () {
                $('#creditInfo').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无积分信息</p></div>');
            });
        }

        // 加载积分变动日志
        function loadCreditLogs() {
            ajaxRequest('/api/credit/log?page=1&size=5', 'GET', null, function (data) {
                var records = data.records || data.list || [];
                if (records.length === 0) {
                    $('#creditLogList').html('<p style="text-align:center;font-size:12px;color:var(--text-tertiary);padding:12px 0;">暂无积分变动记录</p>');
                    return;
                }
                var html = '<div style="font-size:12px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.5px;">最近变动</div>';
                records.forEach(function (log) {
                    var scoreColor = log.changeScore > 0 ? 'var(--success)' : 'var(--danger)';
                    var scoreSign = log.changeScore > 0 ? '+' : '';
                    html += '<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border-subtle);">';
                    html += '  <div>';
                    html += '    <span style="font-size:13px;color:var(--text-primary);">' + escapeHtml(log.reason || '积分变动') + '</span>';
                    html += '    <span style="font-size:11px;color:var(--text-tertiary);margin-left:6px;">' + formatDate(log.createTime) + '</span>';
                    html += '  </div>';
                    html += '  <span style="font-size:14px;font-weight:600;color:' + scoreColor + ';">' + scoreSign + log.changeScore + '</span>';
                    html += '</div>';
                });
                $('#creditLogList').html(html);
            });
        }

        // 加载
        loadUserInfo();
        loadCredit();
    });
</script>
