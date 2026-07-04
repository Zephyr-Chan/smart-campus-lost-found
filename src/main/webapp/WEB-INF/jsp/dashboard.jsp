<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<!-- ECharts CDN -->
<script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
<div class="main-container">
    <!-- 总览数字卡片 -->
    <div class="layui-row layui-col-space20" id="statRow">
        <div class="layui-col-xs6 layui-col-md3">
            <div class="stat-card">
                <div class="stat-icon bg-red"><i class="layui-icon layui-icon-about"></i></div>
                <div class="stat-info">
                    <div class="stat-value" id="totalLost">-</div>
                    <div class="stat-label">丢失总数</div>
                </div>
            </div>
        </div>
        <div class="layui-col-xs6 layui-col-md3">
            <div class="stat-card">
                <div class="stat-icon bg-blue"><i class="layui-icon layui-icon-template-1"></i></div>
                <div class="stat-info">
                    <div class="stat-value" id="totalFound">-</div>
                    <div class="stat-label">拾到总数</div>
                </div>
            </div>
        </div>
        <div class="layui-col-xs6 layui-col-md3">
            <div class="stat-card">
                <div class="stat-icon bg-green"><i class="layui-icon layui-icon-ok-circle"></i></div>
                <div class="stat-info">
                    <div class="stat-value" id="totalRecovered">-</div>
                    <div class="stat-label">已找回数</div>
                </div>
            </div>
        </div>
        <div class="layui-col-xs6 layui-col-md3">
            <div class="stat-card">
                <div class="stat-icon bg-orange"><i class="layui-icon layui-icon-time"></i></div>
                <div class="stat-info">
                    <div class="stat-value" id="totalPending">-</div>
                    <div class="stat-label">待处理数</div>
                </div>
            </div>
        </div>
    </div>

    <!-- 图表区域 -->
    <div class="layui-row layui-col-space20">
        <!-- 找回率饼图 -->
        <div class="layui-col-md6">
            <div class="chart-container">
                <div class="chart-title">找回率统计</div>
                <div id="recoveryChart" class="chart-box"></div>
            </div>
        </div>
        <!-- 分类柱状图 -->
        <div class="layui-col-md6">
            <div class="chart-container">
                <div class="chart-title">各分类丢失物品数量</div>
                <div id="categoryChart" class="chart-box"></div>
            </div>
        </div>
    </div>

    <!-- 地点分布 -->
    <div class="chart-container">
        <div class="chart-title">丢失地点分布 Top 10</div>
        <div id="locationChart" class="chart-box" style="height:400px;"></div>
    </div>

    <!-- ===== 新增图表区域 ===== -->

    <!-- 趋势折线图 -->
    <div class="chart-container">
        <div class="chart-title">发布趋势（最近30天）</div>
        <div id="trendChart" class="chart-box" style="height:350px;"></div>
    </div>

    <!-- 热力图 + 用户活跃度 -->
    <div class="layui-row layui-col-space20">
        <!-- 物品地理分布热力图 -->
        <div class="layui-col-md6">
            <div class="chart-container">
                <div class="chart-title">物品地理分布热力图</div>
                <div id="heatmapChart" class="chart-box" style="height:350px;"></div>
            </div>
        </div>
        <!-- 用户活跃度 -->
        <div class="layui-col-md6">
            <div class="chart-container">
                <div class="chart-title">用户活跃度（近30天新增注册）</div>
                <div id="userActiveChart" class="chart-box" style="height:350px;"></div>
            </div>
        </div>
    </div>

    <!-- 分类找回率雷达图 -->
    <div class="chart-container">
        <div class="chart-title">各分类找回率</div>
        <div id="categoryRecoveryChart" class="chart-box" style="height:400px;"></div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
