package com.poetry.hearing.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration          // 声明为配置类
@EnableWebSecurity      // 启用 Spring Security web 安全的功能
@EnableGlobalMethodSecurity(prePostEnabled = true)//允许进入页面方法前检验
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

    @Bean
    UserAuthenticationFilter authenticationTokenFilter() throws Exception {
        UserAuthenticationFilter userAuthenticationFilter = new UserAuthenticationFilter();
        userAuthenticationFilter.setAuthenticationManager(authenticationManager());
        return userAuthenticationFilter;
    }

    @Bean
    TokenAuthenticationFilter tokenAuthenticationFilter() throws Exception {
        return new TokenAuthenticationFilter(authenticationManager());
    }

    @Bean
    LoginSuccessHandler loginSuccessHandler() {
        return new LoginSuccessHandler();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()                     // 禁用 Spring Security 自带的跨域处理;
                .authorizeRequests()
                .antMatchers("/myself").authenticated()           // 需携带有效 token
                .antMatchers("/article").authenticated()           // 需携带有效 token
                .anyRequest().permitAll()
                .and()
                .formLogin().loginPage("/registerOrLogin").loginProcessingUrl("/hearing")
                    .successHandler(loginSuccessHandler())
                .and()
                .logout().clearAuthentication(true).invalidateHttpSession(true).logoutUrl("/logout")
                    .logoutSuccessUrl("/hearing")
                .and()
                .addFilter(authenticationTokenFilter())
                .addFilter(tokenAuthenticationFilter());
    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        System.out.println("password...");
//        auth.userDetailsService(new MyUserDetailsService()).passwordEncoder(new BCryptPasswordEncoder());
//    }
}
