package com.mirrors;

import ch.qos.logback.core.util.ContentTypeUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * MinIO测试类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 10:00
 */
public class MinioTest {


    /**
     * 连接
     */
    static MinioClient minioClient = MinioClient.builder()
            .endpoint("http://192.168.101.130:9800") // 注意区分 管理界面端口 还是 服务端口
            .credentials("minioadmin", "minioadmin")
            .build();

    /**
     * 测试上传
     */
    @Test
    public void testUpload() throws Exception {
        // 通过扩展名拿到 mimetype
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        // 参数信息
        UploadObjectArgs args = UploadObjectArgs.builder()
                .bucket("testbucket")
                .filename("C:\\Users\\24180\\Pictures\\Camera Roll\\2023-10-24.png")
                .object("test1/2023-10-24.png") // 子目录
                .contentType(mimeType)
                .build();
        // 上传
        minioClient.uploadObject(args);
    }

    /**
     * 测试删除
     */
    @Test
    public void testDelete() throws Exception {
        // 参数信息
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket("testbucket")
                .object("test1/2023-10-24.png")
                .build();
        // 删除
        minioClient.removeObject(args);
    }

    /**
     * 测试查询下载
     */
    @Test
    public void testGetFile() throws Exception {
        // 参数信息
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test1/2023-10-24.png")
                .build();
        // 下载
        FilterInputStream inputStream = minioClient.getObject(args);
        FileOutputStream outputStream = new FileOutputStream("C:\\JavaProjects\\2-点评项目\\project\\download\\2023-10-24.png");
        IOUtils.copy(inputStream, outputStream);
    }

}
