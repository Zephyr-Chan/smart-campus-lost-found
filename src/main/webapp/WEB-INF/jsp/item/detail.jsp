<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="main-container">
    <div id="detailContent" style="max-width:900px;margin:0 auto;">
        <div style="margin-bottom:16px;font-size:13px;color:var(--text-tertiary);">
            <a href="/" style="color:var(--text-secondary);">首页</a>
            <span style="margin:0 6px;">/</span>
            <a href="#" id="breadcrumbList" style="color:var(--text-secondary);">列表</a>
            <span style="margin:0 6px;">/</span>
            <span>详情</span>
        </div>
        <div class="empty-state">
            <div class="loading-spinner" style="margin:0 auto 16px;"></div>
            <p>正在加载详情...</p>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer', 'element'], function () {
        var layer = layui.layer;
        var element = layui.element;

        var itemType = getQueryParam('type');
        var itemId = getQueryParam('id');
        var currentUser = getCurrentUser();

        if (!itemType || !itemId) {
            $('#detailContent').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>参数错误</p></div>');
            return;
        }

        // 加载详情数据
        function loadDetail() {
            var apiUrl = itemType === 'lost' ? '/api/lost/get/' + itemId : '/api/found/get/' + itemId;
            ajaxRequest(apiUrl, 'GET', null, function (data) {
                renderDetail(data);
            }, function () {
                $('#detailContent').html('<div class="empty-state"><i class="layui-icon layui-icon-face-crying"></i><p>物品不存在或已被删除</p></div>');
            });
        }

        // 渲染详情
        function renderDetail(item) {
            var typeLabel = itemType === 'lost' ? '丢失物品' : '拾到物品';
            var typeIcon = itemType === 'lost' ? 'layui-icon-list' : 'layui-icon-template-1';
            var timeLabel = itemType === 'lost' ? '丢失时间' : '拾取时间';
            var publisherLabel = itemType === 'lost' ? '发布者' : '拾取者';

            var images = getAllImages(item.images);
            var imagesHtml = '';
            if (images.length > 0) {
                imagesHtml = '<div class="detail-section"><div class="section-label">物品图片</div><div class="detail-image-gallery">';
                images.forEach(function (url) {
                    var safeUrl = escapeHtml(getImageUrl(url));
                    imagesHtml += '<img src="' + safeUrl + '" onclick="layer.photos({photos:{data:[{src:\'' + safeUrl + '\'}]},anim:5})">';
                });
                imagesHtml += '</div></div>';
            }

            // 判断是否显示操作按钮
            var isOwner = currentUser && item.userId === currentUser.id;
            var actionBtns = '';

            // 丢失物品：所有登录用户均可触发智能匹配和查看匹配结果
            if (itemType === 'lost' && isLogin()) {
                actionBtns += '<button class="layui-btn layui-btn-normal" onclick="startMatch(' + item.id + ', event)"><i class="layui-icon layui-icon-search"></i> 智能匹配</button>';
            }

            if (itemType === 'found' && !isOwner && isLogin()) {
                // 拾到物品且不是自己发布的：显示直接认领按钮
                actionBtns += '<button class="layui-btn layui-btn-warm" onclick="directClaim(' + item.id + ')"><i class="layui-icon layui-icon-ok"></i> 直接认领</button>';
            }

            if (itemType === 'lost' && isLogin()) {
                actionBtns += '<a href="/match/result?lostItemId=' + item.id + '" class="layui-btn layui-btn-primary"><i class="layui-icon layui-icon-chart"></i> 查看匹配结果</a>';
            }

            var html = '';
            // 面包屑导航
            html += '<div style="margin-bottom:16px;font-size:13px;color:var(--text-tertiary);">';
            html += '    <a href="/" style="color:var(--text-secondary);">首页</a>';
            html += '    <span style="margin:0 6px;">/</span>';
            html += '    <a href="#" id="breadcrumbList" style="color:var(--text-secondary);">列表</a>';
            html += '    <span style="margin:0 6px;">/</span>';
            html += '    <span>详情</span>';
            html += '</div>';

            html += '<div class="custom-card">';
            html += '  <div class="card-title">';
            html += '    <span><i class="layui-icon ' + typeIcon + '"></i>' + typeLabel + '详情</span>';
            html += '    <div style="display:flex;align-items:center;gap:12px;">';
            html += '      <span class="status-tag ' + getStatusClass(item.status) + '">' + getStatusText(item.status) + '</span>';
            if (isLogin()) {
                html += '      <button class="layui-btn layui-btn-sm layui-btn-primary" id="btnFavorite" onclick="toggleFavorite(' + item.id + ', \'' + itemType + '\')"><i class="layui-icon layui-icon-star" id="favIcon"></i> <span id="favText">收藏</span></button>';
            }
            html += '    </div>';
            html += '  </div>';
            html += '  <h2 style="font-size:22px;margin-bottom:20px;color:var(--text-primary);">' + escapeHtml(item.title) + '</h2>';

            html += '  <div class="layui-row layui-col-space20">';
            html += '    <div class="layui-col-md6">';
            html += '      <div class="detail-section"><div class="section-label">物品分类</div><div class="section-value">' + getCategoryText(item.category) + '</div></div>';
            html += '    </div>';
            html += '    <div class="layui-col-md6">';
            html += '      <div class="detail-section"><div class="section-label">' + (itemType === 'lost' ? '丢失' : '拾取') + '地点</div><div class="section-value">' + escapeHtml(item.location) + '</div></div>';
            html += '    </div>';
            html += '    <div class="layui-col-md6">';
            html += '      <div class="detail-section"><div class="section-label">' + timeLabel + '</div><div class="section-value">' + formatDate(item.eventTime) + '</div></div>';
            html += '    </div>';
            html += '    <div class="layui-col-md6">';
            html += '      <div class="detail-section"><div class="section-label">' + publisherLabel + '</div><div class="section-value">' + (itemType === 'lost' ? (item.publisherName || '匿名') : (item.finderName || '匿名')) + '</div></div>';
            html += '    </div>';
            if (item.contactInfo) {
                html += '    <div class="layui-col-md6">';
                html += '      <div class="detail-section"><div class="section-label">联系方式</div><div class="section-value">' + escapeHtml(item.contactInfo) + '</div></div>';
                html += '    </div>';
            }
            html += '    <div class="layui-col-md6">';
            html += '      <div class="detail-section"><div class="section-label">发布时间</div><div class="section-value">' + formatDate(item.createTime) + '</div></div>';
            html += '    </div>';
            html += '  </div>';

            html += '  <div class="detail-section" style="margin-top:20px;">';
            html += '    <div class="section-label">详细描述</div>';
            html += '    <div class="section-value" style="line-height:1.8;white-space:pre-wrap;">' + escapeHtml(item.description) + '</div>';
            html += '  </div>';

            html += imagesHtml;

            if (actionBtns) {
                html += '<div style="margin-top:24px;padding-top:20px;border-top:1px solid var(--border-subtle);">' + actionBtns + '</div>';
            }

            html += '</div>';

            // 匹配结果区域（隐藏，点击智能匹配后显示）
            html += '<div class="custom-card" id="matchResultCard" style="display:none;">';
            html += '  <div class="card-title"><span><i class="layui-icon layui-icon-search"></i>智能匹配结果</span></div>';
            html += '  <div id="matchResultList"></div>';
            html += '</div>';

            // 相似物品推荐
            html += '<div class="custom-card" id="similarCard" style="display:none;">';
            html += '  <div class="card-title"><span><i class="layui-icon layui-icon-component"></i>相似物品推荐</span></div>';
            html += '  <div class="item-grid" id="similarList"></div>';
            html += '</div>';

            // 评论区
            html += '<div class="custom-card" id="commentCard">';
            html += '  <div class="card-title"><span><i class="layui-icon layui-icon-dialogue"></i>评论</span></div>';
            html += '  <div id="commentList"><div class="empty-state"><i class="layui-icon layui-icon-loading"></i><p>加载评论中...</p></div></div>';
            if (isLogin()) {
                html += '  <div style="margin-top:16px;padding-top:16px;border-top:1px solid var(--border-subtle);">';
                html += '    <div class="layui-form-item" style="margin-bottom:0;">';
                html += '      <textarea id="commentContent" placeholder="说点什么..." class="layui-textarea" style="border-radius:var(--radius-md);margin-bottom:8px;min-height:60px;"></textarea>';
                html += '      <button class="layui-btn layui-btn-sm layui-btn-normal" onclick="submitComment()"><i class="layui-icon layui-icon-ok"></i> 发表评论</button>';
                html += '    </div>';
                html += '  </div>';
            }
            html += '</div>';

            $('#detailContent').html(html);

            // 设置面包屑列表链接
            $('#breadcrumbList').attr('href', '/' + itemType + '/list').text(typeLabel + '列表');

            // 检查收藏状态
            checkFavorite(item.id, itemType);

            // 加载相似推荐
            ajaxRequest('/api/recommend/similar/' + itemId + '?type=' + itemType + '&limit=4', 'GET', null, function(data) {
                if (data && data.length > 0) {
                    var simHtml = '';
                    data.forEach(function(item) {
                        var img = getFirstImage(item.images);
                        var imgHtml = img
                            ? '<img class="item-card-image" src="' + escapeHtml(getImageUrl(img)) + '" alt="' + escapeHtml(item.title || '') + '">'
                            : '<div class="item-card-image" style="display:flex;align-items:center;justify-content:center;"><i class="layui-icon layui-icon-picture" style="font-size:36px;color:var(--gray-300);"></i></div>';
                        var detailUrl = '/item/detail?type=' + (item.itemType || itemType) + '&id=' + (item.id || item.itemId);
                        simHtml += '<div class="item-card" onclick="window.location.href=\'' + detailUrl + '\'">';
                        simHtml += '  ' + imgHtml;
                        simHtml += '  <div class="item-card-body">';
                        simHtml += '    <div class="item-card-title">' + escapeHtml(item.title || '无标题') + '</div>';
                        simHtml += '    <div class="item-card-desc">' + escapeHtml(truncateStr(item.description || '', 40)) + '</div>';
                        simHtml += '    <div class="item-card-meta"><span class="meta-location"><i class="layui-icon layui-icon-location"></i>' + escapeHtml(truncateStr(item.location || '未知', 12)) + '</span></div>';
                        simHtml += '  </div>';
                        simHtml += '</div>';
                    });
                    $('#similarList').html(simHtml);
                    $('#similarCard').show();
                }
            });

            // 加载评论
            loadComments(itemId, itemType);

            // 记录浏览历史（登录用户）
            if (isLogin()) {
                $.ajax({
                    url: '/api/recommend/view?itemId=' + itemId + '&itemType=' + itemType,
                    type: 'POST',
                    headers: {'Authorization': 'Bearer ' + getToken()},
                    error: function() { /* 静默处理 */ }
                });
            }
        }

        // 智能匹配
        window.startMatch = function (lostItemId, ev) {
            var btn = $(ev.target).closest('button');
            btn.attr('disabled', true).text('匹配中...');
            ajaxRequest('/api/match/' + lostItemId, 'GET', null, function (data) {
                btn.attr('disabled', false).html('<i class="layui-icon layui-icon-search"></i> 智能匹配');
                renderMatchResult(data);
            }, function () {
                btn.attr('disabled', false).html('<i class="layui-icon layui-icon-search"></i> 智能匹配');
            });
        };

        // 渲染匹配结果
        function renderMatchResult(list) {
            if (!list || list.length === 0) {
                $('#matchResultList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无匹配的拾到物品</p></div>');
                $('#matchResultCard').show();
                return;
            }
            var html = '';
            list.forEach(function (item) {
                var score = item.matchScore ? (item.matchScore * 100).toFixed(1) : '0.0';
                var scoreNum = parseFloat(score);
                var scoreColor = scoreNum >= 70 ? 'var(--success)' : (scoreNum >= 40 ? 'var(--warning)' : 'var(--danger)');
                var scoreColorVal = scoreNum >= 70 ? '#16A34A' : (scoreNum >= 40 ? '#D97706' : '#DC2626');
                var img = getFirstImage(item.images);
                var imgHtml = img ? '<img src="' + escapeHtml(getImageUrl(img)) + '" style="width:80px;height:80px;object-fit:cover;border-radius:4px;">' : '';

                html += '<div class="match-item">';
                html += '  <div class="layui-row layui-col-space15">';
                html += '    <div class="layui-col-md2">' + imgHtml + '</div>';
                html += '    <div class="layui-col-md7">';
                html += '      <h4 style="font-size:16px;margin-bottom:8px;">' + escapeHtml(item.title) + '</h4>';
                html += '      <p style="color:var(--text-secondary);font-size:13px;margin-bottom:4px;"><i class="layui-icon layui-icon-note"></i> ' + getCategoryText(item.category) + '</p>';
                html += '      <p style="color:var(--text-secondary);font-size:13px;margin-bottom:4px;"><i class="layui-icon layui-icon-location"></i> ' + escapeHtml(item.location) + '</p>';
                html += '      <p style="color:var(--text-secondary);font-size:13px;"><i class="layui-icon layui-icon-time"></i> ' + formatDate(item.eventTime) + '</p>';
                html += '    </div>';
                html += '    <div class="layui-col-md3" style="text-align:center;">';
                html += '      <div style="font-size:28px;font-weight:700;color:' + scoreColorVal + ';">' + score + '%</div>';
                html += '      <div style="width:100%;height:6px;background:var(--gray-100);border-radius:3px;margin:8px 0;overflow:hidden;">';
                html += '        <div style="width:' + scoreNum + '%;height:100%;background:' + scoreColorVal + ';border-radius:3px;transition:width 0.3s ease;"></div>';
                html += '      </div>';
                html += '      <div style="font-size:12px;color:var(--text-tertiary);margin-bottom:8px;">相似度</div>';
                html += '      <a href="/item/detail?type=found&id=' + item.foundItemId + '" class="layui-btn layui-btn-sm layui-btn-primary">查看详情</a>';
                if (isLogin()) {
                    html += '      <button class="layui-btn layui-btn-sm layui-btn-normal" onclick="initiateClaim(' + itemId + ',' + item.foundItemId + ',' + item.matchScore + ')">发起认领</button>';
                }
                html += '    </div>';
                html += '  </div>';
                html += '</div>';
            });
            $('#matchResultList').html(html);
            $('#matchResultCard').show();
        }

        // 发起认领
        window.initiateClaim = function (lostItemId, foundItemId, matchScore) {
            if (!isLogin()) {
                layer.msg('请先登录', { icon: 2, time: 1500 }, function () {
                    window.location.href = '/login';
                });
                return;
            }
            layer.prompt({
                title: '请输入认领备注（选填）',
                formType: 2,
                area: ['400px', '120px']
            }, function (value, index) {
                $.ajax({
                    url: '/api/claim',
                    type: 'POST',
                    headers: { 'Authorization': 'Bearer ' + getToken() },
                    data: {
                        lostItemId: lostItemId,
                        foundItemId: foundItemId,
                        matchScore: matchScore,
                        remark: value
                    },
                    success: function (res) {
                        if (res.code === 200) {
                            layer.close(index);
                            layer.msg('认领申请已提交', { icon: 1, time: 1500 }, function () {
                                window.location.href = '/claim/my';
                            });
                        } else {
                            layer.msg(res.msg || '认领失败', { icon: 2 });
                        }
                    },
                    error: function () {
                        layer.msg('网络错误', { icon: 2 });
                    }
                });
            });
        };

        // 直接认领（拾到物品）
        window.directClaim = function (foundItemId) {
            if (!isLogin()) {
                layer.msg('请先登录', { icon: 2, time: 1500 }, function () {
                    window.location.href = '/login';
                });
                return;
            }
            layer.confirm('确定要认领此物品吗？', { icon: 3, title: '确认认领' }, function (index) {
                layer.close(index);
                layer.prompt({
                    title: '请输入认领备注（说明丢失情况）',
                    formType: 2,
                    area: ['400px', '120px']
                }, function (value, index2) {
                    // 直接认领：lostItemId传0或null表示无对应丢失物品
                    $.ajax({
                        url: '/api/claim',
                        type: 'POST',
                        headers: { 'Authorization': 'Bearer ' + getToken() },
                        data: {
                            lostItemId: 0,
                            foundItemId: foundItemId,
                            matchScore: 0,
                            remark: value
                        },
                        success: function (res) {
                            if (res.code === 200) {
                                layer.close(index2);
                                layer.msg('认领申请已提交', { icon: 1, time: 1500 }, function () {
                                    window.location.href = '/claim/my';
                                });
                            } else {
                                layer.msg(res.msg || '认领失败', { icon: 2 });
                            }
                        },
                        error: function () {
                            layer.msg('网络错误', { icon: 2 });
                        }
                    });
                });
            });
        };

        // 检查收藏状态
        function checkFavorite(itemId, type) {
            if (!isLogin()) return;
            ajaxRequest('/api/favorite/check?itemId=' + itemId + '&type=' + type, 'GET', null, function(data) {
                if (data === true) {
                    $('#favIcon').removeClass('layui-icon-star').addClass('layui-icon-star-fill');
                    $('#favText').text('已收藏');
                    $('#btnFavorite').attr('data-favorited', 'true');
                }
            });
        }

        // 切换收藏
        window.toggleFavorite = function(itemId, type) {
            if (!isLogin()) {
                layer.msg('请先登录', {icon: 2});
                return;
            }
            $.ajax({
                url: '/api/favorite/toggle',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                headers: {'Authorization': 'Bearer ' + getToken()},
                data: JSON.stringify({itemId: itemId, itemType: type}),
                success: function(res) {
                    if (res.code === 200) {
                        if (res.data === true) {
                            $('#favIcon').removeClass('layui-icon-star').addClass('layui-icon-star-fill');
                            $('#favText').text('已收藏');
                            layer.msg('收藏成功', {icon: 1, time: 1500});
                        } else {
                            $('#favIcon').removeClass('layui-icon-star-fill').addClass('layui-icon-star');
                            $('#favText').text('收藏');
                            layer.msg('已取消收藏', {icon: 1, time: 1500});
                        }
                    }
                },
                error: function() { layer.msg('网络错误', {icon: 2}); }
            });
        };

        var commentPage = 1;
        var commentTotal = 0;
        // 加载评论
        function loadComments(itemId, type, append) {
            ajaxRequest('/api/comment/list?itemId=' + itemId + '&type=' + type + '&page=' + commentPage + '&size=10', 'GET', null, function(data) {
                renderComments(data, append);
            }, function() {
                $('#commentList').html('<div class="empty-state"><i class="layui-icon layui-icon-face-surprised"></i><p>暂无评论</p></div>');
            });
        }

        function renderComments(data, append) {
            var records = data.records || data.list || [];
            commentTotal = data.total || 0;
            if (records.length === 0 && !append) {
                $('#commentList').html('<div class="empty-state"><i class="layui-icon layui-icon-dialogue"></i><p>暂无评论，快来抢沙发吧~</p></div>');
                return;
            }
            var html = '';
            records.forEach(function(c) {
                var avatar = c.avatar ? getImageUrl(c.avatar) : 'https://cdn.jsdelivr.net/gh/layui/layui@2.9.x/src/css/modules/layer/default/avatar.png';
                html += '<div class="comment-item" style="display:flex;gap:12px;padding:12px 0;border-bottom:1px solid var(--border-subtle);">';
                html += '  <img src="' + escapeHtml(avatar) + '" style="width:36px;height:36px;border-radius:50%;flex-shrink:0;" alt="头像">';
                html += '  <div style="flex:1;">';
                html += '    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">';
                html += '      <span style="font-weight:600;font-size:13px;color:var(--text-primary);">' + escapeHtml(c.username || '匿名用户') + '</span>';
                html += '      <span style="font-size:11px;color:var(--text-tertiary);">' + formatDate(c.createTime) + '</span>';
                html += '    </div>';
                html += '    <p style="font-size:13px;color:var(--text-secondary);line-height:1.6;margin:0;">' + escapeHtml(c.content) + '</p>';
                if (currentUser && c.userId === currentUser.id) {
                    html += '    <a href="javascript:deleteComment(' + c.id + ');" style="font-size:12px;color:var(--text-tertiary);margin-top:4px;display:inline-block;">删除</a>';
                }
                html += '  </div>';
                html += '</div>';
            });
            if (commentTotal > commentPage * 10) {
                html += '<div class="load-more-btn" style="text-align:center;padding:12px;"><button class="layui-btn layui-btn-sm layui-btn-primary" onclick="loadMoreComments()">加载更多</button></div>';
            }
            if (append) {
                $('#commentList .load-more-btn').remove();
                $('#commentList').append(html);
            } else {
                $('#commentList').html(html);
            }
        }

        window.loadMoreComments = function() {
            commentPage++;
            loadComments(itemId, itemType, true);
        };

        window.submitComment = function() {
            var content = $('#commentContent').val().trim();
            if (!content) {
                layer.msg('请输入评论内容', {icon: 2});
                return;
            }
            $.ajax({
                url: '/api/comment',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                headers: {'Authorization': 'Bearer ' + getToken()},
                data: JSON.stringify({itemId: parseInt(itemId), itemType: itemType, content: content}),
                success: function(res) {
                    if (res.code === 200) {
                        $('#commentContent').val('');
                        commentPage = 1;
                        loadComments(itemId, itemType);
                        layer.msg('评论成功', {icon: 1, time: 1500});
                    } else {
                        layer.msg(res.msg || '评论失败', {icon: 2});
                    }
                },
                error: function() { layer.msg('网络错误', {icon: 2}); }
            });
        };

        window.deleteComment = function(commentId) {
            layer.confirm('确定删除这条评论吗？', {icon: 3}, function(index) {
                layer.close(index);
                $.ajax({
                    url: '/api/comment/' + commentId,
                    type: 'DELETE',
                    headers: {'Authorization': 'Bearer ' + getToken()},
                    success: function(res) {
                        if (res.code === 200) {
                            commentPage = 1;
                            loadComments(itemId, itemType);
                            layer.msg('删除成功', {icon: 1, time: 1500});
                        } else {
                            layer.msg(res.msg || '删除失败', {icon: 2});
                        }
                    },
                    error: function() { layer.msg('网络错误', {icon: 2}); }
                });
            });
        };

        // 加载数据
        loadDetail();
    });
</script>
