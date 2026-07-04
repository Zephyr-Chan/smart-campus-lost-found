/**
 * 公共JS工具函数
 * 校园失物招领系统 - 前端公共方法
 */

// ===== Token 管理（Cookie + localStorage 双重存储）=====

/**
 * Cookie操作：设置Cookie
 */
function setCookie(name, value, days) {
    var expires = '';
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = '; expires=' + date.toUTCString();
    }
    document.cookie = name + '=' + encodeURIComponent(value) + expires + '; path=/; SameSite=Lax';
}

/**
 * Cookie操作：获取Cookie
 */
function getCookie(name) {
    var nameEQ = name + '=';
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) {
            return decodeURIComponent(c.substring(nameEQ.length, c.length));
        }
    }
    return null;
}

/**
 * Cookie操作：删除Cookie
 */
function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}

var TOKEN_COOKIE_NAME = 'lost_found_token';

/**
 * 获取token（优先Cookie，降级localStorage）
 */
function getToken() {
    var token = getCookie(TOKEN_COOKIE_NAME);
    if (!token) {
        try { token = localStorage.getItem('token'); } catch (e) { /* localStorage不可用 */ }
    }
    return token;
}

/**
 * 存储token（同时写入Cookie和localStorage）
 */
function setToken(token) {
    setCookie(TOKEN_COOKIE_NAME, token, 1); // 1天过期
    try { localStorage.setItem('token', token); } catch (e) { /* localStorage不可用 */ }
}

/**
 * 清除token
 */
function removeToken() {
    deleteCookie(TOKEN_COOKIE_NAME);
    try {
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
    } catch (e) { /* localStorage不可用 */ }
}

/**
 * 判断是否登录
 */
function isLogin() {
    var token = getToken();
    return token !== null && token !== undefined && token !== '';
}

/**
 * 获取当前登录用户信息（从localStorage）
 */
function getCurrentUser() {
    var userStr;
    try { userStr = localStorage.getItem('userInfo'); } catch (e) { return null; }
    if (userStr) {
        try {
            return JSON.parse(userStr);
        } catch (e) {
            return null;
        }
    }
    return null;
}

/**
 * 判断当前用户是否为管理员
 */
function isAdmin() {
    var user = getCurrentUser();
    return user !== null && user.role === 1;
}

/**
 * 退出登录
 */
function logout() {
    var token = getToken();
    if (token) {
        $.ajax({
            url: '/api/user/logout',
            type: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            complete: function () {
                removeToken();
                window.location.href = '/login';
            }
        });
    } else {
        removeToken();
        window.location.href = '/login';
    }
}

/**
 * 页面加载时检查登录状态
 * @param {boolean} requireLogin 是否强制要求登录
 */
function checkLogin(requireLogin) {
    if (requireLogin && !isLogin()) {
        layer.msg('请先登录', { icon: 2, time: 1500 }, function () {
            window.location.href = '/login';
        });
        return false;
    }
    return true;
}

// ===== AJAX 请求封装 =====

/**
 * 封装jQuery AJAX，自动添加Authorization头，处理401跳转登录
 * @param {string} url 请求地址
 * @param {string} method 请求方法 GET/POST/PUT/DELETE
 * @param {object} data 请求数据
 * @param {function} callback 成功回调函数 callback(data)
 * @param {function} errorCallback 错误回调函数（可选）
 */
