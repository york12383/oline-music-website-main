package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @Author Actording
 * @Time : 20点49分2025年11月20日
 **/
@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;


    //问题在于后端接收参数的方式。您的前端发送的是 JSON 对象 {linkUrl: "xxx"}
    @PostMapping("/addBanner")
    public R addBanner(@RequestBody Map<String, String> request) {
        String path = request.get("linkUrl");
        System.out.println("接收到的path: " + path);
        return bannerService.addBanner(path);
    }

    @GetMapping("/getAllBanner")
    public R getAllBanner(){
        return R.success("成功获取轮播图",bannerService.getAllBanner());
    }

    @DeleteMapping("/deleteBannerOfId")
    public R deleteBannerOfId(@RequestParam int id){
        return bannerService.deleteBannerOfId(id);
    }

    @PostMapping("/uploadImg")
    public R uploadImg(@RequestParam("file") MultipartFile avatorFile){
        return bannerService.uploadImg(avatorFile);
    }
}
