package com.mirrors.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 上传文件的参数，由Controller层生成，传给Service层
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 15:10
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileParam {
    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件content-type
     */
    private String contentType;

    /**
     * 文件类型（文档，音频，视频）
     */
    private String fileType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 用户id
     */
    private Long userId;
}
