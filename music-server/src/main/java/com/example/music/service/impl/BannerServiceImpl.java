package com.example.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.controller.FileUploadController;
import com.example.music.mapper.BannerMapper;
import com.example.music.model.domain.Banner;
import com.example.music.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author asus
 * @description 针对表【banner】的数据库操作Service实现
 * @createDate 2022-06-13 13:13:42
 */

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired  // 注入 FileUploadController
    private FileUploadController fileUploadController;

    /**
     * 获取所有轮播图信息，并使用Redis缓存
     * 
     * @Cacheable 注解表示该方法的返回值将被缓存，下一次相同请求会直接从缓存中获取，不再执行方法体
     * value = "banner" 表示缓存的名称为 banner
     * key = "'list'" 表示缓存的键为 list，Redis 中存储的 key 为 banner::list
     * 
     * 当方法被调用时，若缓存中存在该 key，则直接返回缓存数据；
     * 若不存在，则执行方法体，并将返回结果存入缓存以便下次使用
     */
    //@Cacheable(value = "banner", key = "'list'")
    @Override
    public List<Banner> getAllBanner() {
        // 打印日志，用于调试，表示本次请求未命中缓存
        //System.out.println("没有走缓存");
        // 查询数据库，获取所有的 Banner 数据
        // selectList(null) 表示查询所有记录，等价于 SELECT * FROM banner
        System.out.println("开始查询数据库"+ bannerMapper.selectList(null) );
        return bannerMapper.selectList(null);
    }

    @Override // 删除指定 id 的 Banner
    public R deleteBannerOfId(Integer id) {
        if (bannerMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }

    @Override // 添加 Banner
    public R addBanner(String path) {
        Banner banner = new Banner();
        banner.setPic(path);
        if (bannerMapper.insert(banner) > 0){
            return R.success("添加成功", banner.getId());
        }
        return R.error("添加失败");
    }

    @Override // 上传图片
    public R uploadImg(MultipartFile avatorFile) {
        String s = fileUploadController.uploadbannerImgFile(avatorFile);
        //System.out.println("wudijdisjaidsadsadafada"+s);
        return R.success("上传成功", s);
    }
}
