package com.poetry.hearing.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class MyAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        MyUserDetails user = (MyUserDetails) userDetailsService.loadUserByUsername(userDetails.getUsername());
        if(user == null){
            throw new BadCredentialsException("Username not found.");
        }

        //加密过程在这里体现
        if (!userDetails.getPassword().equals(user.getPassword())) {
            throw new BadCredentialsException("Wrong password.");
        }

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        return new UsernamePasswordAuthenticationToken(user, userDetails.getPassword(), authorities);
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
