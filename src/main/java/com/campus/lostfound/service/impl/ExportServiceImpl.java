package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.utils.ExcelExportUtil;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.OperationLog;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.OperationLogMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据导出服务实现类
 */
@Slf4j
@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void exportItems(HttpServletResponse response, String itemType, String status) {
        List<String> headers = Arrays.asList("ID", "标题", "分类", "地点", "发布时间", "状态");
        List<List<Object>> rows = new ArrayList<>();

        Integer statusFilter = parseStatus(status);
        if (Constants.ITEM_TYPE_FOUND.equalsIgnoreCase(itemType)) {
            QueryWrapper<FoundItem> wrapper = new QueryWrapper<>();
            if (statusFilter != null) {
                wrapper.eq("status", statusFilter);
            }
            wrapper.orderByDesc("create_time");
            List<FoundItem> list = foundItemMapper.selectList(wrapper);
            for (FoundItem item : list) {
                rows.add(Arrays.asList(
                        item.getId(),
                        item.getTitle(),
                        item.getCategory(),
                        item.getLocation(),
                        formatDateTime(item.getCreateTime()),
                        itemStatusText(item.getStatus())
                ));
            }
        } else {
            // 默认导出丢失物品
            QueryWrapper<LostItem> wrapper = new QueryWrapper<>();
            if (statusFilter != null) {
                wrapper.eq("status", statusFilter);
            }
            wrapper.orderByDesc("create_time");
            List<LostItem> list = lostItemMapper.selectList(wrapper);
            for (LostItem item : list) {
                rows.add(Arrays.asList(
                        item.getId(),
                        item.getTitle(),
                        item.getCategory(),
                        item.getLocation(),
                        formatDateTime(item.getCreateTime()),
                        itemStatusText(item.getStatus())
                ));
            }
        }

        String fileName = Constants.ITEM_TYPE_FOUND.equalsIgnoreCase(itemType) ? "拾到物品列表" : "丢失物品列表";
        doExport(response, fileName, headers, rows);
    }

    @Override
    public void exportClaims(HttpServletResponse response, String status) {
        QueryWrapper<Claim> wrapper = new QueryWrapper<>();
        Integer statusFilter = parseStatus(status);
        if (statusFilter != null) {
            wrapper.eq("status", statusFilter);
        }
        wrapper.orderByDesc("create_time");
        List<Claim> claims = claimMapper.selectList(wrapper);
        doExportClaims(response, "认领记录", claims);
    }

    @Override
    public void exportLogs(HttpServletResponse response) {
        List<String> headers = Arrays.asList("ID", "用户名", "操作", "方法", "IP", "时间");
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        List<List<Object>> rows = new ArrayList<>();
        for (OperationLog logItem : logs) {
            rows.add(Arrays.asList(
                    logItem.getId(),
                    logItem.getUsername(),
                    logItem.getOperation(),
                    logItem.getMethod(),
                    logItem.getIp(),
                    formatDateTime(logItem.getCreateTime())
            ));
        }
        doExport(response, "操作日志", headers, rows);
    }

    @Override
    public void exportMyClaims(HttpServletResponse response, Long userId) {
        QueryWrapper<Claim> wrapper = new QueryWrapper<>();
        wrapper.eq("claimant_id", userId).orderByDesc("create_time");
        List<Claim> claims = claimMapper.selectList(wrapper);
        doExportClaims(response, "我的认领记录", claims);
    }

    /**
     * 导出认领记录（exportClaims 与 exportMyClaims 共用）
     * 表头：[ID, 失物标题, 拾物标题, 认领人, 状态, 匹配分数, 创建时间]
     */
    private void doExportClaims(HttpServletResponse response, String fileName, List<Claim> claims) {
        List<String> headers = Arrays.asList("ID", "失物标题", "拾物标题", "认领人", "状态", "匹配分数", "创建时间");

        // 批量关联失物标题、拾物标题、认领人姓名，避免 N+1 查询
        Set<Long> lostIds = claims.stream().map(Claim::getLostItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> foundIds = claims.stream().map(Claim::getFoundItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> claimantIds = claims.stream().map(Claim::getClaimantId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> lostTitleMap = batchLoadLostTitles(lostIds);
        Map<Long, String> foundTitleMap = batchLoadFoundTitles(foundIds);
        Map<Long, String> userNameMap = batchLoadUserNames(claimantIds);

        List<List<Object>> rows = new ArrayList<>();
        for (Claim claim : claims) {
            rows.add(Arrays.asList(
                    claim.getId(),
                    lostTitleMap.getOrDefault(claim.getLostItemId(), ""),
                    foundTitleMap.getOrDefault(claim.getFoundItemId(), ""),
                    userNameMap.getOrDefault(claim.getClaimantId(), ""),
                    claimStatusText(claim.getStatus()),
                    claim.getMatchScore() != null ? claim.getMatchScore() : BigDecimal.ZERO,
                    formatDateTime(claim.getCreateTime())
            ));
        }
        doExport(response, fileName, headers, rows);
    }

    private void doExport(HttpServletResponse response, String fileName, List<String> headers, List<List<Object>> rows) {
        try {
            ExcelExportUtil.exportToExcel(response, fileName, headers, rows);
        } catch (IOException e) {
            log.error("导出 Excel 失败：{}", fileName, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    // ===== 批量加载辅助方法 =====

    private Map<Long, String> batchLoadLostTitles(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<LostItem> list = lostItemMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>(list.size());
        for (LostItem item : list) {
            map.put(item.getId(), item.getTitle());
        }
        return map;
    }

    private Map<Long, String> batchLoadFoundTitles(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<FoundItem> list = foundItemMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>(list.size());
        for (FoundItem item : list) {
            map.put(item.getId(), item.getTitle());
        }
        return map;
    }

    private Map<Long, String> batchLoadUserNames(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<User> list = userMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>(list.size());
        for (User user : list) {
            map.put(user.getId(), StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
        }
        return map;
    }

    // ===== 状态/格式化辅助方法 =====

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return Integer.valueOf(status.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_FMT);
    }

    private String itemStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Constants.STATUS_PENDING:
                return "待处理";
            case Constants.STATUS_MATCHED:
                return "已匹配";
            case Constants.STATUS_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }

    private String claimStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Constants.CLAIM_PENDING:
                return "待审核";
            case Constants.CLAIM_APPROVED:
                return "已通过";
            case Constants.CLAIM_REJECTED:
                return "已拒绝";
            case Constants.CLAIM_COMPLETED:
                return "已完成";
            default:
                return "未知";
        }
    }
}
