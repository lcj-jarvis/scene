package com.mrlu.server;

import com.mrlu.server.entity.User;
import com.mrlu.server.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author 简单de快乐
 * @create 2024-04-17 17:18
 */
@SpringBootTest
public class UserMapperTest {


    @Autowired
    private UserMapper userMapper;

    @Test
    public void t1() {
        User user = new User();
        user.setId(System.currentTimeMillis());
        user.setAge(1);
        user.setName(UUID.randomUUID().toString().substring(0,5));
        user.setEmail(user.getName());
        user.setDeleted(0);
        user.setVersion(1);
        userMapper.insertAll(user);
    }

    @Test
    public void t2() throws InterruptedException {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setId(System.currentTimeMillis());
            Thread.sleep(100);
            user.setAge(new Random().nextInt());
            user.setName(UUID.randomUUID().toString().substring(0,5));
            user.setEmail(user.getName());
            user.setDeleted(0);
            user.setVersion(1);
            users.add(user);
        }
        userMapper.insertAllBatch(users);
    }

    @Test
    public void t3() {
        userMapper.softDeleteAll();
    }



}
