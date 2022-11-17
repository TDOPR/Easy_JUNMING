package com.haoliang.controller;

import com.haoliang.common.model.JsonResult;
import com.haoliang.common.utils.JwtTokenUtils;
import com.haoliang.model.dto.AmountDTO;
import com.haoliang.model.vo.TrusteeshipAmountVO;
import com.haoliang.service.TrusteeshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author Dominick Li
 * @Description 托管量化
 * @CreateTime 2022/11/17 11:09
 **/
@RestController
@RequestMapping("/trusteeship")
public class TrusteeshipController {

    @Autowired
    private TrusteeshipService trusteeshipRecharge;

    /**
     * 我的量化金额
     */
    @GetMapping
    public JsonResult<TrusteeshipAmountVO> quantificationAmount(@RequestHeader(JwtTokenUtils.TOKEN_NAME)String token){
        return trusteeshipRecharge.getTrusteeshipAmount(token);
    }

    /**
     * 托管金额充值
     */
    @PostMapping("/recharge")
    public JsonResult entrustWithdrawal(@Valid @RequestBody AmountDTO amountDTO, @RequestHeader(JwtTokenUtils.TOKEN_NAME)String token){
        return trusteeshipRecharge.recharge(amountDTO,token);
    }

    /**
     * 托管金额提现到钱包
     */
    @PostMapping("/withdrawal")
    public JsonResult trusteeshipWithdrawal(@Valid @RequestBody AmountDTO amountDTO,@RequestHeader(JwtTokenUtils.TOKEN_NAME)String token){
        return trusteeshipRecharge.withdrawal(amountDTO,token);
    }

}