function ajaxRequest(url, method, data, callback, errorCallback) {
    var token = getToken();
    var headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    var ajaxOptions = {
        url: url,
        type: method.toUpperCase(),
        headers: headers,
        success: function (response) {
            if (response.code === 200) {
                if (callback) {
                    callback(response.data, response);
                }
            } else if (response.code === 401) {
                layer.msg(response.msg || '登录已过期，请重新登录', { icon: 2, time: 1500 }, function () {
                    removeToken();
                    window.location.href = '/login';
                });
            } else {
                if (errorCallback) {
                    errorCallback(response);
                } else {
                    layer.msg(response.msg || '操作失败', { icon: 2 });
                }
            }
        },
        error: function (xhr) {
            if (xhr.status === 401) {
                layer.msg('登录已过期，请重新登录', { icon: 2, time: 1500 }, function () {
                    removeToken();
                    window.location.href = '/login';
                });
            } else if (xhr.status === 403) {
                layer.msg('无权限访问', { icon: 2 });
            } else {
                var errorMsg = '网络请求失败';
                try {
                    var resp = JSON.parse(xhr.responseText);
                    if (resp.msg) {
                        errorMsg = resp.msg;
                    }
                } catch (e) {
                    // ignore
                }
                if (errorCallback) {
                    errorCallback({ code: xhr.status, msg: errorMsg });
                } else {
                    layer.msg(errorMsg, { icon: 2 });
                }
            }
        }
    };

    if (method.toUpperCase() === 'GET') {
        ajaxOptions.data = data;
        ajaxOptions.dataType = 'json';
    } else {
        ajaxOptions.data = JSON.stringify(data);
        ajaxOptions.contentType = 'application/json;charset=UTF-8';
        ajaxOptions.dataType = 'json';
    }

    $.ajax(ajaxOptions);
}

/**
 * 封装文件上传AJAX
 * @param {string} url 上传地址
 * @param {FormData} formData 表单数据
 * @param {function} callback 成功回调
 * @param {function} errorCallback 错误回调（可选）
 * @param {function} progressCallback 进度回调（可选）
 */
function ajaxUpload(url, formData, callback, errorCallback, progressCallback) {
    var token = getToken();
    $.ajax({
        url: url,
        type: 'POST',
        headers: token ? { 'Authorization': 'Bearer ' + token } : {},
        data: formData,
        processData: false,
        contentType: false,
        dataType: 'json',
        xhr: function () {
            var xhr = new XMLHttpRequest();
            if (progressCallback) {
                xhr.upload.addEventListener('progress', function (e) {
                    if (e.lengthComputable) {
                        progressCallback(Math.round((e.loaded / e.total) * 100));
                    }
                });
            }
            return xhr;
        },
        success: function (response) {
            if (response.code === 200) {
                if (callback) callback(response.data, response);
            } else if (response.code === 401) {
                layer.msg('登录已过期，请重新登录', { icon: 2, time: 1500 }, function () {
                    removeToken();
                    window.location.href = '/login';
                });
            } else {
                if (errorCallback) {
                    errorCallback(response);
                } else {
                    layer.msg(response.msg || '上传失败', { icon: 2 });
                }
            }
        },
        error: function (xhr) {
            if (xhr.status === 401) {
                removeToken();
                window.location.href = '/login';
            } else {
                if (errorCallback) {
                    errorCallback({ code: xhr.status, msg: '文件上传失败' });
                } else {
                    layer.msg('文件上传失败', { icon: 2 });
                }
            }
        }
    });
}

// ===== 日期格式化 =====

/**
 * 格式化日期为 YYYY-MM-DD HH:mm
 * @param {string} dateStr 日期字符串
 */
function formatDate(dateStr) {
    if (!dateStr) return '';
    var date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    var year = date.getFullYear();
    var month = (date.getMonth() + 1).toString().padStart(2, '0');
    var day = date.getDate().toString().padStart(2, '0');
    var hours = date.getHours().toString().padStart(2, '0');
    var minutes = date.getMinutes().toString().padStart(2, '0');
    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
}

// ===== 状态转换 =====

/**
 * 物品状态转文字
 * 0-待处理 1-已匹配 2-已关闭
 */
function getStatusText(status) {
    var map = {
        0: '待处理',
        1: '已匹配',
        2: '已关闭'
    };
    return map[status] !== undefined ? map[status] : '未知';
}

/**
 * 物品状态转CSS类名
 */
function getStatusClass(status) {
    var map = {
        0: 'status-pending',
        1: 'status-matched',
        2: 'status-closed'
    };
    return map[status] !== undefined ? map[status] : 'status-closed';
}

/**
 * 认领状态转文字
 * 0-待审核 1-已通过 2-已拒绝 3-已完成
 */
