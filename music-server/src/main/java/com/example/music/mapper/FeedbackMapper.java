package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.domain.Feedback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface FeedbackMapper extends BaseMapper<Feedback> {
    @Select("select count(*) from feedback where status = 0")
    Long countPendingFeedback();

    @Select("select date(create_time) as date, count(*) as count " +
            "from feedback " +
            "where create_time >= #{startDate} and create_time < #{endDate} " +
            "group by date(create_time) " +
            "order by date(create_time)")
    List<Map<String, Object>> getDailyFeedbackTrend(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
}
