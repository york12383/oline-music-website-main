package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.CollectMapper;
import com.example.music.model.domain.Collect;
import com.example.music.model.request.CollectRequest;
import com.example.music.service.CollectService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect> implements CollectService {
    private static final byte SONG_TYPE = 0;
    private static final byte SONG_LIST_TYPE = 1;

    @Autowired
    private CollectMapper collectMapper;

    @Override
    public R addCollection(CollectRequest addCollectRequest) {
        Byte type = normalizeType(addCollectRequest.getType(), addCollectRequest.getSongId(), addCollectRequest.getSongListId());
        if (type == null) {
            return R.error("收藏参数错误");
        }

        QueryWrapper<Collect> queryWrapper = buildCollectionQuery(addCollectRequest.getUserId(), type,
                addCollectRequest.getSongId(), addCollectRequest.getSongListId());
        if (collectMapper.selectCount(queryWrapper) > 0) {
            return R.success("已收藏", true);
        }

        Collect collect = new Collect();
        BeanUtils.copyProperties(addCollectRequest, collect);
        collect.setType(type);
        if (type == SONG_TYPE) {
            collect.setSongListId(null);
        } else {
            collect.setSongId(null);
        }

        if (collectMapper.insert(collect) > 0) {
            return R.success("收藏成功", true);
        }
        return R.error("收藏失败");
    }

    @Override
    public R existCollection(CollectRequest isCollectRequest) {
        Byte type = normalizeType(isCollectRequest.getType(), isCollectRequest.getSongId(), isCollectRequest.getSongListId());
        if (type == null) {
            return R.success("未收藏", false);
        }

        QueryWrapper<Collect> queryWrapper = buildCollectionQuery(isCollectRequest.getUserId(), type,
                isCollectRequest.getSongId(), isCollectRequest.getSongListId());
        boolean exists = collectMapper.selectCount(queryWrapper) > 0;
        return R.success(exists ? "已收藏" : "未收藏", exists);
    }

    @Override
    public R deleteCollect(CollectRequest deleteCollectRequest) {
        Byte type = normalizeType(deleteCollectRequest.getType(), deleteCollectRequest.getSongId(), deleteCollectRequest.getSongListId());
        if (type == null) {
            return R.error("取消收藏参数错误");
        }

        QueryWrapper<Collect> queryWrapper = buildCollectionQuery(deleteCollectRequest.getUserId(), type,
                deleteCollectRequest.getSongId(), deleteCollectRequest.getSongListId());
        if (collectMapper.delete(queryWrapper) > 0) {
            return R.success("取消收藏", false);
        }
        return R.success("已取消收藏", false);
    }

    @Override
    public R collectionOfUser(Integer userId, Byte type) {
        QueryWrapper<Collect> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        if (type != null) {
            queryWrapper.eq("type", type);
        }
        queryWrapper.orderByDesc("create_time");
        return R.success("用户收藏", collectMapper.selectList(queryWrapper));
    }

    private Byte normalizeType(Byte type, Integer songId, Integer songListId) {
        if (type != null) {
            return type;
        }
        if (songId != null) {
            return SONG_TYPE;
        }
        if (songListId != null) {
            return SONG_LIST_TYPE;
        }
        return null;
    }

    private QueryWrapper<Collect> buildCollectionQuery(Integer userId, Byte type, Integer songId, Integer songListId) {
        QueryWrapper<Collect> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("type", type);
        if (type == SONG_TYPE) {
            queryWrapper.eq("song_id", songId);
        } else {
            queryWrapper.eq("song_list_id", songListId);
        }
        return queryWrapper;
    }
}
