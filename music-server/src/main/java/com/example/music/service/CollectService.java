package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Collect;
import com.example.music.model.request.CollectRequest;

public interface CollectService extends IService<Collect> {

    R addCollection(CollectRequest addCollectRequest);

    R existCollection(CollectRequest isCollectRequest);

    R deleteCollect(CollectRequest deleteCollectRequest);

    R collectionOfUser(Integer userId, Byte type);
}