package com.example.music.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Paths;

@Controller
@RequestMapping("/download")
public class FileDownloadController {

 @Value("${local.file.storage-path}")
 private String fileStoragePath;

 @GetMapping("/{filename:.+}")
 public ResponseEntity<Resource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
  try {
   // 构建文件路径
   String filePath = Paths.get(fileStoragePath, "song", filename).toString();
   File file = new File(filePath);

   // 检查文件是否存在
   if (!file.exists()) {
    return ResponseEntity.notFound().build();
   }

   // 创建文件系统资源
   FileSystemResource resource = new FileSystemResource(file);

   // 设置响应头
   HttpHeaders headers = new HttpHeaders();
   headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
   headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

   // 确定媒体类型
   String contentType = request.getServletContext().getMimeType(file.getAbsolutePath());
   if (contentType == null) {
    contentType = "application/octet-stream";
   }

   return ResponseEntity.ok()
           .headers(headers)
           .contentLength(file.length())
           .contentType(MediaType.parseMediaType(contentType))
           .body(resource);

  } catch (Exception e) {
   return ResponseEntity.internalServerError().build();
  }
 }
}
