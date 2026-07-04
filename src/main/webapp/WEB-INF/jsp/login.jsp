<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录 - 校园失物招领系统</title>
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
<div class="auth-wrapper">
    <div class="auth-card">
        <div class="auth-logo">
            <i class="layui-icon layui-icon-search"></i>
        </div>
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-subtitle">登录您的账号继续使用</p>

        <form class="layui-form" id="loginForm" lay-filter="loginForm">
            <div class="auth-field">
                <label>用户名</label>
                <input type="text" name="username" required lay-verify="required"
                       placeholder="请输入用户名" autocomplete="off"
                       class="layui-input">
            </div>
            <div class="auth-field">
                <label>密码</label>
                <input type="password" name="password" required lay-verify="required"
                       placeholder="请输入密码" autocomplete="off"
                       class="layui-input">
            </div>
            <button type="submit" class="layui-btn layui-btn-normal auth-btn"
                    lay-submit lay-filter="doLogin">
                登 录
            </button>
            <div class="auth-footer">
                还没有账号？<a href="/register">立即注册</a>
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

        if (isLogin()) {
            window.location.href = '/';
        }

        form.on('submit(doLogin)', function (data) {
            var formData = data.field;
            var btnElem = $(data.elem);
            btnElem.attr('disabled', true).text('登录中...');

            $.ajax({
                url: '/api/user/login',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                data: JSON.stringify({
                    username: formData.username,
                    password: formData.password
                }),
                success: function (res) {
                    btnElem.attr('disabled', false).text('登 录');
                    if (res.code === 200) {
                        setToken(res.data.token);
                        localStorage.setItem('userInfo', JSON.stringify(res.data.user));
                        layer.msg('登录成功', { icon: 1, time: 1000 }, function () {
                            window.location.href = '/';
                        });
                    } else {
                        layer.msg(res.msg || '登录失败', { icon: 2 });
                    }
                },
                error: function () {
                    btnElem.attr('disabled', false).text('登 录');
                    layer.msg('网络错误，请稍后重试', { icon: 2 });
                }
            });
            return false;
        });
    });
</script>
</body>
</html>
