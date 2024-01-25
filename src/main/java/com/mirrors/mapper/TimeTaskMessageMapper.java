package com.mirrors.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mirrors.entity.TimeTaskMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/1/18 10:26
 */
public interface TimeTaskMessageMapper extends BaseMapper<TimeTaskMessage> {

    @Select("SELECT t.* FROM tb_message t " +
            "WHERE t.id % #{shardTotal} = #{shardIndex} " +
            "and t.state = '0' " +
            "and t.message_type = #{messageType} " +
            "limit #{count}")
    List<TimeTaskMessage> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                                 @Param("shardIndex") int shardIndex,
                                                 @Param("messageType") String messageType,
                                                 @Param("count") int count);
}
