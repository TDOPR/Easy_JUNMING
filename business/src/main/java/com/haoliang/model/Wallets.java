package com.haoliang.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.haoliang.common.base.BaseModel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Dominick Li
 * @Description 用户钱包
 * @CreateTime 2022/11/1 10:57
 **/
@Data
@TableName("wallets")
public class Wallets extends BaseModel {

    /**
     * 所属用户Id
     */
    private Integer userId;

    /**
     * 机器人购买金额
     */
    private Integer robotAmount;

    /**
     * 机器人等级
     */
    private Integer robotLevel;

    /**
     * 托管本金 (余额)
     */
    private BigDecimal principalAmount;

    /**
     * 累计提现金额
     */
    private BigDecimal withdrawAmount;

    /**
     * 静态收益
     */
    private BigDecimal staticRewardAmount;

    /**
     * 团队业绩
     */
    private BigDecimal performanceAmount;

    /**
     * 总静态收益
     */
    private BigDecimal totalStaticReward;

    /**
     * 历史总金额
     */
    private BigDecimal totalAmount;




}
