package com.deng.dao;

import com.deng.entity.OrderInfo;
import com.deng.entity.SeckillOrder;
import org.apache.ibatis.annotations.*;

/**
 * seckill_order表数据访问层
 */
@Mapper
public interface OrderDao {
    /**
     * 通过用户id与商品id从订单列表中获取订单信息
     * @param userId
     * @param goodsId
     * @return
     */

    @Select("SELECT * FROM seckill_order WHERE user_id=#{userId} AND goods_id=#{goodsId}")
    SeckillOrder getSeckillOrderByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") long goodsId);

    /**
     * 将订单信息插入order_info表中
     * @param orderInfo
     * @return 插入成功的订单信息id
     */

    @Insert("INSERT INTO order_info (user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)"
            + "VALUES (#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    // 查询出插入订单信息的表id，并返回
    @SelectKey(keyColumn = "id", keyProperty = "id", resultType = long.class, before = false, statement = "SELECT last_insert_id()")
    long insert(OrderInfo orderInfo);

    /**
     * 将秒杀信息插入到seckill_order表中
     * @param seckillOrder
     */
    @Insert("INSERT INTO seckill_order(user_id, order_id, goods_id) VALUES (#{userId}, #{orderId}, #{goodsId})")
    void insertSeckillOrder(SeckillOrder seckillOrder);

    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    @Select("select * from order_info where id = #{orderId}")
    OrderInfo getOrderById(@Param("orderId") long orderId);


}
