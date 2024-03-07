package com.mirrors.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirrors.dto.Result;
import com.mirrors.entity.UploadFile;
import com.mirrors.entity.UploadFileParam;

/**
 * 分布式存储
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 11:04
 */
public interface IUploadService extends IService<UploadFile> {

    /**
     * 上传文件
     *
     * @param uploadFileParam
     * @param bytes
     * @param path
     * @param objectName
     * @return
     */
    Result uploadImage2Minio(UploadFileParam uploadFileParam, byte[] bytes, String path, String objectName);

    /**
     * 将文件信息存入数据库中
     *
     * @param md5Hex
     * @param uploadFileParam
     * @param mediaFilesBucket
     * @param objectName
     * @return
     */
    UploadFile addFile2Db(String md5Hex, UploadFileParam uploadFileParam, String mediaFilesBucket, String objectName);

    /**
     * 合并分块
     *
     * @param fileMd5
     * @param chunkTotal
     * @param uploadFileParam
     * @return
     */
    Result mergeChunks(String fileMd5, int chunkTotal, UploadFileParam uploadFileParam);

    /**
     * 上传分块
     *
     * @param fileMd5
     * @param chunk
     * @param bytes
     * @return
     */
    Result uploadChunks(String fileMd5, int chunk, byte[] bytes);

    /**
     * 检测分块
     *
     * @param fileMd5
     * @param chunk
     * @return
     */
    Result checkChunks(String fileMd5, int chunk);

    /**
     * 检测文件是否完整
     *
     * @param fileMd5
     * @return
     */
    Result checkFile(String fileMd5);
}
