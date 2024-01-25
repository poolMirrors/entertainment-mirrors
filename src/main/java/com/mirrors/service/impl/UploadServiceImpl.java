package com.mirrors.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.dto.Result;
import com.mirrors.entity.ShopType;
import com.mirrors.entity.UploadFile;
import com.mirrors.entity.UploadFileParam;
import com.mirrors.exception.BusinessException;
import com.mirrors.exception.BusinessExceptionHandler;
import com.mirrors.mapper.ShopTypeMapper;
import com.mirrors.mapper.UploadFileMapper;
import com.mirrors.service.IShopTypeService;
import com.mirrors.service.IUploadService;
import com.mirrors.utils.MinioUtil;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.List;

/**
 * 分布式存储 服务实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 11:05
 */
@Slf4j
@Service
public class UploadServiceImpl extends ServiceImpl<UploadFileMapper, UploadFile> implements IUploadService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.media-files}")
    private String mediaFilesBucket;

    @Value("${minio.bucket.video-files}")
    private String videoFilesBucket;

    //---------------------------------------上传大文件视频到MinIO-----------------------------------------------

    /**
     * 合并分块
     *
     * @param fileMd5
     * @param chunkTotal
     * @param uploadFileParam
     * @return
     */
    @Override
    public Result mergeChunks(String fileMd5, int chunkTotal, UploadFileParam uploadFileParam) {
        // 拿到所有分块
        File[] chunkFiles = MinioUtil.mergeCheckChunks(minioClient, fileMd5, chunkTotal, videoFilesBucket);
        // 文件扩展名
        String filename = uploadFileParam.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        // 创建临时合并文件
        File mergeFile;
        try {
            mergeFile = File.createTempFile(fileMd5, extension);
        } catch (Exception e) {
            throw new BusinessException("合并文件过程中创建临时文件出错");
        }
        // 合并流程
        try {
            //（1）开始合并
            byte[] buffer = new byte[1024];
            try (RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw")) {
                // 遍历分块
                for (File chunkFile : chunkFiles) {
                    FileInputStream inputStream = new FileInputStream(chunkFile);
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        // 写入临时文件
                        rw.write(buffer, 0, len);
                    }
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessException("合并文件过程中出错");
            }
            log.debug("合并文件完成{}", mergeFile.getAbsolutePath());
            uploadFileParam.setFileSize(mergeFile.length());
            //（2）校验文件内容，通过 md5 对比
            try (FileInputStream inputStream = new FileInputStream(mergeFile)) {
                String md5Hex = DigestUtil.md5Hex(inputStream);
                if (!fileMd5.equalsIgnoreCase(md5Hex)) {
                    throw new BusinessException("合并文件校验失败");
                }
                log.debug("合并文件校验通过 {}", mergeFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessException("合并文件校验异常");
            }
            //（3）合并临时文件重新上传到MinIO
            String mergeFilePath = MinioUtil.getFilePathByMd5(fileMd5, extension);
            try {
                MinioUtil.addFile2MinioByPath(minioClient, mergeFile.getAbsolutePath(), videoFilesBucket, mergeFilePath);
                log.debug("合并文件上传 MinIO 完成：{}", mergeFile.getAbsolutePath());
            } catch (Exception e) {
                throw new BusinessException("合并文件时上传文件出错");
            }
            //（4）上传到数据库
            IUploadService uploadService = (IUploadService) AopContext.currentProxy();
            UploadFile uploadFile = uploadService.addFile2Db(fileMd5, uploadFileParam, videoFilesBucket, mergeFilePath);
            if (uploadFile == null) {
                throw new BusinessException("文件入库出错");
            }
            return Result.ok();
        } finally {
            // 删除临时文件
            for (File chunkFile : chunkFiles) {
                chunkFile.delete();
            }
            mergeFile.delete();
            log.debug("临时文件清理完毕");
        }
    }

    /**
     * 上传分块
     *
     * @param fileMd5
     * @param chunk
     * @param bytes
     * @return
     */
    @Override
    public Result uploadChunks(String fileMd5, int chunk, byte[] bytes) {
        // 得到分块文件的目录路径
        String chunksFolderPath = MinioUtil.getChunksFolderPath(fileMd5);
        // 得到分块文件的路径
        String chunksPath = chunksFolderPath + chunk;
        // 存储到Minio
        try {
            MinioUtil.addFile2Minio(minioClient, bytes, mediaFilesBucket, chunksPath);
            log.info("上传分块成功：{}", chunksPath);
            return Result.ok("上传分块成功");
        } catch (Exception e) {
            log.error("上传分块文件：{}，失败。", chunksPath, e);
        }
        return Result.fail("上传分块失败");
    }

    /**
     * 检测分块
     *
     * @param fileMd5
     * @param chunkIdx
     * @return
     */
    @Override
    public Result checkChunks(String fileMd5, int chunkIdx) {
        // 分块文件所在目录
        String chunksFolderPath = MinioUtil.getChunksFolderPath(fileMd5);
        // 分块文件的路径
        String chunksPath = chunksFolderPath + chunkIdx;
        // 查询文件系统是否存在
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(videoFilesBucket)
                .object(chunksPath)
                .build();
        try {
            InputStream inputStream = minioClient.getObject(args);
            if (inputStream == null) {
                // 文件不存在
                return Result.ok("文件不存在，可以上传");
            }
        } catch (Exception e) {
            // 文件不存在
            return Result.ok("文件不存在，可以上传");
        }
        // 文件已存在
        return Result.ok("文件已经存在，不需要重复上传");
    }

    /**
     * 检测文件是否完整
     *
     * @param fileMd5
     * @return
     */
    @Override
    public Result checkFile(String fileMd5) {
        // 在文件表中存在，并且在文件系统中存在，此文件才存在
        List<UploadFile> files = query().eq("id", fileMd5).list();
        if (files == null || files.size() == 0) {
            return Result.ok("文件不存在，可以上传");
        }
        // 查询文件系统中是否存在
        UploadFile uploadFile = files.get(0);
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(uploadFile.getBucket())
                .object(uploadFile.getFilePath())
                .build();
        try {
            InputStream inputStream = minioClient.getObject(args);
            if (inputStream == null) {
                // 文件不存在
                return Result.ok("文件不存在，可以上传");
            }
        } catch (Exception e) {
            // 文件不存在
            return Result.ok("文件不存在，可以上传");
        }
        // 文件已存在
        return Result.ok("文件已经存在，不需要重复上传");
    }


    //---------------------------------------上传图片到MinIO----------------------------------------------------

    /**
     * 上传文件到Minio
     *
     * @param uploadFileParam
     * @param fileBytes
     * @param path
     * @param objectName
     * @return
     */
    @Override
    public Result uploadImage2Minio(UploadFileParam uploadFileParam, byte[] fileBytes, String path, String objectName) {
        // 如果为空，就使用默认路径；不为空，则路径最后要加上"/"
        if (!StringUtils.hasText(path)) {
            path = MinioUtil.getImageFolderPathByData(new Date(), true, true, true);
        } else if (!path.contains("/")) {
            path += "/";
        }
        // 获取文件MD5值
        String md5Hex = DigestUtil.md5Hex(fileBytes);
        // 得到文件名
        String filename = uploadFileParam.getFilename();
        // 构造 objectName
        if (!StringUtils.hasText(objectName)) {
            objectName = md5Hex + filename.substring(filename.lastIndexOf("."));
        }
        objectName = path + objectName;
        // 上传
        try {
            // 保存到MinIO
            MinioUtil.addFile2Minio(minioClient, fileBytes, mediaFilesBucket, objectName);
            // 保存文件信息到数据库
            IUploadService uploadService = (IUploadService) AopContext.currentProxy();
            uploadService.addFile2Db(md5Hex, uploadFileParam, mediaFilesBucket, objectName);

            return Result.ok("上传文件成功");
        } catch (Exception e) {
            log.error("文件上传失败", e);
        }
        return Result.fail("上传文件失败");
    }

    /**
     * 将文件信息存入数据库中
     *
     * @param md5Hex          fileId
     * @param uploadFileParam
     * @param bucket
     * @param objectName      对象名
     */
    @Override
    @Transactional
    public UploadFile addFile2Db(String md5Hex, UploadFileParam uploadFileParam, String bucket, String objectName) {
        // 扩展名
        String extension = null;
        if (objectName.contains(".")) {
            extension = objectName.substring(objectName.lastIndexOf("."));
        }
        // 获取扩展名对应的媒体类型
        String contentType = MinioUtil.getMimeTypeByExtension(extension);
        // 从数据库查询文件
        int count = query().eq("id", md5Hex).count();
        // 没查到就写入数据库
        if (count <= 0) {
            UploadFile uploadFile = new UploadFile();
            // 拷贝基本信息
            uploadFile.setId(md5Hex);
            uploadFile.setFileId(md5Hex);
            uploadFile.setUserId(uploadFileParam.getUserId());
            uploadFile.setFilePath(objectName);
            // 图片、mp4视频可以直接设置url
            if (contentType.contains("image") || contentType.contains("mp4")) {
                uploadFile.setUrl("/" + bucket + "/" + objectName);
            }
            uploadFile.setBucket(bucket);
            // 插入数据库
            boolean save = save(uploadFile);
            if (!save) {
                BusinessException.throwException("文件信息保存失败");
            }
            // TODO 若要对视频进行转码，添加到 数据库的待处理任务表

            return uploadFile;
        }
        return null;
    }
}
