package com.deng.controller;


import com.deng.controller.result.CodeMsg;
import com.deng.controller.result.Result;
import com.deng.entity.OrderInfo;
import com.deng.entity.SeckillUser;
import com.deng.service.GoodsService;
import com.deng.service.OrderService;
import com.deng.vo.GoodsVo;
import com.deng.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * c5：订单详情页面静态化
 */
@Controller
@RequestMapping("order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    /**
     * 获取订单详情
     *
     * @param model
     * @param user
     * @param orderId
     * @return
     */
    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> orderInfo(Model model,
                                           SeckillUser user,
                                           @RequestParam("orderId") long orderId) {

        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 获取订单信息
        OrderInfo order = orderService.getOrderById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }

        // 如果订单存在，则根据订单信息获取商品信息
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(order);// 设置订单信息
        vo.setGoods(goods);// 设置商品信息
        return Result.success(vo);
    }
}
