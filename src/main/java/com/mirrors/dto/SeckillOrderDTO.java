package com.mirrors.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀订单的基本信息实体类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 15:22
 */
@Data
@Builder
public class SeckillOrderDTO {

    private Integer userId;

    private Integer voucherId;

    private Long orderId;
}
