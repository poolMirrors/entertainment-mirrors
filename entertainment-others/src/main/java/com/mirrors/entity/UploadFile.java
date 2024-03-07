package com.mirrors.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 保存到数据库的上传文件信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 21:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_upload_file")
public class UploadFile implements Serializable {

    /**
     * 主键；默认ASSIGN_ID（雪花算法）；一般文件MD5值；与fileId一致
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文件标识；一般文件MD5值
     */
    @TableField(value = "file_id")
    private String fileId;

    /**
     * 媒资文件访问地址
     */
    private String url;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件类型（文档，音频，视频）
     */
    @TableField(value = "file_type")
    private String fileType;

    /**
     * 存储目录
     */
    private String bucket;

    /**
     * 存储路径 <=> objectName(minio)
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 文件大小
     */
    @TableField(value = "file_size")
    private Long fileSize;
}
