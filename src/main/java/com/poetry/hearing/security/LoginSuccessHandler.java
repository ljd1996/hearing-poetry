package com.poetry.hearing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();

    public LoginSuccessHandler() {

    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        System.out.println("LoginSuccessHandler...");
        String url = null;
        // 从别的请求页面跳转过来的情况，savedRequest不为空
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            url = savedRequest.getRedirectUrl();
            System.out.println("url:" + url);
        }

        if (url == null) {
            System.out.println("url==null");
            getRedirectStrategy().sendRedirect(request, response, "/");
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
