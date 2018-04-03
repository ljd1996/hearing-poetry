package com.poetry.hearing.security;

import com.poetry.hearing.domain.User;
import com.poetry.hearing.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenAuthenticationFilter extends BasicAuthenticationFilter {

    @Value("${token.header}")
    private String tokenHeader;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private MyAuthenticationProvider myAuthenticationProvider;

    public TokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String authToken = request.getHeader(this.tokenHeader);
        if (authToken != null) {
            String username = this.tokenUtils.getUsernameFromToken(authToken);
            String password = this.tokenUtils.getPasswordFromToken(authToken);

            // 如果上面解析 token 成功并且拿到了 username 并且本次会话的权限还未被写入 且token有效
            if (username != null && password != null && SecurityContextHolder.getContext().getAuthentication()
                    == null && this.tokenUtils.validateToken(authToken)) {
                User user = new User();
                user.setName(username);
                user.setPasswd(password);
                UserDetails userDetails = new MyUserDetails(user);
                SecurityContextHolder.getContext().setAuthentication(myAuthenticationProvider.
                        authenticate(new UsernamePasswordAuthenticationToken(userDetails, null)));
            }
        }
        chain.doFilter(request, response);
    }
}
