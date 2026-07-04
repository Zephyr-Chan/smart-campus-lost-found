package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 贡献排行榜服务接口
 */
public interface RankService {

    /**
     * 获取排行榜列表
     *
     * @param type  榜单类型：credit-积分榜，recovery-找回率榜，contribution-贡献榜
     * @param limit 返回条数
     * @return 排行榜数据列表
     */
    Result<List<Map<String, Object>>> getRankList(String type, int limit);
}
