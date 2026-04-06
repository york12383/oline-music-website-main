package com.example.music.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.*;
import com.example.music.model.domain.Consumer;
import com.example.music.model.domain.RankList;
import com.example.music.model.domain.RankSong;

import com.example.music.model.request.*;
import com.example.music.service.RankListService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author Administrator
 * 评价服务实现类
 */
@Service
public class RankListServiceImpl extends ServiceImpl<RankListMapper, RankList> implements RankListService {


    @Autowired
    private RankListMapper rankMapper;
    @Autowired
    private RankSongMapper rankSongMapper;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private SongListMapper songListMapper;
    @Autowired
    private ConsumerMapper consumerMapper;
    @Autowired
    private RankListMapper rankListMapper;

    @Override
    public R addRank(RankListRequest rankListAddRequest) {
        QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", rankListAddRequest.getSongListId());
        queryWrapper.eq("consumer_id", rankListAddRequest.getConsumerId());
        if (rankMapper.selectCount(queryWrapper) > 0) {
            return R.warning("您已经评价过这个歌单了");
        }

        RankList rankList = new RankList();
        BeanUtils.copyProperties(rankListAddRequest, rankList);
        if (rankMapper.insert(rankList) > 0) {
            return R.success("评价成功");
        } else {
            return R.error("评价失败");
        }
    }

    // 获取指定歌单的评分
    @Override
    public R rankOfSongListId(Long songListId) {
        // 评分总人数如果为 0，则返回0；否则返回计算出的结果
        QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id",songListId);
        Long rankNum = rankMapper.selectCount(queryWrapper);
        //平均分 = (该歌单所有评分总和) / (对该歌单评分的人数)
        return R.success(null, (rankNum <= 0) ? 0 : rankMapper.selectScoreSum(songListId) / rankNum);
    }

    // 获取用户对指定歌单的评分
    @Override
    public R getUserRank(Long consumerId, Long songListId) {
        Integer i = rankMapper.selectUserRank(consumerId, songListId);
        return R.success(null, i);
    }


    // 获取指定歌曲的评分
    @Override
    public R rankOfSongId(Integer songId) {
        QueryWrapper<RankSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        Long rankNum = rankSongMapper.selectCount(queryWrapper);
        //平均分 = (该歌单所有评分总和) / (对该歌单评分的人数)
        return R.success(null, (rankNum <= 0) ? 0 : rankSongMapper.selectScoreSum(songId) / rankNum);
    }


    @Override
    public R addSongRanK(RankSongRequest rankSongRequest) {
        QueryWrapper<RankSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id", rankSongRequest.getSongId());
        queryWrapper.eq("consumer_id", rankSongRequest.getConsumerId());
        if (rankSongMapper.selectCount(queryWrapper) > 0) {
            return R.warning("您已经评价过这首歌了");
        }

        RankSong rankSong = new RankSong();
        BeanUtils.copyProperties(rankSongRequest, rankSong);
        if (rankSongMapper.insert(rankSong) > 0) {
            return R.success("评价成功");
        } else {
            return R.error("评价失败");
        }
    }


    @Override
    public R deleteRankSong(Integer songId, Integer consumerId, HttpSession session) {
        String consumerName = (String) session.getAttribute("username");

        if (consumerId == consumerMapper.selectIdByconsumerName(consumerName)){
            QueryWrapper<RankSong> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("consumer_id", consumerId).eq("song_id", songId);
            rankSongMapper.delete(queryWrapper);
            return R.success("评价删除成功！");
        }
     return R.error("您没有权限删除该评分！");
    }

    @Override
    public R deleteRankList(Integer songListId, Integer consumerId, HttpSession session) {
        String consumerName = (String) session.getAttribute("username");

        if (consumerId == consumerMapper.selectIdByconsumerName(consumerName)){
            QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("consumer_id", consumerId).eq("song_list_id", songListId);
            rankListMapper.delete(queryWrapper);
            return R.success("评价删除成功！");
        }
        return R.error("您没有权限删除该评分！");
    }



    // 删除用户对指定歌单/歌曲的评分
    @Override
    public R deleteRankOfId(Integer type, Integer id) {

        if (type == 1) {
            // 删除用户对指定歌单的评分
            QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id",id);
            rankMapper.delete(queryWrapper);
        } else if (type == 0) {
            // 删除用户对指定歌曲的评分
            QueryWrapper<RankSong> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id",id);
            rankSongMapper.delete(queryWrapper);
        }
        return R.success("评分删除成功！");
    }

    @Override
    public R getUserRankSong(Integer consumerId, Integer songId) {
        Integer i = rankSongMapper.selectUserRankSong(consumerId, songId);
        return R.success(null, i);
    }

