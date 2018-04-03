package com.poetry.hearing.service;

import com.poetry.hearing.domain.User;
import com.poetry.hearing.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Set;

@Service
public class RedisService {

    private Logger logger = LoggerFactory.getLogger(RedisService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void setCache(String key, String value){
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        if (redisTemplate.hasKey(key)){
            redisTemplate.delete(key);
        }
        operations.set(key, value);
    }

    public String getCacheByKey(String key){
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    public void updateCache(String key, String value){
        if (redisTemplate.hasKey(key)){
            redisTemplate.delete(key);
            setCache(key, value);
        }
    }

    public void deleteCache(String keyPattern){
        Set<String> keys = redisTemplate.keys(keyPattern);
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            if (redisTemplate.hasKey(key)){
                redisTemplate.delete(key);
            }
        }
    }

    /**
     * 用户缓存
     * 用户缓存：key-->login_user,value-->用户邮箱
     * 用户缓存hash表：hash-->hash_user_[用户邮箱],key-->user_name，value-->用户名；key-->user_passwd,value-->用户密码
     * @return
     */
    public User getUserCache(){
        if (getCacheByKey(Constant.KEY_HAS_USER_CACHE) == null){
            return null;
        }
        User user = new User();
        HashOperations<String, String, String> userHash = redisTemplate.opsForHash();
        user.setName(userHash.get(Constant.KEY_USER_PRE + getCacheByKey(Constant.KEY_HAS_USER_CACHE),
                Constant.KEY_USER_NAME));
        user.setPasswd(userHash.get(Constant.KEY_USER_PRE + getCacheByKey(Constant.KEY_HAS_USER_CACHE),
                Constant.KEY_USER_PASSWD));
        user.setEmail(getCacheByKey(Constant.KEY_HAS_USER_CACHE));
        if (user.getName() == null || user.getPasswd() == null || user.getEmail() == null){
            return null;
        }
        return user;
    }

    public void setUserCache(User user){
        setCache(Constant.KEY_HAS_USER_CACHE, user.getEmail());
        if (redisTemplate.hasKey(Constant.KEY_USER_PRE + user.getEmail())){
            redisTemplate.delete(Constant.KEY_USER_PRE + user.getEmail());
        }
        HashOperations<String, String, String> userHash = redisTemplate.opsForHash();
        userHash.put(Constant.KEY_USER_PRE + user.getEmail(), Constant.KEY_USER_NAME, user.getName());
        userHash.put(Constant.KEY_USER_PRE + user.getEmail(), Constant.KEY_USER_PASSWD, user.getPasswd());
    }

    public void clearUserCache(){
        deleteCache(Constant.KEY_USER_PRE + "*");
        deleteCache(Constant.KEY_HAS_USER_CACHE);
    }
}
