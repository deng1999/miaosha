package com.deng.controller;

import com.deng.controller.result.Result;
import com.deng.entity.SeckillUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用于压测的controller,只有一个返回用户信息的功能
 * 这样，请求的压力会全部集中在数据库
 */
@Controller
@RequestMapping("/")
public class UserController {
    /**
     * 返回用户信息
     * @param user
     * @return
     */
    @RequestMapping("/user_info")
    @ResponseBody
    public Result<SeckillUser> userInfo(SeckillUser user){
        return Result.success(user);
    }
}