    // 在 RankListServiceImpl.java 中添加以下方法实现
    @Override
    public R getAllRankByType(Integer type, Integer currentPage, Integer pageSize, String name,String consumerName) {
        if (type == 0) {
            return getSongRanks(currentPage, pageSize,  name, consumerName);// 查询歌曲评分
        } else if (type == 1) {
            return getSongListRanks(currentPage, pageSize,  name, consumerName);// 查询歌单评分
        } else {
            return R.error("无效的类型参数");
        }
    }
    private R getSongRanks(Integer currentPage, Integer pageSize, String songName, String consumerName) {
        try {
            Page<RankSong> page = new Page<>(currentPage, pageSize);// 构建分页参数
            LambdaQueryWrapper<RankSong> queryWrapper = new LambdaQueryWrapper<>();// 构建查询条件
            if (StringUtils.isNotBlank(songName)) {
                // 这里假设你需要联表查询歌曲表来匹配歌曲名
                // 具体实现取决于你的表结构
                queryWrapper.exists(
                        "SELECT 1 FROM song s WHERE s.id = song_id AND s.name LIKE '%" + songName + "%'"
                );
            }// 添加歌曲名搜索条件（需要联表查询）
            if (StringUtils.isNotBlank(consumerName)) {
                queryWrapper.exists(
                        "SELECT 1 FROM consumer c WHERE c.id = consumer_id AND c.username LIKE '%" + consumerName + "%'"
                );
            } // 添加用户名搜索条件（需要联表查询）
            queryWrapper.orderByDesc(RankSong::getId);// 按评分时间倒序排列
            // 执行查询
            IPage<RankSong> rankPage = rankSongMapper.selectPage(page, queryWrapper);// 修改为明确的泛型声明
            // 转换为DTO并填充额外信息
            List<SongRankDTO> resultList = rankPage.getRecords().stream().map(rank -> {
                SongRankDTO dto = new SongRankDTO();
                dto.setId(rank.getId());
                dto.setScore(rank.getScore());
                dto.setCreateTime(null);

                // 这里需要根据你的实际表结构来设置歌曲名和用户名
                // 假设你有相应的方法来获取这些信息
                dto.setSongName(getSongNameById(rank.getSongId()));
                dto.setConsumerName(getConsumerNameById(rank.getConsumerId()));

                return dto;
            }).collect(Collectors.toList());
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", rankPage.getTotal());
            result.put("records", resultList);
            result.put("size", rankPage.getSize());
            result.put("current", rankPage.getCurrent());
            result.put("pages", rankPage.getPages());
            return R.success("获取歌曲评分成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("获取歌曲评分失败: " + e.getMessage());
        }
    }

    private R getSongListRanks(Integer currentPage, Integer pageSize, String songListName,String consumerName){
        try {
        Page<RankList> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<RankList> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(songListName)) {
            queryWrapper.exists(
                    "SELECT 1 FROM song_list sl WHERE sl.id = song_list_id AND sl.title LIKE '%" + songListName + "%'"
            );
        }
        if (StringUtils.isNotBlank(consumerName)) {
            queryWrapper.exists(
                    "SELECT 1 FROM consumer c WHERE c.id = consumer_id AND c.username LIKE '%" + consumerName + "%'"
            );
        }

        IPage<RankList> rankPage = rankListMapper.selectPage(page, queryWrapper);
        // 转换为DTO并填充额外信息
        List<SongListRankDTO> resultList = rankPage.getRecords().stream().map(rank -> {
            SongListRankDTO dto = new SongListRankDTO();
            dto.setId(rank.getId());
            dto.setScore(rank.getScore());
            dto.setCreateTime(null);
            dto.setSongListName(getSongListNameById(rank.getSongListId()));
            dto.setConsumerName(getConsumerNameById(Math.toIntExact(rank.getConsumerId())));
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", resultList);
        result.put("total", rankPage.getTotal());
        result.put("size", rankPage.getSize());
        result.put("current", rankPage.getCurrent());
        result.put("pages", rankPage.getPages());

        return R.success("获取歌单评分成功", result);

    } catch (Exception e) {
        e.printStackTrace();
        return R.error("获取歌单评分失败: " + e.getMessage());
    }

    }

    // 辅助方法 - 根据ID获取歌曲名
    private String getSongNameById(Integer songId) {
        if (songId == null) return "未知歌曲";
        // 实现从歌曲表查询歌曲名的逻辑
        // 例如: return songMapper.selectSongNameById(songId);
        return songMapper.selectSongNameById(songId);

    }

    // 辅助方法 - 根据ID获取歌单名
    private String getSongListNameById(Long songListId) {
        if (songListId == null) return "未知歌单";
        // 实现从歌单表查询歌单名的逻辑
        return songListMapper.selectSongListNameById(songListId);

    }

    // 辅助方法 - 根据ID获取用户名
    private String getConsumerNameById(Integer consumerId) {
        if (consumerId == null) return "未知用户";
        // 实现从用户表查询用户名的逻辑
        return consumerMapper.selectConsumerNameById(consumerId);
        //return "用户名"; // 临时返回
    }



}
