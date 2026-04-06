package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.model.domain.Consumer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ConsumerMapper extends BaseMapper<Consumer> {
    String selectConsumerNameById(Integer id);

    Integer selectIdByconsumerName(String username);

    @Select("SELECT COUNT(*) FROM consumer WHERE deleted = 0")
    Long countActiveUsers();
    
    /**
     * 查询指定日期范围内每天新增用户数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日新增用户数统计
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
           "FROM consumer " +
           "WHERE deleted = 0 AND create_time >= #{startDate} AND create_time < #{endDate} " +
           "GROUP BY DATE(create_time) " +
           "ORDER BY date")
    List<Map<String, Object>> getWeeklyNewUsers(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
                                               
    /**
     * 按年龄段统计用户分布
     * @return 各年龄段用户数量
     */
    @Select("SELECT " +
           "CASE " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) < 18 THEN '18岁以下' " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) >= 18 AND TIMESTAMPDIFF(YEAR, birth, CURDATE()) < 30 THEN '18-30岁' " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) >= 30 AND TIMESTAMPDIFF(YEAR, birth, CURDATE()) < 40 THEN '30-40岁' " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) >= 40 AND TIMESTAMPDIFF(YEAR, birth, CURDATE()) < 50 THEN '40-50岁' " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) >= 50 AND TIMESTAMPDIFF(YEAR, birth, CURDATE()) < 60 THEN '50-60岁' " +
           "  WHEN TIMESTAMPDIFF(YEAR, birth, CURDATE()) >= 60 THEN '60岁以上' " +
           "  ELSE '未知' " +
           "END as ageRange, " +
           "COUNT(*) as count " +
           "FROM consumer " +
           "WHERE deleted = 0 AND birth IS NOT NULL " +
           "GROUP BY ageRange " +
           "ORDER BY FIELD(ageRange, '18岁以下', '18-30岁', '30-40岁', '40-50岁', '50-60岁', '60岁以上', '未知')")
    List<Map<String, Object>> getUserAgeDistribution();

    /**
     * 按性别统计用户分布
     *
     * @return 各性别用户数量
     */
    @Select("SELECT " +
           "CASE " +
           "  WHEN sex = 0 THEN '女' " +
           "  WHEN sex = 1 THEN '男' " +
           "  WHEN sex = 2 THEN '保密' " +
           "  ELSE '未知' " +
           "END as sexLabel, " +
           "COUNT(*) as count " +
           "FROM consumer " +
           "WHERE deleted = 0 " +
           "GROUP BY sexLabel")
    List<Map<String, Object>> getUserSexDistribution();
}
