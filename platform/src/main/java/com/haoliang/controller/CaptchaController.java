package com.haoliang.controller;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import com.haoliang.common.constant.CacheKeyPrefixConstants;
import com.haoliang.common.model.JsonResult;
import com.haoliang.common.model.vo.CaptchaVO;
import com.haoliang.common.utils.RedisUtils;
import com.haoliang.common.utils.ReflectUtils;
import com.haoliang.common.utils.SpringUtil;
import com.haoliang.config.LoginConfig;
import com.haoliang.enums.CaptchaCategory;
import com.haoliang.enums.CaptchaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 验证码操作处理
 */
@Slf4j
@RestController
public class CaptchaController {

    @Autowired
    private LoginConfig loginConfig;

    /**
     * 生成验证码
     */
    @GetMapping("/captchaImage")
    public JsonResult getCode() {
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = CacheKeyPrefixConstants.CAPTCHA_CODE + uuid;
        // 获取验证码类型
        CaptchaType captchaType = loginConfig.getCaptcha().getType();

        if (captchaType == CaptchaType.RANDOM) {
            //随机策略
            int num = (int) (Math.random() * 2);
            captchaType = num == 0 ? CaptchaType.MATH : CaptchaType.CHAR;
        }

        //获取配置的长度
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? loginConfig.getCaptcha().getNumberLength() : loginConfig.getCaptcha().getCharLength();

        CodeGenerator codeGenerator = ReflectUtils.newInstance(captchaType.getClazz(), length);

        //获取干扰策略
        CaptchaCategory captchaCategory = loginConfig.getCaptcha().getCategory();

        if (captchaCategory == CaptchaCategory.RANDOM) {
            //随机策略
            int num = (int) (Math.random() * 3);
            if (num == 0) {
                captchaCategory = CaptchaCategory.LINE;
            } else if (num == 1) {
                captchaCategory = CaptchaCategory.CIRCLE;
            } else {
                captchaCategory = CaptchaCategory.SHEAR;
            }
        }

        AbstractCaptcha captcha = SpringUtil.getBean(captchaCategory.getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        String code = isMath ? getCodeResult(captcha.getCode()) : captcha.getCode();
        RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(loginConfig.getCaptcha().getExpirationTime()));
        return JsonResult.successResult(new CaptchaVO(uuid, captcha.getImageBase64()));
    }

    /**
     * 验证码结果
     */
    private String getCodeResult(String capStr) {
        int numberLength = loginConfig.getCaptcha().getNumberLength();
        int a = Convert.toInt(StringUtils.substring(capStr, 0, numberLength).trim());
        char operator = capStr.charAt(numberLength);
        int b = Convert.toInt(StringUtils.substring(capStr, numberLength + 1, numberLength + 1 + numberLength).trim());
        switch (operator) {
            case '*':
                return Convert.toStr(a * b);
            case '+':
                return Convert.toStr(a + b);
            case '-':
                return Convert.toStr(a - b);
            default:
                return StringUtils.EMPTY;
        }
    }

}
