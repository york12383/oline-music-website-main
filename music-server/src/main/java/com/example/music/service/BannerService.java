package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Banner;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author asus
* @description 针对表【banner】的数据库操作Service
* @createDate 2022-06-13 13:13:42
*/
public interface BannerService extends IService<Banner> {

    R addBanner(String path);

    List<Banner> getAllBanner();

    R deleteBannerOfId(Integer id);

    R uploadImg(MultipartFile avatorFile);
}
