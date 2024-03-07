package com.mirrors.test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.User;
import com.mirrors.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mirrors.utils.RedisConstants.LOGIN_USER_KEY;
import static com.mirrors.utils.RedisConstants.LOGIN_USER_TTL_MINUTES;

@SpringBootTest
public class GetAllTokenTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Test
    public void getAllToken() throws IOException {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());

        // 修改 文件位置
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:\\JavaProjects\\2-点评项目\\project\\entertainment-mirrors\\data\\tokens.txt"));

        users.stream().forEach(user -> {
            String token = UUID.randomUUID().toString(true);
            // 生成token文件
            try {

                bufferedWriter.write(token);
                bufferedWriter.newLine(); //【使用newline】避免自己给字符添加 \n 导致key中带有\n的字符

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 同时写入redis，保存UserDTO
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> map = BeanUtil.beanToMap(
                    userDTO,
                    new HashMap<>(),
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> {
                                return fieldValue.toString(); // 将其他类型 全部转为String
                            })
            );
            stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL_MINUTES, TimeUnit.MINUTES);
        });

        bufferedWriter.close();

        System.out.println(users);
    }

}
