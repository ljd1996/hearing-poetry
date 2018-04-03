package com.poetry.hearing.service;

import com.poetry.hearing.dao.UserMapper;
import com.poetry.hearing.domain.User;
import com.poetry.hearing.domain.UserExample;
import com.poetry.hearing.util.Constant;
import com.poetry.hearing.util.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "user")
public class UserService {
    private Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMapper userMapper;

    @Cacheable(key = "#p1")
    public Msg updateUserInfo(String email, String name, String autograph) {
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        criteria.andEmailEqualTo(email);
        User user = new User();
        user.setName(name);
        user.setAutograph(autograph);
        if (userMapper.updateByExampleSelective(user, example) > 0) {
            return Msg.success().add(Constant.MSG_LOGINING_USER, userMapper.selectByExample(example).get(0));
        }
        return Msg.fail();
    }

    @CachePut(key = "#p0.name")
    public Msg register(User user){
        try {
            userMapper.insert(user);
        } catch (Exception e){
            return Msg.success().add(Constant.LOGIN_REGISTER_STATUS, false);
        }
        redisService.setUserCache(user);
        return Msg.success().add(Constant.LOGIN_REGISTER_STATUS, true).add(Constant.MSG_LOGINING_USER, user);
    }
}
