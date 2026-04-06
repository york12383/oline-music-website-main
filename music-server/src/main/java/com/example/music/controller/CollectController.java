package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.CollectRequest;
import com.example.music.service.CollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CollectController {

    @Autowired
    private CollectService collectService;

    @PostMapping("/collection/add")
    public R addCollection(@RequestBody CollectRequest addCollectRequest) {
        return collectService.addCollection(addCollectRequest);
    }

    @DeleteMapping("/collection/delete")
    public R deleteCollection(@RequestParam Integer userId,
                              @RequestParam(required = false) Byte type,
                              @RequestParam(required = false) Integer songId,
                              @RequestParam(required = false) Integer songListId) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setUserId(userId);
        collectRequest.setType(type);
        collectRequest.setSongId(songId);
        collectRequest.setSongListId(songListId);
        return collectService.deleteCollect(collectRequest);
    }

    @PostMapping("/collection/status")
    public R isCollection(@RequestBody CollectRequest isCollectRequest) {
        return collectService.existCollection(isCollectRequest);
    }

    @GetMapping("/collection/detail")
    public R collectionOfUser(@RequestParam Integer userId, @RequestParam(required = false) Byte type) {
        return collectService.collectionOfUser(userId, type);
    }
}