<script>
    layui.use(['layer'], function () {
        var layer = layui.layer;

        // 暮山紫霭 ECharts 12 色统一调色板
        var ECHART_COLORS = ['#7C6B9E', '#9F7AEA', '#8B7DA8', '#B5A9C9', '#16A34A', '#D97706', '#DC2626', '#06B6D4', '#6B5F86', '#A89BC4', '#4A4259', '#D4CDE0'];

        var recoveryChart = null;
        var categoryChart = null;
        var locationChart = null;
        var trendChart = null;
        var heatmapChart = null;
        var userActiveChart = null;
        var categoryRecoveryChart = null;

        // 初始化图表
        function initCharts() {
            recoveryChart = echarts.init(document.getElementById('recoveryChart'));
            categoryChart = echarts.init(document.getElementById('categoryChart'));
            locationChart = echarts.init(document.getElementById('locationChart'));
            trendChart = echarts.init(document.getElementById('trendChart'));
            heatmapChart = echarts.init(document.getElementById('heatmapChart'));
            userActiveChart = echarts.init(document.getElementById('userActiveChart'));
            categoryRecoveryChart = echarts.init(document.getElementById('categoryRecoveryChart'));

            // 加载动画
            recoveryChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            categoryChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            locationChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            trendChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            heatmapChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            userActiveChart.showLoading({ text: '加载中...', color: '#7C6B9E' });
            categoryRecoveryChart.showLoading({ text: '加载中...', color: '#7C6B9E' });

            // 自适应
            window.addEventListener('resize', function () {
                recoveryChart.resize();
                categoryChart.resize();
                locationChart.resize();
                trendChart.resize();
                heatmapChart.resize();
                userActiveChart.resize();
                categoryRecoveryChart.resize();
            });
        }

        // 加载数据
        function loadData() {
            ajaxRequest('/api/dashboard/overview', 'GET', null, function (data) {
                // 更新数字卡片
                $('#totalLost').text(data.totalLost || 0);
                $('#totalFound').text(data.totalFound || 0);
                $('#totalRecovered').text(data.totalRecovered || 0);
                $('#totalPending').text(data.totalPending || 0);

                // 找回率饼图
                renderRecoveryChart(data);

                // 分类柱状图
                renderCategoryChart(data.categoryStats || []);

                // 地点分布柱状图
                renderLocationChart(data.locationStats || []);
            }, function () {
                recoveryChart.hideLoading();
                categoryChart.hideLoading();
                locationChart.hideLoading();
                layer.msg('仪表盘数据加载失败', { icon: 2 });
            });
        }

        // 找回率饼图
        function renderRecoveryChart(data) {
            recoveryChart.hideLoading();
            var recovered = data.totalRecovered || 0;
            var totalLost = data.totalLost || 0;
            var unrecovered = totalLost - recovered;

            recoveryChart.setOption({
                tooltip: {
                    trigger: 'item',
                    formatter: '{a} <br/>{b}: {c} ({d}%)'
                },
                legend: {
                    orient: 'vertical',
                    right: 10,
                    top: 'center',
                    data: ['已找回', '未找回']
                },
                color: [ECHART_COLORS[4], ECHART_COLORS[6]],
                series: [{
                    name: '找回情况',
                    type: 'pie',
                    radius: ['50%', '70%'],
                    center: ['40%', '50%'],
                    avoidLabelOverlap: false,
                    itemStyle: {
                        borderRadius: 8,
                        borderColor: '#fff',
                        borderWidth: 2
                    },
                    label: {
                        show: true,
                        position: 'center',
                        formatter: function () {
                            var rate = totalLost > 0 ? ((recovered / totalLost) * 100).toFixed(1) : 0;
                            return '{title|找回率}\n{rate|' + rate + '%}';
                        },
                        rich: {
                            title: { fontSize: 14, color: '#94A3B8', padding: [0, 0, 8, 0] },
                            rate: { fontSize: 28, fontWeight: 'bold', color: '#16A34A' }
                        }
                    },
                    emphasis: {
                        label: { show: true, fontSize: 16, fontWeight: 'bold' }
                    },
                    data: [
                        { value: recovered, name: '已找回' },
                        { value: unrecovered > 0 ? unrecovered : 0, name: '未找回' }
                    ]
                }]
            });
        }

        // 分类柱状图
        function renderCategoryChart(categoryStats) {
            categoryChart.hideLoading();
            var categories = [];
            var counts = [];
            categoryStats.forEach(function (item) {
                categories.push(getCategoryText(item.category));
                counts.push(item.count);
            });

            categoryChart.setOption({
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: categories,
                    axisLabel: { interval: 0, rotate: categories.length > 5 ? 30 : 0 }
                },
                yAxis: {
                    type: 'value',
                    minInterval: 1
                },
                series: [{
                    name: '数量',
                    type: 'bar',
                    data: counts,
                    barWidth: '40%',
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: '#7C6B9E' },
                            { offset: 1, color: '#93C5FD' }
                        ]),
                        borderRadius: [4, 4, 0, 0]
                    },
                    label: {
                        show: true,
                        position: 'top',
                        color: '#0F172A'
                    }
                }]
            });
        }

        // 地点分布柱状图
        function renderLocationChart(locationStats) {
            locationChart.hideLoading();
            // 取前10
            var top10 = locationStats.slice(0, 10);
            var locations = [];
            var counts = [];
            top10.forEach(function (item) {
                locations.push(item.location);
                counts.push(item.count);
            });

            locationChart.setOption({
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' }
                },
                grid: {
                    left: '3%',
                    right: '6%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    minInterval: 1
                },
                yAxis: {
                    type: 'category',
                    data: locations.reverse(),
                    axisLabel: { interval: 0 }
                },
                series: [{
                    name: '丢失次数',
                    type: 'bar',
                    data: counts.reverse(),
                    barWidth: '50%',
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                            { offset: 0, color: '#D97706' },
                            { offset: 1, color: '#FCD34D' }
                        ]),
                        borderRadius: [0, 4, 4, 0]
                    },
                    label: {
                        show: true,
                        position: 'right',
                        color: '#0F172A'
                    }
                }]
            });
        }

        // ===== 新增图表：趋势、热力图、用户活跃度、分类找回率 =====

        // 加载趋势数据
        function loadTrendData() {
            ajaxRequest('/api/dashboard/trend?days=30', 'GET', null, function (data) {
                renderTrendChart(data);
            }, function () {
                trendChart.hideLoading();
                trendChart.setOption({
                    title: { text: '趋势数据加载失败', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                layer.msg('趋势数据加载失败', { icon: 2 });
            });
        }

        // 渲染趋势折线图（丢失 vs 拾到）
        function renderTrendChart(data) {
            trendChart.hideLoading();
            var dates = data.dates || [];
            var lostCounts = data.lostCounts || [];
            var foundCounts = data.foundCounts || [];

            trendChart.setOption({
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'cross' }
                },
                legend: {
                    data: ['丢失物品', '拾到物品'],
                    top: 5
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: 40,
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: dates,
                    axisLabel: {
                        formatter: function (val) {
                            return val.substring(5);
                        },
                        interval: Math.ceil(dates.length / 15)
                    }
                },
                yAxis: {
                    type: 'value',
                    minInterval: 1
                },
                color: [ECHART_COLORS[6], ECHART_COLORS[0]],
                series: [
                    {
                        name: '丢失物品',
                        type: 'line',
                        smooth: true,
                        data: lostCounts,
                        areaStyle: {
                            opacity: 0.1
                        },
                        itemStyle: { color: '#DC2626' }
                    },
                    {
                        name: '拾到物品',
                        type: 'line',
                        smooth: true,
                        data: foundCounts,
                        areaStyle: {
                            opacity: 0.1
                        },
                        itemStyle: { color: '#7C6B9E' }
                    }
                ]
            });
        }

        // 加载热力图数据
        function loadHeatmapData() {
            ajaxRequest('/api/dashboard/heatmap', 'GET', null, function (data) {
                renderHeatmapChart(data);
            }, function () {
                heatmapChart.hideLoading();
                heatmapChart.setOption({
                    title: { text: '热力图数据加载失败', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                layer.msg('热力图数据加载失败', { icon: 2 });
            });
        }

        // 渲染热力图（散点图，按经纬度分布）
        function renderHeatmapChart(data) {
            heatmapChart.hideLoading();
            if (!data || data.length === 0) {
                heatmapChart.setOption({
                    title: { text: '暂无地理分布数据', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                return;
            }

            var scatterData = [];
            var maxCount = 0;
            data.forEach(function (item) {
                if (item.lng && item.lat) {
                    scatterData.push([item.lng, item.lat, item.count, item.geohash]);
                    if (item.count > maxCount) {
                        maxCount = item.count;
                    }
                }
            });

            heatmapChart.setOption({
                tooltip: {
                    formatter: function (params) {
                        return '区域: ' + params.data[3] + '<br/>物品数量: ' + params.data[2];
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    name: '经度',
                    nameLocation: 'middle',
                    nameGap: 25,
                    scale: true
                },
                yAxis: {
                    type: 'value',
                    name: '纬度',
                    nameLocation: 'middle',
                    nameGap: 35,
                    scale: true
                },
                visualMap: {
                    min: 0,
                    max: maxCount > 0 ? maxCount : 1,
                    calculable: true,
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 5,
                    inRange: {
                        color: [ECHART_COLORS[0], ECHART_COLORS[5], ECHART_COLORS[6]]
                    },
                    text: ['多', '少']
                },
                series: [{
                    type: 'scatter',
                    data: scatterData,
                    symbolSize: function (data) {
                        return Math.sqrt(data[2]) * 8 + 10;
                    },
                    itemStyle: {
                        opacity: 0.8,
                        shadowBlur: 10,
                        shadowColor: 'rgba(0, 0, 0, 0.2)'
                    },
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 15,
                            opacity: 1
                        }
                    }
                }]
            });
        }

        // 加载用户活跃度数据
        function loadUserActiveData() {
            ajaxRequest('/api/dashboard/user-active', 'GET', null, function (data) {
                renderUserActiveChart(data);
            }, function () {
                userActiveChart.hideLoading();
                userActiveChart.setOption({
                    title: { text: '用户数据加载失败', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                layer.msg('用户活跃度数据加载失败', { icon: 2 });
            });
        }

        // 渲染用户活跃度柱状图
        function renderUserActiveChart(data) {
            userActiveChart.hideLoading();
            if (!data || data.length === 0) {
                userActiveChart.setOption({
                    title: { text: '暂无用户数据', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                return;
            }

            var dates = [];
            var counts = [];
            data.forEach(function (item) {
                dates.push(item.date);
                counts.push(item.count);
            });

            userActiveChart.setOption({
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    formatter: function (params) {
                        return params[0].name + '<br/>新增用户: ' + params[0].value + ' 人';
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    top: 30,
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: dates,
                    axisLabel: {
                        formatter: function (val) {
                            return val.substring(5);
                        },
                        interval: Math.ceil(dates.length / 10),
                        rotate: 30
                    }
                },
                yAxis: {
                    type: 'value',
                    minInterval: 1,
                    name: '新增用户数'
                },
                series: [{
                    name: '新增用户',
                    type: 'bar',
                    data: counts,
                    barWidth: '50%',
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: '#16A34A' },
                            { offset: 1, color: '#34D399' }
                        ]),
                        borderRadius: [4, 4, 0, 0]
                    },
                    label: {
                        show: true,
                        position: 'top',
                        color: '#0F172A',
                        fontSize: 11
                    }
                }]
            });
        }

        // 加载分类找回率数据
        function loadCategoryRecoveryData() {
            ajaxRequest('/api/dashboard/category-recovery', 'GET', null, function (data) {
                renderCategoryRecoveryChart(data);
            }, function () {
                categoryRecoveryChart.hideLoading();
                categoryRecoveryChart.setOption({
                    title: { text: '分类找回率数据加载失败', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                layer.msg('分类找回率数据加载失败', { icon: 2 });
            });
        }

        // 渲染分类找回率雷达图
        function renderCategoryRecoveryChart(data) {
            categoryRecoveryChart.hideLoading();
            if (!data || !data.categories || data.categories.length === 0) {
                categoryRecoveryChart.setOption({
                    title: { text: '暂无分类数据', left: 'center', top: 'center', textStyle: { color: '#94A3B8', fontSize: 14 } }
                });
                return;
            }

            var categories = data.categories;
            var recoveryRates = data.recoveryRates || [];
            var totalCounts = data.totalCounts || [];
            var matchedCounts = data.matchedCounts || [];

            // 雷达图指标（最大值100%）
            var indicator = categories.map(function (cat) {
                return { name: cat, max: 100 };
            });

            categoryRecoveryChart.setOption({
                tooltip: {
                    trigger: 'item',
                    formatter: function (params) {
                        var html = params.name + '<br/>';
                        categories.forEach(function (cat, i) {
                            html += cat + ': 找回率 ' + recoveryRates[i] + '%';
                            html += ' (' + matchedCounts[i] + '/' + totalCounts[i] + ')<br/>';
                        });
                        return html;
                    }
                },
                legend: {
                    data: ['找回率'],
                    top: 5
                },
                radar: {
                    indicator: indicator,
                    shape: 'polygon',
                    splitNumber: 5,
                    axisName: {
                        color: '#475569',
                        fontSize: 12
                    },
                    splitArea: {
                        areaStyle: {
                            color: ['rgba(124, 107, 158, 0.02)', 'rgba(124, 107, 158, 0.05)', 'rgba(124, 107, 158, 0.08)', 'rgba(124, 107, 158, 0.11)', 'rgba(124, 107, 158, 0.14)']
                        }
                    },
                    splitLine: {
                        lineStyle: { color: '#E2E8F0' }
                    },
                    axisLine: {
                        lineStyle: { color: '#E2E8F0' }
                    }
                },
                color: [ECHART_COLORS[0]],
                series: [{
                    name: '找回率',
                    type: 'radar',
                    data: [{
                        value: recoveryRates,
                        name: '找回率',
                        areaStyle: {
                            opacity: 0.2
                        },
                        lineStyle: {
                            width: 2
                        },
                        itemStyle: {
                            color: '#7C6B9E'
                        }
                    }]
                }]
            });
        }

        // 初始化
        $(document).ready(function () {
            initCharts();
            loadData();
            loadTrendData();
            loadHeatmapData();
            loadUserActiveData();
            loadCategoryRecoveryData();
        });
    });
</script>
