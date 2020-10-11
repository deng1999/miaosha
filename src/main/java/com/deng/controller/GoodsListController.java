package com.deng.controller;

import com.deng.controller.result.Result;
import com.deng.entity.SeckillUser;
import com.deng.rsdis.GoodsKeyPrefix;
import com.deng.rsdis.RedisService;
import com.deng.service.GoodsService;
import com.deng.service.SeckillUserService;
import com.deng.vo.GoodsDetailVo;
import com.deng.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsListController {
    @Autowired
    SeckillUserService seckillUserService;

    @Autowired
    GoodsService goodsService;
    @Autowired
    RedisService redisService;
    // 因为在redis缓存中不存页面缓存时需要手动渲染，所以注入一个视图解析器，自定义渲染（默认是由SpringBoot完成的）
    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * 获取秒杀的对象SeckillUser,并将其传递到页面解析器
     * 从数据库中获取商品信息（包含秒杀信息）
     * @param response
     * @param request
     * @param model 响应的资源文件
     * @param user  通过自定义参数解析器UserArgumentResolver解析的 SeckillUser 对象
     * @return
     */
    @RequestMapping(value = "/to_list")//
    @ResponseBody// produces表明：这个请求会返回text/html媒体类型的数据
    public String toList(HttpServletResponse response,
                         HttpServletRequest request,
                         Model model,
                         SeckillUser user){
        model.addAttribute("user",user);
         //1.从redis缓存中读取html
        String html = redisService.get(GoodsKeyPrefix.goodsListKeyPrefix, "", String.class);

        if (!StringUtils.isEmpty(html))
            return html;
        //2.如果redis中不存在该缓存，则需要手动渲染
        // 查询商品列表，用于手动渲染时将商品数据填充到页面
        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsVoList);
          //3.渲染html
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        // (第一个参数为渲染的html文件名，第二个为web上下文：里面封装了web应用的上下文)
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", webContext);

        if (!StringUtils.isEmpty(html)) // 如果html文件不为空，则将页面缓存在redis中
            redisService.set(GoodsKeyPrefix.goodsListKeyPrefix, "", html);

        return html;


    }

    /**
     *处理商品详情页（做了页面静态化处理）
     *
     * 缓存实现：从redis中取出商品详情页面，如果没有则需要手动渲染页面，并且将渲染的页面存储在redis中供下一次访问时获取
     * 实际上URL级缓存和页面及缓存是一样的，只不过URL级缓存会根据url的参数从redis中取不同的数据
     * @param request
     * @param response
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/to_detail/{goodsId}",produces = "text/html")
    @ResponseBody
    public String toDetail(HttpServletRequest request,
                       HttpServletResponse response,
                       Model model,
                       SeckillUser user,
                       @PathVariable("goodsId") long goodsId){
        //1.根据商品id从redis中取详情数据的缓存
    String html = redisService.get(GoodsKeyPrefix.goodsDetailKeyPrefix, "" + goodsId, String.class);
    if (!StringUtils.isEmpty(html)){//如果缓存中存在数据直接返回
        return html;
    }
    //2.如果缓存中数据不存在，则需要手动渲染详情界面数据并返回
        model.addAttribute("user",user);
        //通过商品id查询
        GoodsVo goodsVoByGoodsId = goodsService.getGoodsVoByGoodsId(goodsId);
         model.addAttribute("goods",goodsVoByGoodsId);

         //获取商品的秒杀开始与结束的时间

        long startDate = goodsVoByGoodsId.getStartDate().getTime();
        long endDate = goodsVoByGoodsId.getEndDate().getTime();
        long now = System.currentTimeMillis();
        //秒杀状态；0：秒杀未开始，1：秒杀进行中；2：秒杀已结束
        int miaoshaStatus=0;
        //秒杀剩余时间
        int remainSeconds=0;

        if (now<startDate){//秒杀未开始
            miaoshaStatus=0;
            remainSeconds=(int)((startDate-now)/1000);//剩余时间秒为单位
        }else if (now>startDate){
            miaoshaStatus=2;
            remainSeconds=-1;
        }else {//秒杀进行中
            miaoshaStatus=1;
            remainSeconds=0;
        }
        //将秒杀状态和秒杀剩余时间传递给页面（goods_detail）
        model.addAttribute("miaoshaStatus",miaoshaStatus);
        model.addAttribute("remainSeconds",remainSeconds);
        //3.渲染html
        WebContext webContext=new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        //(第一个参数为渲染的html文件名，第二个为web上下文，里面封装web应用的上下文)
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", webContext);
        if (!StringUtils.isEmpty(html)) //如果html文件不为空，则将页面缓存在redis中
            redisService.set(GoodsKeyPrefix.goodsDetailKeyPrefix,""+goodsId,html);

        return html;

    }
    @RequestMapping(value = "/to_detail_static/{goodsId}")// 注意这种写法
//    @RequestMapping(value = "/to_detail/{goodsId}")// 注意这种写法
    @ResponseBody
    public Result<GoodsDetailVo> toDetailStatic(SeckillUser user,@PathVariable("goodsId") Long goodsId){
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
       //获取商品的秒杀开始与结束时间
        long startDate = goods.getStartDate().getTime();
        long endDate = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus=0;

        int remainSeconds=0;

        if (now<startDate){
            miaoshaStatus=0;
            remainSeconds=(int)((startDate-now)/1000);
        }else if (now>endDate){
            miaoshaStatus=2;
            remainSeconds=-1;
        }else {
            miaoshaStatus=1;
            remainSeconds=0;
        }

        //服务端封装商品数据直接传给客户端，而不用渲染界面
        GoodsDetailVo goodsDetailVo=new GoodsDetailVo();
        goodsDetailVo.setGoods(goods);
        goodsDetailVo.setUser(user);
        goodsDetailVo.setRemainSeconds(remainSeconds);
        goodsDetailVo.setSeckillStatus(miaoshaStatus);

        return Result.success(goodsDetailVo);
    }

}
