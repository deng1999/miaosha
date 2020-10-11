package com.deng.controller;

import com.deng.access.AccessLimit;
import com.deng.controller.result.CodeMsg;
import com.deng.controller.result.Result;
import com.deng.entity.OrderInfo;
import com.deng.entity.SeckillOrder;
import com.deng.entity.SeckillUser;
import com.deng.rabbitmq.MQSender;
import com.deng.rabbitmq.SeckillMessage;
import com.deng.rsdis.GoodsKeyPrefix;
import com.deng.rsdis.RedisService;
import com.deng.service.GoodsService;
import com.deng.service.OrderService;
import com.deng.service.SeckillService;
import com.deng.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀按钮的业务逻辑控制
 * 使用MQ将请求入队
 */
@Controller
@RequestMapping("/miaosha")
public class SeckillController implements InitializingBean {
    @Autowired
    GoodsService goodsService;
    @Autowired
    OrderService orderService;
    @Autowired
    SeckillService seckillService;
    @Autowired
    RedisService redisService;
    @Autowired
    MQSender sender;

    private Map<Long,Boolean> localOverMap=new HashMap<>();
    /**
     * 系统初始化的时候执行
     * 系统初始化的时候从数据库中将商品信息查询出来
     * （包含商品的秒杀信息miaosha_goods和商品的基本信息goods）
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goods = goodsService.listGoodsVo();
        if (goods==null){
            return;
        }
        //将商品库存信息存储到redis中
        for (GoodsVo goodsVo:goods){
            redisService.set(GoodsKeyPrefix.seckillGoodsStockPrefix,""+goodsVo.getId(),goodsVo.getStockCount());
            localOverMap.put(goodsVo.getId(),false);//在系统启动时，标记库存不为空
        }
    }

    /**
     * 秒杀逻辑
     * 用户点击秒杀按钮后的逻辑控制
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping("/do_miaosha")
    public String doMiaosha(Model model, SeckillUser user, @RequestParam("goodsId") Long goodsId){

        model.addAttribute("user",user);

        if (user==null){
            return "login";
        }

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if (stockCount<0){
            model.addAttribute("errmsg", CodeMsg.SECKILL_OVER.getMsg());
            return "miaosha_fail";
        }


        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if (order!=null){
            model.addAttribute("errmsg",CodeMsg.REPEATE_SECKILL.getMsg());
            return "miaosha_fail";
        }

        OrderInfo orderInfo = seckillService.sekill(user, goods);
        model.addAttribute("orderInfo",orderInfo);
        model.addAttribute("goods",goods);
        return "order_detail";
    }

    /**
     *
     * @param model
     * @param user
     * @param goodsId
     * @param path
     * @return
     */
    @RequestMapping(value = "/{path}/do_miaosha_static",method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> doMiaoshaStatic(Model model,SeckillUser user,
                                           @RequestParam("goodsId") Long goodsId,
                                           @PathVariable("path") String path){
        model.addAttribute("user", user);
        // 1. 如果用户为空，则返回登录界面
        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);

        // c6: 验证path是否正确
        boolean check = seckillService.checkPath(user, goodsId, path);
        if (!check)
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 请求非法

        // 通过内存标记，减少对redis的访问，秒杀未结束才继续访问redis
        Boolean over = localOverMap.get(goodsId);
        if (over)
            return Result.error(CodeMsg.SECKILL_OVER);

        // 预减库存
        Long stock = redisService.decr(GoodsKeyPrefix.seckillGoodsStockPrefix, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId, true);// 秒杀结束。标记该商品已经秒杀结束
            return Result.error(CodeMsg.SECKILL_OVER);
        }

        // 判断是否重复秒杀
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }

        // 商品有库存且用户为秒杀商品，则将秒杀请求放入MQ
        SeckillMessage message = new SeckillMessage();
        message.setUser(user);
        message.setGoodsId(goodsId);

        // 放入MQ
        sender.sendMiaoshaMessage(message);
        return Result.success(0);
    }

    /**
     * 用于返回用户秒杀的结果
     *
     * @param model
     * @param user
     * @param goodsId
     * @return orderId：成功, -1：秒杀失败, 0： 排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, SeckillUser user,
                                      @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result= seckillService.getSeckillResult(user.getId(), goodsId);
        return Result.success(result);
    }

    /**
     * 获取秒杀接口地址
     * 每一次点击秒杀，都会生成一个随机的秒杀地址返回给客户端
     * 对秒杀的次数做限制（通过自定义拦截器注解完成）
     *
     * @param model
     * @param user
     * @param goodsId    秒杀的商品id
     * @param verifyCode 验证码
     * @return 被隐藏的秒杀接口路径
     */
    @AccessLimit(seconds = 5, maxAccessCount = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(Model model, SeckillUser user,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode
    ) {

        // 在执行下面的逻辑之前，会相对path请求进行拦截处理（@AccessLimit， AccessInterceptor），防止访问次数过于频繁，对服务器造成过大的压力

        model.addAttribute("user", user);

        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 校验验证码
        boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check)
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 检验不通过，请求非法

        // 检验通过，获取秒杀路径
        String path = seckillService.createSeckillPath(user, goodsId);
        // 向客户端回传随机生成的秒杀地址
        return Result.success(path);
    }


    /**
     * goods_detail.htm: $("#verifyCodeImg").attr("src", "/seckill/verifyCode?goodsId=" + $("#goodsId").val());
     * 使用HttpServletResponse的输出流返回客户端异步获取的验证码（异步获取的代码如上所示）
     *
     * @param response
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response, SeckillUser user,
                                               @RequestParam("goodsId") long goodsId) {
        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);

        // 创建验证码
        try {
            BufferedImage image = seckillService.createVerifyCode(user, goodsId);
            ServletOutputStream out = response.getOutputStream();
            // 将图片写入到resp对象中
            ImageIO.write(image, "JPEG", out);
            out.close();
            out.flush();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.SECKILL_FAIL);
        }
    }

}
