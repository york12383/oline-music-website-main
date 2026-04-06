package com.example.music.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author 祝英台炸油条
 * @Time : 2022/6/7 17:08
 **/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public CorsInterceptor corsInterceptor() {
        return new CorsInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(corsInterceptor())
                .addPathPatterns("/**"); //将该拦截器应用到所有路径 (/**)
//                .excludePathPatterns(
//                        "/admin/login/",
//                        "/user/**"
//                );

    }

//    https://www.cnblogs.com/ztn195/p/19084037
//    @Value("${local.file.storage-path}") // 确保application.properties中有这个配置
//    private String storagePath;
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 映射本地文件路径
//        registry.addResourceHandler("/img/**")
//                .addResourceLocations("file:" + storagePath + "/img/")
//                .setCachePeriod(604800); // 缓存一周（可选）
//
//        // 如果需要其他路径映射可以继续添加
//        registry.addResourceHandler("/user01/**")
//                .addResourceLocations("file:" + storagePath + "/user01/");
//
//        // 确保路径以/结尾
//        if (!storagePath.endsWith("/")) {
//            storagePath = storagePath + "/";
//        }
//    }
//    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
//    @PostConstruct
//    public void init() {
//        log.info("========================================");
//        log.info("配置的文件存储路径：{}", storagePath);
//        log.info("测试文件是否存在：{}",
//                new File(storagePath, "img/singerPic/wanglihong.jpg").exists());
//        log.info("========================================");
//    }

}



