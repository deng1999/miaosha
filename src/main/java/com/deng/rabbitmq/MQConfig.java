package com.deng.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * 通过配置文件获取消息队列
 */
@Configuration
public class MQConfig {

    // 消息队列名
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String QUEUE = "queue";
    public static final String TOPIC_QUEUE1 = "topic.queue1";
    public static final String TOPIC_QUEUE2 = "topic.queue2";
    public static final String HEADER_QUEUE = "header.queue";
    public static final String TOPIC_EXCHANGE = "topicExchange";
    public static final String FANOUT_EXCHANGE = "fanoutExchange";
    public static final String HEADERS_EXCHANGE = "headersExchange";

    /**
     * 消息队列queue
     * Direct模式
     * @return
     */
    @Bean
    public Queue queue(){
        return new Queue(QUEUE,true);
    }
    /**
     * Direct模式 交换机exchange
     * 生成用于秒杀的queue
     *
     * @return
     */
    @Bean
    public Queue seckillQueue(){
        return new Queue(SECKILL_QUEUE,true);
    }
    @Bean
    public Queue topicQueue1(){
        return new Queue(TOPIC_QUEUE1,true);
    }
    @Bean
    public Queue topicQueue2(){
        return new Queue(TOPIC_QUEUE2,true);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public Binding topicBinding1(){
        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with("topic.key");

    }
    @Bean
    public Binding topicBinding2(){
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with("topic.#");
    }
@Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(FANOUT_EXCHANGE);
}
    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(HEADERS_EXCHANGE);
    }

    /**
     * 创建队列
     *
     * @return
     */
    @Bean
    public Queue headerQueue1() {
        return new Queue(HEADER_QUEUE, true);
    }
@Bean
    public Binding headerBinding(){
    Map<String,Object> map=new HashMap<String, Object>();
    map.put("header1","value1");
    map.put("header2","value2");

    return BindingBuilder.bind(headerQueue1()).to(headersExchange()).whereAll(map).match();
}

}
