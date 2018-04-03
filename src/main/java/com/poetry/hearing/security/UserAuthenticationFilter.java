package com.poetry.hearing.security;

import com.poetry.hearing.domain.User;
import com.poetry.hearing.util.TokenDetailImpl;
import com.poetry.hearing.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UserAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    /**
     * json web token 在请求头的名字
     */
    @Value("${token.header}")
    private String tokenHeader;

    /**
     * 辅助操作 token 的工具类
     */
    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private MyAuthenticationProvider myAuthenticationProvider;

    public UserAuthenticationFilter() {
        this.setAuthenticationSuccessHandler(new LoginSuccessHandler());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("attemptAuthentication...");
        User user = new User();
        String name = request.getParameter("name");
        String password  = request.getParameter("passwd");
        if (name != null && password != null) {
            user.setName(name);
            user.setPasswd(password);
        } else {
            return null;
        }
        UserDetails userDetails = new MyUserDetails(user);
        return myAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userDetails, null));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        System.out.println("successfulAuthentication..." + authResult.getName());
        response.addHeader(this.tokenHeader, tokenUtils.generateToken(new TokenDetailImpl(authResult.getName(),
                ((MyUserDetails)authResult.getPrincipal()).getPassword())));
//        SecurityContextHolder.getContext().setAuthentication(authResult);
        super.successfulAuthentication(request, response, chain, authResult);
    }
}
