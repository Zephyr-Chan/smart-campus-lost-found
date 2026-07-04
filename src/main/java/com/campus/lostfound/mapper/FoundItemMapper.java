package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.vo.FoundItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拾到物品Mapper
 */
@Mapper
public interface FoundItemMapper extends BaseMapper<FoundItem> {

    /**
     * 分页查询拾到物品列表（关联user表查拾到者姓名）
     *
     * @param offset   偏移量
     * @param size     每页数量
     * @param category 分类
     * @param keyword  关键词
     * @return 拾到物品VO列表
     */
    List<FoundItemVO> selectFoundItemList(@Param("offset") int offset,
                                          @Param("size") int size,
                                          @Param("category") String category,
                                          @Param("keyword") String keyword);

    /**
     * 根据ID查询拾到物品详情（关联user表）
     *
     * @param id 物品ID
     * @return 拾到物品VO
     */
    FoundItemVO selectFoundItemById(@Param("id") Long id);
}