function getClaimStatusText(status) {
    var map = {
        0: '待审核',
        1: '已通过',
        2: '已拒绝',
        3: '已完成'
    };
    return map[status] !== undefined ? map[status] : '未知';
}

/**
 * 认领状态转CSS类名
 */
function getClaimStatusClass(status) {
    var map = {
        0: 'status-pending',
        1: 'status-approved',
        2: 'status-rejected',
        3: 'status-completed'
    };
    return map[status] !== undefined ? map[status] : 'status-closed';
}

/**
 * 分类转文字
 * electronics-电子产品 certificate-证件 book-书籍 clothing-衣物 other-其他
 */
function getCategoryText(category) {
    var map = {
        'electronics': '电子产品',
        'certificate': '证件',
        'book': '书籍',
        'clothing': '衣物',
        'other': '其他'
    };
    return map[category] !== undefined ? map[category] : category;
}

/**
 * 用户角色转文字
 * 0-学生 1-管理员
 */
function getRoleText(role) {
    return role === 1 ? '管理员' : '学生';
}

/**
 * 用户状态转文字
 * 1-正常 0-禁用
 */
function getUserStatusText(status) {
    return status === 1 ? '正常' : '禁用';
}

// ===== 工具方法 =====

/**
 * HTML转义（防XSS）
 * @param {string} str 原始字符串
 */
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * 获取URL参数
 * @param {string} name 参数名
 */
function getQueryParam(name) {
    var urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

/**
 * 获取图片URL（处理相对路径）
 * @param {string} imagePath 图片路径
 */
function getImageUrl(imagePath) {
    if (!imagePath) return '';
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
        return imagePath;
    }
    return imagePath.startsWith('/') ? imagePath : '/' + imagePath;
}

/**
 * 解析图片路径，兼容JSON数组和逗号分隔两种格式
 * @param {string|Array} images 图片路径
 * @returns {Array} 图片URL数组
 */
function parseImages(images) {
    if (!images) return [];
    if (Array.isArray(images)) {
        return images.filter(function (img) { return img && img.trim() !== ''; });
    }
    if (typeof images === 'string') {
        var trimmed = images.trim();
        if (trimmed === '') return [];
        if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
            try {
                var parsed = JSON.parse(trimmed);
                if (Array.isArray(parsed)) {
                    return parsed.filter(function (img) { return img && img.trim() !== ''; });
                }
            } catch (e) {}
        }
        return trimmed.split(',').filter(function (img) {
            return img.trim() !== '';
        }).map(function (img) {
            return img.trim();
        });
    }
    return [];
}

/**
 * 获取第一张图片URL
 * @param {string|Array} images 多图片路径（JSON数组或逗号分隔）
 */
function getFirstImage(images) {
    var arr = parseImages(images);
    return arr.length > 0 ? arr[0] : '';
}

/**
 * 获取所有图片URL数组
 * @param {string|Array} images 多图片路径（JSON数组或逗号分隔）
 */
function getAllImages(images) {
    return parseImages(images);
}

/**
 * 截断字符串
 * @param {string} str 原字符串
 * @param {number} len 最大长度
 */
function truncateStr(str, len) {
    if (!str) return '';
    if (str.length <= len) return str;
    return str.substring(0, len) + '...';
}

/**
 * 显示加载遮罩
 */
function showLoading() {
    if ($('#loadingMask').length === 0) {
        $('body').append('<div id="loadingMask" class="loading-mask"><div class="loading-spinner"></div></div>');
    }
}

/**
 * 隐藏加载遮罩
 */
function hideLoading() {
    $('#loadingMask').remove();
}

// ===== 全局AJAX设置 =====
$(document).ready(function () {
    // 设置全局AJAX，自动添加Authorization头
    $.ajaxSetup({
        beforeSend: function (xhr) {
            var token = getToken();
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        },
        statusCode: {
            401: function () {
                removeToken();
                if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
                    window.location.href = '/login';
                }
            }
        }
    });
});
