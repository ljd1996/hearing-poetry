package com.poetry.hearing.security;

import com.poetry.hearing.dao.UserMapper;
import com.poetry.hearing.domain.User;
import com.poetry.hearing.domain.UserExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user;
        UserExample userExample = new UserExample();
        UserExample.Criteria criteria = userExample.createCriteria();
        criteria.andNameEqualTo(s);
        List<User> users = userMapper.selectByExample(userExample);

        if (users.size() <= 0) {
            return null;
        } else {
            user = users.get(0);
        }
        return new MyUserDetails(user);
    }
}
