<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div class="custom-card" style="max-width:800px;margin:0 auto;">
        <div class="card-title">
            <span><i class="layui-icon layui-icon-add-1"></i>发布拾到物品</span>
        </div>

        <form class="layui-form" id="publishForm" lay-filter="publishForm">
            <div class="layui-form-item">
                <label class="layui-form-label">物品标题</label>
                <div class="layui-input-block">
                    <input type="text" name="title" required lay-verify="required"
                           placeholder="请输入物品标题（如：黑色华为手机）" class="layui-input">
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">物品分类</label>
                <div class="layui-input-block">
                    <select name="category" lay-verify="required">
                        <option value="">请选择分类</option>
                        <option value="electronics">电子产品</option>
                        <option value="certificate">证件</option>
                        <option value="book">书籍</option>
                        <option value="clothing">衣物</option>
                        <option value="other">其他</option>
                    </select>
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">拾取地点</label>
                <div class="layui-input-block">
                    <input type="text" name="location" required lay-verify="required"
                           placeholder="请输入拾取地点（如：图书馆三楼）" class="layui-input">
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">拾取时间</label>
                <div class="layui-input-block">
                    <input type="text" name="eventTime" id="eventTime" required lay-verify="required"
                           placeholder="请选择拾取时间" class="layui-input" readonly>
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">联系方式</label>
                <div class="layui-input-block">
                    <input type="text" name="contactInfo"
                           placeholder="请输入联系方式（手机号/微信号）" class="layui-input">
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">详细描述</label>
                <div class="layui-input-block">
                    <textarea name="description" required lay-verify="required"
                              placeholder="请详细描述物品特征（颜色、品牌、型号、特殊标记等）"
                              class="layui-textarea" style="height:120px;"></textarea>
                </div>
            </div>

            <div class="layui-form-item">
                <label class="layui-form-label">物品图片</label>
                <div class="layui-input-block">
                    <div class="layui-upload" id="uploadArea">
                        <button type="button" class="layui-btn layui-btn-normal" id="btnUpload">
                            <i class="layui-icon layui-icon-upload"></i> 选择图片
                        </button>
                        <span style="color:var(--text-tertiary);margin-left:10px;">最多上传5张，支持jpg/png格式</span>
                        <blockquote class="layui-elem-quote layui-quote-nm" style="margin-top:10px;padding:10px;">
                            <div class="layui-upload-list" id="imagePreview"></div>
                        </blockquote>
                    </div>
                </div>
            </div>

            <div class="layui-form-item">
                <div class="layui-input-block">
                    <button type="submit" class="layui-btn layui-btn-normal" lay-submit lay-filter="doPublish">
                        <i class="layui-icon layui-icon-ok"></i> 立即发布
                    </button>
                    <button type="button" class="layui-btn layui-btn-primary" id="btnReset">
                        <i class="layui-icon layui-icon-refresh"></i> 重置
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['form', 'layer', 'laydate', 'upload'], function () {
        var form = layui.form;
        var layer = layui.layer;
        var laydate = layui.laydate;
        var upload = layui.upload;

        // 检查登录状态
        if (!checkLogin(true)) return;

        // 日期时间选择器
        laydate.render({
            elem: '#eventTime',
            type: 'datetime',
            format: 'yyyy-MM-dd HH:mm:ss',
            max: 0
        });

        // 已上传图片列表
        var uploadedImages = [];

        // 图片上传
        var uploadInst = upload.render({
            elem: '#btnUpload',
            url: '/api/file/upload',
            headers: { 'Authorization': 'Bearer ' + getToken() },
            accept: 'images',
            acceptMime: 'image/*',
            multiple: true,
            number: 5,
            before: function () {
                if (uploadedImages.length >= 5) {
                    layer.msg('最多只能上传5张图片', { icon: 2 });
                    return false;
                }
            },
            done: function (res) {
                if (res.code === 200) {
                    uploadedImages.push(res.data);
                    renderPreview();
                } else {
                    layer.msg(res.msg || '上传失败', { icon: 2 });
                }
            },
            error: function () {
                layer.msg('上传失败，请稍后重试', { icon: 2 });
            }
        });

        // 渲染预览图
        function renderPreview() {
            var html = '';
            uploadedImages.forEach(function (url, index) {
                html += '<div style="display:inline-block;margin-right:10px;position:relative;">';
                html += '  <img src="' + getImageUrl(url) + '" style="width:100px;height:100px;object-fit:cover;border-radius:4px;border:1px solid var(--border);">';
                html += '  <button type="button" onclick="removeImage(' + index + ')" style="position:absolute;top:-6px;right:-6px;width:20px;height:20px;border-radius:50%;background:#DC2626;color:#fff;border:none;cursor:pointer;line-height:18px;font-size:12px;">x</button>';
                html += '</div>';
            });
            $('#imagePreview').html(html);
        }

        // 删除图片
        window.removeImage = function (index) {
            uploadedImages.splice(index, 1);
            renderPreview();
        };

        // H-4修复：重置按钮同时清空已上传图片
        $('#btnReset').on('click', function () {
            $('#publishForm')[0].reset();
            uploadedImages = [];
            renderPreview();
            form.render();
        });

        // 监听提交
        form.on('submit(doPublish)', function (data) {
            var formData = data.field;
            var btnElem = $(data.elem);
            btnElem.attr('disabled', true).text('发布中...');

            var submitData = {
                title: formData.title,
                category: formData.category,
                location: formData.location,
                eventTime: formData.eventTime,
                contactInfo: formData.contactInfo,
                description: formData.description,
                images: uploadedImages.join(',')
            };

            $.ajax({
                url: '/api/found/publish',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                headers: { 'Authorization': 'Bearer ' + getToken() },
                data: JSON.stringify(submitData),
                success: function (res) {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-ok"></i> 立即发布');
                    if (res.code === 200) {
                        layer.msg('发布成功！', { icon: 1, time: 1500 }, function () {
                            window.location.href = '/found/list?fromPublish=1';
                        });
                    } else {
                        layer.msg(res.msg || '发布失败', { icon: 2 });
                    }
                },
                error: function () {
                    btnElem.attr('disabled', false).html('<i class="layui-icon layui-icon-ok"></i> 立即发布');
                    layer.msg('网络错误，请稍后重试', { icon: 2 });
                }
            });
            return false;
        });
    });
</script>
