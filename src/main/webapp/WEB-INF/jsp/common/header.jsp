<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- 使用EL表达式设置上下文路径 --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="appName" value="校园失物招领" />
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${appName} - 智能匹配系统</title>
    <!-- Google Fonts: Inter + Poppins -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@500;600;700&display=swap" rel="stylesheet">
    <!-- layui CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/layui@2.9.21/dist/css/layui.css">
    <!-- 自定义CSS（带版本号防缓存） -->
    <link rel="stylesheet" href="${ctx}/static/css/main.css?v=7.0">
</head>
<body>
<!-- 导航栏 -->
<div class="nav-header">
    <div class="layui-container">
        <a href="${ctx}/" class="nav-logo">
            <span class="logo-icon"><i class="layui-icon layui-icon-search"></i></span>
            <span>${appName}</span>
        </a>
        <button class="nav-toggle" id="navToggle"><i class="layui-icon layui-icon-spread-left"></i></button>
        <!-- 主导航 -->
        <ul class="layui-nav" lay-filter="navMenu" id="mainNav">
            <li class="layui-nav-item"><a href="${ctx}/">首页</a></li>
            <li class="layui-nav-item"><a href="${ctx}/lost/list">丢失</a></li>
            <li class="layui-nav-item"><a href="${ctx}/found/list">拾到</a></li>
            <li class="layui-nav-item" id="navPublishLost" style="display:none;"><a href="${ctx}/lost/publish">发布</a></li>
            <li class="layui-nav-item"><a href="${ctx}/search">搜索</a></li>
            <li class="layui-nav-item" id="navMessage" style="display:none;"><a href="${ctx}/message/list">消息</a></li>
            <li class="layui-nav-item" id="navFavorite" style="display:none;"><a href="${ctx}/favorite/my">收藏</a></li>
            <li class="layui-nav-item"><a href="${ctx}/rank/list">排行</a></li>
            <li class="layui-nav-item"><a href="${ctx}/dashboard">看板</a></li>
            <!-- 管理员菜单 -->
            <li class="layui-nav-item" id="navAdmin" style="display:none;">
                <a href="javascript:;">管理 <i class="layui-icon layui-icon-down" style="font-size:10px;"></i></a>
                <dl class="layui-nav-child">
                    <dd><a href="${ctx}/admin/announcement">公告管理</a></dd>
                    <dd><a href="${ctx}/admin/users">用户管理</a></dd>
                    <dd><a href="${ctx}/admin/logs">操作日志</a></dd>
                </dl>
            </li>
        </ul>
        <!-- 右侧用户区 -->
        <ul class="layui-nav nav-right-group" lay-filter="navRight">
            <!-- 未登录 -->
            <li class="layui-nav-item" id="navLogin">
                <a href="${ctx}/login">登录</a>
            </li>
            <li class="layui-nav-item" id="navRegister">
                <a href="${ctx}/register">注册</a>
            </li>
            <!-- 已登录 -->
            <li class="layui-nav-item" id="navUser" style="display:none;">
                <a href="javascript:;"><span id="navUsername">用户</span> <i class="layui-icon layui-icon-down" style="font-size:10px;"></i></a>
                <dl class="layui-nav-child">
                    <dd><a href="${ctx}/user/profile">个人中心</a></dd>
                    <dd><a href="${ctx}/claim/my">我的认领</a></dd>
                    <dd><a href="javascript:logout();">退出登录</a></dd>
                </dl>
            </li>
        </ul>
    </div>
</div>
