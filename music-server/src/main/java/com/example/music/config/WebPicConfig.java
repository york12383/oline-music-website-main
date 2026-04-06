package com.example.music.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebPicConfig implements WebMvcConfigurer {

    @Value("${local.file.storage-path}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = storagePath.endsWith("/") ? storagePath : storagePath + "/";

        registry.addResourceHandler("/img/avatorImages/**")
                .addResourceLocations("file:" + basePath + "img/avatorImages/");

        registry.addResourceHandler("/img/singerPic/**", "/singer/img/**")
                .addResourceLocations("file:" + basePath + "img/singerPic/");

        registry.addResourceHandler("/img/songPic/**")
                .addResourceLocations("file:" + basePath + "img/songPic/");

        registry.addResourceHandler("/img/songListPic/**", "/img/songlist/**")
                .addResourceLocations("file:" + basePath + "img/songListPic/");

        registry.addResourceHandler("/img/swiper/**")
                .addResourceLocations("file:" + basePath + "img/swiper/");

        registry.addResourceHandler("/song/**")
                .addResourceLocations("file:" + basePath + "song/");
    }
}
