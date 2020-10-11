package com.deng.rabbitmq;

import com.deng.rsdis.RedisService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQ的信息发送者
 */
@Service
public class MQSender {
    @Autowired
    AmqpTemplate amqpTemplate;

    public void send(Object message){
        String msg= RedisService.beanToString(message);
        amqpTemplate.convertAndSend(MQConfig.QUEUE,msg);

    }

    /**
     * 将为消息投递到topic exchange上
     * @param message
     */
    public void sendTopic(Object message){
        String msg=RedisService.beanToString(message);
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key",msg);
        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key2",msg+"2");
    }

    /**
     * 将消息投递到header exchange上
     * @param message
     */
    public void sendHeader(Object message){
        String msg = RedisService.beanToString(message);
        MessageProperties properties=new MessageProperties();
        properties.setHeader("header1","value1");
        properties.setHeader("header2","value2");
        Message obj=new Message(msg.getBytes(),properties);
        amqpTemplate.convertAndSend(MQConfig.HEADERS_EXCHANGE,"",obj);

    }

    /**
     * 将消息投递到fanout exchange
     * @param message
     */
    public void sendFanout(Object message){
        String msg = RedisService.beanToString(message);
        amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE,"",msg);

    }

    /**
     * 将用户秒杀信息投递到MQ中(使用direct模式的exchange)
     * @param seckillMessage
     */

    public void sendMiaoshaMessage(SeckillMessage seckillMessage){
        String msg = RedisService.beanToString(seckillMessage);
        amqpTemplate.convertAndSend(MQConfig.SECKILL_QUEUE,msg);
    }

}
