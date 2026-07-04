<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>注册 - 校园失物招领系统</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/layui@2.9.21/dist/css/layui.css">
    <link rel="stylesheet" href="/static/css/main.css">
    <style>
        body {
            background: var(--gray-50);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
    </style>
</head>
<body>
<div class="auth-wrapper auth-wrapper-wide">
    <div class="auth-card">
        <div class="auth-logo">
            <i class="layui-icon layui-icon-add-circle"></i>
        </div>
        <h2 class="auth-title">注册新账号</h2>
        <p class="auth-subtitle">加入校园失物招领，帮助更多人找回失物</p>

        <form class="layui-form" id="registerForm" lay-filter="registerForm">
            <div class="auth-field">
                <label>用户名</label>
                <input type="text" name="username" required lay-verify="required|username"
                       placeholder="3-20个字符" autocomplete="off" class="layui-input">
            </div>
            <div class="auth-field">
                <label>密码</label>
                <input type="password" name="password" required lay-verify="required|pass"
                       placeholder="6-20个字符" autocomplete="off" class="layui-input">
            </div>
            <div class="auth-field">
                <label>确认密码</label>
                <input type="password" name="confirmPassword" required lay-verify="required|repass"
                       placeholder="请再次输入密码" autocomplete="off" class="layui-input">
            </div>
            <div class="auth-field">
                <label>真实姓名</label>
                <input type="text" name="realName" lay-verify="required"
                       placeholder="请输入真实姓名" autocomplete="off" class="layui-input">
            </div>
            <div class="auth-field">
                <label>手机号码</label>
                <input type="tel" name="phone" lay-verify="required|phone"
                       placeholder="请输入手机号码" autocomplete="off" class="layui-input">
            </div>
            <div class="auth-field">
                <label>电子邮箱</label>
                <input type="email" name="email" lay-verify="required|email"
                       placeholder="请输入电子邮箱" autocomplete="off" class="layui-input">
            </div>
            <button type="submit" class="layui-btn layui-btn-fluid layui-btn-normal auth-btn"
                    lay-submit lay-filter="doRegister">
                <i class="layui-icon layui-icon-add-1"></i> 注 册
            </button>
            <div class="auth-footer">
                已有账号？<a href="/login">返回登录</a>
            </div>
        </form>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/layui@2.9.21/dist/layui.js"></script>
<script src="/static/js/common.js"></script>
<script>
    layui.use(['form', 'layer'], function () {
        var form = layui.form;
        var layer = layui.layer;

        // 自定义验证规则
        form.verify({
            username: function (value) {
                if (value.length < 3 || value.length > 20) {
                    return '用户名长度需在3-20个字符之间';
                }
            },
            pass: function (value) {
                if (value.length < 6 || value.length > 20) {
                    return '密码长度需在6-20个字符之间';
                }
            },
            repass: function (value) {
                var pass = $('input[name="password"]').val();
                if (value !== pass) {
                    return '两次输入的密码不一致';
                }
            }
        });

        // 监听提交
        form.on('submit(doRegister)', function (data) {
            var formData = data.field;
            var btnElem = $(data.elem);
            btnElem.attr('disabled', true).text('注册中...');

            $.ajax({
                url: '/api/user/register',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                data: JSON.stringify({
                    username: formData.username,
                    password: formData.password,
                    realName: formData.realName,
                    phone: formData.phone,
                    email: formData.email
                }),
                success: function (res) {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-add-1"></i> 注 册');
                    if (res.code === 200) {
                        layer.msg('注册成功，请登录', { icon: 1, time: 1500 }, function () {
                            window.location.href = '/login';
                        });
                    } else {
                        layer.msg(res.msg || '注册失败', { icon: 2 });
                    }
                },
                error: function () {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-add-1"></i> 注 册');
                    layer.msg('网络错误，请稍后重试', { icon: 2 });
                }
            });
            return false;
        });
    });
</script>
</body>
</html>
