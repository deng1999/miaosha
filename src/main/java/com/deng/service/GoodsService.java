package com.deng.service;

import com.deng.dao.GoodsDao;
import com.deng.entity.SeckillGoods;
import com.deng.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {
    @Autowired
    GoodsDao goodsDao;

    /**
     * 查出商品信息（包含该商品的秒杀信息）
     * @return
     */
    public List<GoodsVo> listGoodsVo(){
        return goodsDao.listGoodsVo();
    }

    /**
     * 通过商品的id查出商品的所有信息（包含该商品的秒杀信息）
     * @param goodsId
     * @return
     */
    public GoodsVo getGoodsVoByGoodsId(Long goodsId){
        return goodsDao.getGoodsVoByGoodsId(goodsId);
    }

    /**
     * order表减库存
     *
     * @param goodsVo
     * @return
     */
    public boolean reduceStock(GoodsVo goodsVo){
        SeckillGoods seckillGoods=new SeckillGoods();
        seckillGoods.setGoodsId(goodsVo.getId());
        int stack = goodsDao.reduceStack(seckillGoods);
        return stack>0;
    }
}
