package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.domain.Singer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface SingerMapper extends BaseMapper<Singer> {
    
    /**
     * 查询拥有最新歌曲的热门歌手
     * @param limit 限制返回数量
     * @return 热门歌手列表
     */
    List<Singer> selectHotSingers(@Param("limit") int limit);
}