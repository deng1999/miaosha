package com.deng.service;

import com.deng.dao.OrderDao;
import com.deng.entity.OrderInfo;
import com.deng.entity.SeckillGoods;
import com.deng.entity.SeckillOrder;
import com.deng.entity.SeckillUser;
import com.deng.rsdis.OrderKeyPrefix;
import com.deng.rsdis.RedisService;
import com.deng.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {
    @Autowired
    OrderDao orderDao;
    @Autowired
    RedisService redisService;

    /**
     *通过用户id与商品id从订单列表中获取订单信息，这个地方用到唯一索引（unique index!!!!!）
     * 优化，不同每次都去数据库读取秒杀订单信息，而是在第一次生成秒杀订单成功后，
     * 将订单存储在redis中，再次读取订单信息的时候就直接从redis中读取
     *
     * @param userId
     * @param goodsId
     * @return
     */
    public SeckillOrder getSeckillOrderByUserIdAndGoodsId(Long userId,Long goodsId){
        //从redis中取缓存，减少数据库访问
        SeckillOrder seckillOrder = redisService.get(OrderKeyPrefix.getSeckillOrderByUidGid, ":" + userId + "_" + goodsId, SeckillOrder.class);
        if (seckillOrder!=null){
            return seckillOrder;
        }
        return orderDao.getSeckillOrderByUserIdAndGoodsId(userId,goodsId);
    }

    /**获取订单信息
     *
     * @param orderId
     * @return
     */
    public OrderInfo getOrderById(long orderId){
        return orderDao.getOrderById(orderId);
    }

    /**
     * 创建订单
     * 增加redis缓存
     *
     * @param user
     * @param goodsVo
     * @return
     */
    @Transactional
    public OrderInfo createOrder(SeckillUser user, GoodsVo goodsVo){
        OrderInfo orderInfo=new OrderInfo();

        SeckillOrder seckillOrder=new SeckillOrder();

        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goodsVo.getId());
        orderInfo.setGoodsName(goodsVo.getGoodsName());
        orderInfo.setGoodsPrice(goodsVo.getGoodsPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());
        //将订单信息插入order_info表
        long insert = orderDao.insert(orderInfo);
        seckillOrder.setGoodsId(goodsVo.getId());
        seckillOrder.setOrderId(orderInfo.getId());
        seckillOrder.setUserId(user.getId());

        //将秒杀订单插入seckill_order表中
        orderDao.insertSeckillOrder(seckillOrder);
        //将秒杀信息订单信息存储于redis中
        redisService.set(OrderKeyPrefix.getSeckillOrderByUidGid,":"+user.getId()+"_"+goodsVo.getId(),seckillOrder);
        return orderInfo;
    }


}
