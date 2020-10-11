package com.deng.access;

import com.alibaba.fastjson.JSON;
import com.deng.controller.result.CodeMsg;
import com.deng.controller.result.Result;
import com.deng.entity.SeckillUser;
import com.deng.rsdis.AccessKeyPrefix;
import com.deng.rsdis.RedisService;
import com.deng.service.SeckillUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户访问拦截器，限制用户对某一接口的频繁访问
 */
@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    SeckillUserService userService;

    @Autowired
    RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod){
            SeckillUser user= this.getUser(request, response);
            UserContext.setUser(user);
            HandlerMethod hm=(HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);

            if (accessLimit==null){
                return true;
            }
            // 获取注解的元素值
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxAccessCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    this.render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            } else {
                //do nothing
            }
            // 设置过期时间
            AccessKeyPrefix accessKeyPrefix = AccessKeyPrefix.withExpire(seconds);
            // 在redis中存储的访问次数的key为请求的URI
            Integer count = redisService.get(accessKeyPrefix, key, Integer.class);
            // 第一次重复点击秒杀
            if (count == null) {
                redisService.set(accessKeyPrefix, key, 1);
                // 点击次数为未达最大值
            } else if (count < maxCount) {
                redisService.incr(accessKeyPrefix, key);
                // 点击次数已满
            } else {
                this.render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
        }

    /**
     * 点击次数满后，向客户端反馈一个“频繁请求”提示信息
     * @param response
     * @param cm
     * @throws IOException
     */
    private void render(HttpServletResponse response, CodeMsg cm) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = response.getOutputStream();
        String string = JSON.toJSONString(Result.error(cm));
        outputStream.write(string.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

    }


    /**
     * 和UserArgumentResolver功能类似，用于解析拦截的请求，获取MiaoshaUser对象
     * @param request
     * @param response
     * @return
     */
    private SeckillUser getUser(HttpServletRequest request, HttpServletResponse response) {

        // 从请求中获取token
        String paramToken = request.getParameter(SeckillUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, SeckillUserService.COOKIE_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        return userService.getMisaoshaUserByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request, String cookieNameToken) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieNameToken)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
