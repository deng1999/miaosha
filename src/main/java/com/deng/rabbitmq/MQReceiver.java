package com.deng.rabbitmq;

import com.deng.entity.SeckillOrder;
import com.deng.entity.SeckillUser;
import com.deng.rsdis.RedisService;
import com.deng.service.GoodsService;
import com.deng.service.OrderService;
import com.deng.service.SeckillService;
import com.deng.vo.GoodsVo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQ消息接收者
 * 消费者绑定在队列监听
 */
@Service
public class MQReceiver {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillService seckillService;

    //    @RabbitHandler
    @RabbitListener(queues = {MQConfig.QUEUE})
    public void receive(String message) {

    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void receiveTopic1(String message) {

    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void receiveTopic2(String message) {

    }

    @RabbitListener(queues = MQConfig.HEADER_QUEUE)
    public void receiveHeaderQueue(byte[] message) {

    }

    /**
     * 处理收到的秒杀成功信息
     *
     * @param message
     */
    @RabbitListener(queues = MQConfig.SECKILL_QUEUE)
    public void receiveMiaoshaInfo(String message){

        SeckillMessage seckillMessage= RedisService.stringToBean(message, SeckillMessage.class);
        //获取秒杀的用户信息与商品id
        SeckillUser user = seckillMessage.getUser();
        long goodsId = seckillMessage.getGoodsId();
        //获取商品的库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if (stockCount<=0)
            return;
        //判断是否已经秒杀到了
        SeckillOrder order=orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(),goodsId);
        if (order!=null)
            return;
        //减库存 下订单 写入秒杀订单
        seckillService.sekill(user,goods);
    }
}
