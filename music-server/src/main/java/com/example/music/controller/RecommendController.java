package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.RecommendBootstrapRequest;
import com.example.music.service.RecommendService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@CrossOrigin
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @GetMapping("/data")
    public R getRecommendations(HttpSession session,
                                @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        return R.success("推荐成功", recommendService.selectListByRecommend(currentUserId, limit));
    }

    @GetMapping("/song-lists")
    public R getRecommendSongLists(HttpSession session,
                                   @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        return R.success("推荐成功", recommendService.selectSongListsByRecommend(currentUserId, limit));
    }

    @GetMapping("/singers")
    public R getRecommendSingers(HttpSession session,
                                 @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        return R.success("推荐成功", recommendService.selectSingersByRecommend(currentUserId, limit));
    }

    @GetMapping("/bootstrap/options")
    public R getBootstrapOptions(HttpSession session) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        return R.success("获取成功", recommendService.getBootstrapOptions(currentUserId));
    }

    @PostMapping("/bootstrap/preferences")
    public R saveBootstrapPreferences(HttpSession session,
                                      @RequestBody RecommendBootstrapRequest request) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        recommendService.saveBootstrapPreferences(currentUserId, request);
        return R.success("保存成功");
    }

    @GetMapping("/debug/profile")
    public R getDebugProfile(HttpSession session) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        if (currentUserId == null || currentUserId <= 0) {
            return R.error("请先登录");
        }
        return R.success("获取成功", recommendService.getRecommendDebugProfile(currentUserId));
    }
}
