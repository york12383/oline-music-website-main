package com.example.music.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileUploadController {

    // 注入配置的本地存储路径
    @Value("${local.file.storage-path}")
    private  String storagePath;

    //更新用户头像
    public  String uploadAtorImgFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/img/avatorImages/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }
            // 获取原始文件名，并生成唯一文件名以避免重复
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf('.'));
            // 构建目标文件对象
            File targetFile = new File(dest, fileName);
            // 将上传的文件写入目标位置
            file.transferTo(targetFile);
            // 返回成功提示
            return fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }

    //上传歌曲图片(文件唯一UUID)
    public  String uploadSongImgFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/img/songPic/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }

            // 获取原始文件名，并生成唯一文件名以避免重复
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf('.'));

            // 构建目标文件对象
            File targetFile = new File(dest, fileName);

            // 将上传的文件写入目标位置
            file.transferTo(targetFile);
            System.out.println("FileUploadController文件存储路径：" + targetPath);
            // 返回成功提示
            return  fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }
    //歌单图片
    public  String uploadSonglistImgFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/img/songListPic/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf('.'));
            // 构建目标文件对象
            File targetFile = new File(dest, fileName);
            // 将上传的文件写入目标位置
            file.transferTo(targetFile);
            // 返回成功提示
            return fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }
    //上传歌手图片
    public  String uploadImgFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/img/singerPic/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String fileName = file.getOriginalFilename();
            // 构建目标文件对象
            File targetFile = new File(dest, fileName);

            // 将上传的文件写入目标位置
            file.transferTo(targetFile);

            // 返回成功提示
            return "File uploaded successfully" + fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }
    //上传歌曲文件(文件唯一UUID)
    public  String uploadFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/song/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }


            // 获取原始文件名，并生成唯一文件名以避免重复
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf('.'));

            //String fileName = file.getOriginalFilename();
            // 构建目标文件对象
            File targetFile = new File(dest, fileName);

            // 将上传的文件写入目标位置
            file.transferTo(targetFile);

            // 返回成功提示
            return fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }


    public  String uploadbannerImgFile(MultipartFile file) {
        try {
            // 构建目标路径
            String targetPath = storagePath + "/img/swiper/";

            // 创建目标目录（如果不存在）
            File dest = new File(targetPath);
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf('.'));

            File targetFile = new File(dest, fileName);

            // 将上传的文件写入目标位置
            file.transferTo(targetFile);

            // 返回成功提示
            return "/img/swiper/" + fileName;
        } catch (IOException e) {
            // 捕获异常并返回错误信息
            e.printStackTrace();
            return "Error uploading file: " + e.getMessage();
        }
    }
}

