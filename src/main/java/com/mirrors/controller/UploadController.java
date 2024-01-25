package com.mirrors.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mirrors.dto.Result;
import com.mirrors.entity.UploadFileParam;
import com.mirrors.service.IUploadService;
import com.mirrors.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 上传管理
 */
@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    @Autowired
    private IUploadService uploadService;

    //---------------------------------------上传大文件视频到MinIO-----------------------------------------------

    /**
     * （1）检测文件是否完整
     *
     * @param fileMd5
     * @return
     */
    @PostMapping("/checkfile")
    public Result checkFile(@RequestParam("fileMd5") String fileMd5) {
        return uploadService.checkFile(fileMd5);
    }

    /**
     * （2）检测分块
     *
     * @param fileMd5
     * @param chunk
     * @return
     */
    @PostMapping("/checkchunks")
    public Result checkChunks(@RequestParam("fileMd5") String fileMd5,
                              @RequestParam("chunk") int chunk) {
        return uploadService.checkChunks(fileMd5, chunk);
    }

    /**
     * （3）上传分块
     *
     * @param file
     * @param fileMd5
     * @param chunk
     * @return
     * @throws Exception
     */
    @PostMapping("/uploadchunks")
    public Result uploadChunks(@RequestParam("file") MultipartFile file,
                               @RequestParam("fileMd5") String fileMd5,
                               @RequestParam("chunk") int chunk) throws Exception {
        return uploadService.uploadChunks(fileMd5, chunk, file.getBytes());
    }

    /**
     * （4）合并分块
     *
     * @param fileMd5
     * @param fileName
     * @param chunkTotal
     * @return
     */
    @PostMapping("/mergechunks")
    public Result mergeChunks(@RequestParam("fileMd5") String fileMd5,
                              @RequestParam("fileName") String fileName,
                              @RequestParam("chunkTotal") int chunkTotal) {
        UploadFileParam uploadFileParam = new UploadFileParam();
        uploadFileParam.setFileType("video");
        uploadFileParam.setFilename(fileName);
        return uploadService.mergeChunks(fileMd5, chunkTotal, uploadFileParam);
    }

    //---------------------------------------上传图片到MinIO----------------------------------------------------

    /**
     * 上传图片到分布式存储
     *
     * @param file
     * @param userId
     * @param path
     * @param objectName
     * @return
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // consumes指定类型
    public Result uploadImage2Minio(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "userId", required = false) Long userId,
                                    @RequestParam(value = "path", required = false) String path,
                                    @RequestParam(value = "objectName", required = false) String objectName) {
        String contentType = file.getContentType();
        // 设置参数
        UploadFileParam uploadFileParam = new UploadFileParam();
        uploadFileParam.setFileSize(file.getSize());
        uploadFileParam.setContentType(contentType);
        uploadFileParam.setFilename(file.getOriginalFilename());
        uploadFileParam.setUserId(userId);
        if (contentType.contains("image")) {
            uploadFileParam.setFileType("image");
        } else {
            uploadFileParam.setFileType("video");
        }
        // 调用Service服务
        try {
            return uploadService.uploadImage2Minio(uploadFileParam, file.getBytes(), path, objectName);
        } catch (Exception e) {
            log.error("文件上传过程中出错");
            e.printStackTrace();
        }
        return Result.fail("文件上传过程中出错");
    }

    //---------------------------------------测试接口（已过时）---------------------------------------------------

    /**
     * 上传博客图片；改为 upload2Minio() 接口
     *
     * @param image
     * @return
     */
    @Deprecated
    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
            String fileName = createNewFileName(originalFilename);
            // 保存文件【一般会有文件服务器，这里只上传到本地nginx】
            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
            // 返回结果
            log.debug("文件上传成功，{}", fileName);

            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除博客图片
     *
     * @param filename
     * @return
     */
    @Deprecated
    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

    /**
     * 生成新的文件名
     *
     * @param originalFilename
     * @return
     */
    @Deprecated
    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }
}
