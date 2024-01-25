package com.mirrors.utils;

import cn.hutool.crypto.digest.DigestUtil;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.mirrors.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MinIO相关工具类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 15:29
 */
@Slf4j
public class MinioUtil {

    /**
     * 通过文件路径上传文件到MinIO
     *
     * @param minioClient
     * @param filepath
     * @param bucket
     * @param objectName
     */
    public static void addFile2MinioByPath(MinioClient minioClient, String filepath, String bucket, String objectName) {
        // 扩展名
        String extension = null;
        if (filepath.contains(".")) {
            extension = filepath.substring(filepath.lastIndexOf("."));
        }
        // 获取扩展名对应的媒体类型
        String contentType = getMimeTypeByExtension(extension);
        try {
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(filepath)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwException("上传文件到文件系统出错");
        }
    }

    /**
     * 合并分块时，检查所有分块是否上传完毕并返回所有的分块文件
     *
     * @param minioClient
     * @param fileMd5
     * @param chunkTotal
     * @param bucket
     * @return
     */
    public static File[] mergeCheckChunks(MinioClient minioClient, String fileMd5, int chunkTotal, String bucket) {
        // 得到分块目录路径
        String chunksFolderPath = getChunksFolderPath(fileMd5);
        File[] files = new File[chunkTotal];
        // 检测分块文件是否上传完毕
        for (int i = 0; i < chunkTotal; i++) {
            String chunkPath = chunksFolderPath + i;
            // 从MinIO拿到分块文件
            File chunkFile;
            try {
                chunkFile = File.createTempFile("chunk" + i, null);
            } catch (Exception e) {
                throw new BusinessException("下载分块时创建临时文件出错");
            }
            files[i] = downloadFileFromMinio(minioClient, chunkFile, bucket, chunkPath);
        }
        return files;
    }

    /**
     * 从MinIO拿到分块文件；下载到chunkFile
     *
     * @param minioClient
     * @param chunkFile
     * @param bucket
     * @param chunkPath
     * @return
     */
    public static File downloadFileFromMinio(MinioClient minioClient, File chunkFile, String bucket, String chunkPath) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucket)
                .object(chunkPath)
                .build();
        // 拿到文件输入流
        try (InputStream inputStream = minioClient.getObject(args)) {
            // 输出流下载
            try (OutputStream outputStream = Files.newOutputStream(chunkFile.toPath())) {
                IOUtils.copy(inputStream, outputStream);
            } catch (Exception e) {
                BusinessException.throwException("下载文件" + chunkPath + "出错");
            }

        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwException("文件不存在 " + chunkPath);
        }
        return chunkFile;
    }

    /**
     * 根据文件md5值得到分块文件的目录
     *
     * @param fileMd5 文件md5值
     * @return 分块路径
     */
    public static String getChunksFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    /**
     * 根据文件md5值获取文件绝对路径
     *
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件绝对路径
     */
    public static String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 根据扩展名得到对应的媒体类型
     *
     * @param extension 文件扩展名
     * @return 对应的媒体类型
     */
    public static String getMimeTypeByExtension(String extension) {
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (StringUtils.hasText(extension)) {
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
            if (extensionMatch != null) {
                contentType = extensionMatch.getMimeType();
            }
        }
        return contentType;
    }

    /**
     * 上传文件到MinIO
     *
     * @param fileBytes
     * @param bucket
     * @param objectName
     * @return
     */
    public static void addFile2Minio(MinioClient minioClient, byte[] fileBytes, String bucket, String objectName) {
        try {
            // 获取 contentType
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // 未知的二进制流
            if (objectName.contains(".")) {
                // 如果文件名有扩展名，取 objectName 中的扩展名
                String extension = objectName.substring(objectName.lastIndexOf("."));
                ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
                if (extensionMatch != null) {
                    contentType = extensionMatch.getMimeType();
                }
            }
            // 创建 输入流
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            // 参数信息
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName) // 子目录
                    .stream(inputStream, inputStream.available(), -1) // -1 表示文件分片按 5M(不小于5M,不大于5T)，分片数量最大 10000
                    .contentType(contentType)
                    .build();
            // 上传
            minioClient.putObject(args);
        } catch (Exception e) {
            log.error("上传文件出错", e);
        }
    }

    /**
     * 根据日期拼接目录（）
     *
     * @param date
     * @param year
     * @param month
     * @param day
     * @return
     */
    public static String getImageFolderPathByData(Date date, boolean year, boolean month, boolean day) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // 获取当前日期字符串
        String dateString = simpleDateFormat.format(date);
        // 取出年、月、日
        String[] dateStringArray = dateString.split("-");
        // 拼接
        StringBuilder folderString = new StringBuilder();
        if (year) {
            folderString.append(dateStringArray[0]).append("/");
        }
        if (month) {
            folderString.append(dateStringArray[1]).append("/");
        }
        if (day) {
            folderString.append(dateStringArray[2]).append("/");
        }
        return folderString.toString();
    }
}
