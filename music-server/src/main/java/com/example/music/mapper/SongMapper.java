package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.domain.Song;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface SongMapper extends BaseMapper<Song> {

     String selectSongNameById(Integer songId);

     List<Song> selectFavoriteSongsByUserId(Integer userId);
     
    /**
     * 查询指定日期范围内每天新增歌曲数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日新增歌曲数统计
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
           "FROM song " +
           "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
           "GROUP BY DATE(create_time) " +
           "ORDER BY date")
    List<Map<String, Object>> getWeeklyNewSongs(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
}