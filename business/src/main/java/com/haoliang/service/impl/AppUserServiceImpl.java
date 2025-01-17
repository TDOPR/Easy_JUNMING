package com.haoliang.service.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haoliang.common.config.AppParamProperties;
import com.haoliang.common.config.GlobalProperties;
import com.haoliang.common.config.SysSettingParam;
import com.haoliang.common.constant.CacheKeyPrefixConstants;
import com.haoliang.common.constant.LanguageKeyConstants;
import com.haoliang.common.enums.BooleanEnum;
import com.haoliang.common.enums.ReturnMessageEnum;
import com.haoliang.common.model.JsonResult;
import com.haoliang.common.model.PageParam;
import com.haoliang.common.model.SysLoginLog;
import com.haoliang.common.model.ThreadLocalManager;
import com.haoliang.common.model.dto.UpdatePasswordDTO;
import com.haoliang.common.model.vo.PageVO;
import com.haoliang.common.service.SysLoginLogService;
import com.haoliang.common.util.*;
import com.haoliang.common.util.encrypt.AESUtil;
import com.haoliang.common.util.redis.RedisUtil;
import com.haoliang.enums.CoinNetworkSourceEnum;
import com.haoliang.enums.FlowingTypeEnum;
import com.haoliang.enums.RobotEnum;
import com.haoliang.mapper.*;
import com.haoliang.model.*;
import com.haoliang.model.condition.AppUsersCondition;
import com.haoliang.model.dto.AppUserDTO;
import com.haoliang.model.dto.AppUserLoginDTO;
import com.haoliang.model.dto.AppUserRegisterDTO;
import com.haoliang.model.dto.FindPasswordDTO;
import com.haoliang.model.vo.*;
import com.haoliang.service.*;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author Dominick Li
 * @Description
 * @CreateTime 2022/11/4 11:21
 **/
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUsers> implements AppUserService {

    @Autowired
    private SysLoginLogService sysLoginLogService;

    @Autowired
    private WalletsService walletsService;

    @Autowired
    private DayRateService dayRateService;

    @Autowired
    private TreePathService treePathService;

    @Autowired
    private AppParamProperties appParamProperties;

    @Resource
    private EvmAddressPoolMapper evmAddressPoolMapper;

    @Resource
    private AppUserMapper appUserMapper;

    @Resource
    private AppVersionsMapper appVersionsMapper;

    @Resource
    private EvmWithdrawMapper evmWithdrawMapper;

    @Resource
    private WalletLogsMapper walletLogsMapper;

    @Resource
    private EvmUserWalletService evmUserWalletService;

    @Override
    public JsonResult<MyDetailVO> getMyDetail() {
        Integer userId = JwtTokenUtil.getUserIdFromToken(ThreadLocalManager.getToken());
        AppUsers appUsers = this.selectColumnsByUserId(userId, AppUsers::getLevel, AppUsers::getAutograph, AppUsers::getAutograph, AppUsers::getNickName, AppUsers::getInviteCode, AppUsers::getHeadImage);
        MyDetailVO myDetailVO = new MyDetailVO();
        BeanUtils.copyProperties(appUsers, myDetailVO);
        myDetailVO.setPlatformDesc(MessageUtil.get(LanguageKeyConstants.PLATFORM_DESC, ThreadLocalManager.getLanguage()));
        return JsonResult.successResult(myDetailVO);
    }

    @Override
    public JsonResult home() {
        Integer robotLevel = 0;
        String token = ThreadLocalManager.getToken();
        BigDecimal principalAmount = BigDecimal.ZERO;
        //判断当前用户是否登录
        if (!JwtTokenUtil.isTokenExpired(token)) {
            Integer userId = JwtTokenUtil.getUserIdFromToken(token);
            Wallets wallets = walletsService.selectColumnsByUserId(userId, Wallets::getPrincipalAmount, Wallets::getRobotLevel);
            robotLevel = wallets.getRobotLevel();
            principalAmount = wallets.getPrincipalAmount();
        }
        HomeVO homeVO = new HomeVO();
        RobotEnum robotEnum = RobotEnum.levelOf(robotLevel);
        homeVO.setDayRate(NumberUtil.formatBigDecimalToRateStr(dayRateService.selectNewDayRate(robotLevel)));
        homeVO.setWeekRate(NumberUtil.formatBigDecimalToRateStr(dayRateService.selectNewWeekRate(robotLevel)));
        homeVO.setDayRateSection(robotEnum.getDayRateSection());
        homeVO.setWeekRateSection(robotEnum.getWeekRateSection());
        //总地址
        homeVO.setTotalAddress((int) this.count());
        //24小时新增地址
        LocalDateTime dateTime = LocalDateTime.now();
        dateTime.minusDays(1);
        homeVO.setAddress((int) this.count(new LambdaQueryWrapper<AppUsers>().ge(AppUsers::getCreateTime, dateTime)));
        //我的托管量和平台托管基恩
        homeVO.setTrusteeshipAmount(NumberUtil.toMoeny(principalAmount));
        BigDecimal totalAmount = walletsService.getPlatformTotalLockAmount();
        homeVO.setTotalTrusteeshipAmount(NumberUtil.toMoeny(totalAmount));

        return JsonResult.successResult(homeVO);
    }

    @Override
    public JsonResult<AppTokenVO> login(AppUserLoginDTO appUserLoginDTO, String localIp) {
        AppUsers appUsers = this.getOne(new LambdaQueryWrapper<AppUsers>().eq(AppUsers::getEmail, appUserLoginDTO.getEmail()));
        if (appUsers == null) {
            return JsonResult.failureResult(ReturnMessageEnum.EMAIL_NOT_EXISTS);
        } else if (appUsers.getEnabled().equals(BooleanEnum.FALSE.getIntValue())) {
            return JsonResult.failureResult(ReturnMessageEnum.ACCOUNT_DISABLED);
        } else if (!appUsers.getPassword().equals(AESUtil.encrypt(appUserLoginDTO.getPassword(), appUsers.getSalt()))) {
            return JsonResult.failureResult(ReturnMessageEnum.PASSWORD_ERROR);
        }

        //修改登录次数
        UpdateWrapper<AppUsers> updateWrapper = Wrappers.update();
        updateWrapper.lambda()
                .set(AppUsers::getLoginCount, appUsers.getLoginCount() + 1)
                .eq(AppUsers::getId, appUsers.getId());
        this.update(updateWrapper);

        String token = JwtTokenUtil.getToken(appUsers.getId());

        //单点登录需要删除用户在其它地方登录的Token
        if (SysSettingParam.isEnableSso()) {
            RedisUtil.deleteObjects(CacheKeyPrefixConstants.APP_TOKEN + appUsers.getId() + ":*");
        }

        //把token存储到缓存中
        String tokenKey = CacheKeyPrefixConstants.APP_TOKEN + appUsers.getId() + ":" + IdUtil.simpleUUID();
        RedisUtil.setCacheObject(tokenKey, token, Duration.ofSeconds(GlobalProperties.getTokenExpire()));
        sysLoginLogService.save(new SysLoginLog(appUsers.getEmail(), localIp, 2));

        //返回token给客户端
        AppTokenVO appTokenVO = new AppTokenVO();
        appTokenVO.setToken(tokenKey);
        return JsonResult.successResult(appTokenVO);
    }

    @Override
    @Transactional
    public JsonResult register(AppUserRegisterDTO appUserRegisterDTO) {
        String cacheKey = CacheKeyPrefixConstants.CAPTCHA_CODE + appUserRegisterDTO.getUuid();
        String code = RedisUtil.getCacheObject(cacheKey);
        if (code == null) {
            return JsonResult.failureResult(ReturnMessageEnum.VERIFICATION_CODE_EXPIRE);
        }
        if (!code.equals(appUserRegisterDTO.getCode())) {
            return JsonResult.failureResult(ReturnMessageEnum.VERIFICATION_CODE_ERROR);
        }
        AppUsers appUsers;
        appUsers = this.getOne(new LambdaQueryWrapper<AppUsers>().eq(AppUsers::getEmail, appUserRegisterDTO.getEmail()));
        if (appUsers == null) {
            Integer inviteUserId = null;
            if (StringUtil.isNotEmpty(appUserRegisterDTO.getInviteCode())) {
                //根据邀请码找到对应的用户
                AppUsers inviteUser = this.getOne(new LambdaQueryWrapper<AppUsers>().eq(AppUsers::getInviteCode, appUserRegisterDTO.getInviteCode()));
                if (inviteUser == null) {
                    return JsonResult.failureResult(ReturnMessageEnum.INVITE_CODE_ERROR);
                }
                inviteUserId = inviteUser.getId();
            }
            //自动注册
            appUsers = new AppUsers();
            appUsers.setEmail(appUserRegisterDTO.getEmail());
            //生成邀请码
            appUsers.setInviteCode(getInviteCode());
            //设置用户的邀请人ID
            appUsers.setInviteId(inviteUserId);
            //设置密码加密用的盐
            appUsers.setSalt(IdUtil.simpleUUID());
            appUsers.setPassword(AESUtil.encrypt(appUserRegisterDTO.getPassword(), appUsers.getSalt()));
            this.save(appUsers);

            //默认为用户分配一个BSC的区块链钱包
            {
                EvmAddressPool evmAddressPool = evmAddressPoolMapper.randomGetAddress(CoinNetworkSourceEnum.BSC.getName());
                if (evmAddressPool != null) {
                    evmAddressPoolMapper.deleteByAddress(evmAddressPool.getAddress());
                    //添加到区块链用户钱包表
                    evmUserWalletService.save(EvmUserWallet.builder()
                            .address(evmAddressPool.getAddress())
                            .coinId(evmAddressPool.getCoinId())
                            .keystore(evmAddressPool.getKeystore())
                            .valid("E")
                            .lowerAddress(evmAddressPool.getAddress().toLowerCase())
                            .coinType(evmAddressPool.getCoinType())
                            .password(evmAddressPool.getPwd())
                            .userId(appUsers.getId())
                            .build());
                }
            }

            //创建一条钱包记录
            Wallets wallets = new Wallets();
            wallets.setUserId(appUsers.getId());
            walletsService.save(wallets);
            //添加一条默认的treepath记录
            TreePath treePath = TreePath.builder()
                    .ancestor(appUsers.getId())
                    .descendant(appUsers.getId())
                    .level(0)
                    .build();
            treePathService.save(treePath);
            if (inviteUserId != null) {
                //保存上下级关系
                treePathService.insertTreePath(appUsers.getId(), inviteUserId);
            }
            return JsonResult.successResult();
        } else {
            return JsonResult.failureResult(ReturnMessageEnum.EMAIL_EXISTS);
        }
    }

    @Override
    public JsonResult findPassword(FindPasswordDTO findPasswordDTO) {
        String cacheKey = CacheKeyPrefixConstants.CAPTCHA_CODE + findPasswordDTO.getUuid();
        String code = RedisUtil.getCacheObject(cacheKey);
        if (code == null) {
            return JsonResult.failureResult(ReturnMessageEnum.VERIFICATION_CODE_EXPIRE);
        }
        if (!code.equals(findPasswordDTO.getCode())) {
            return JsonResult.failureResult(ReturnMessageEnum.VERIFICATION_CODE_ERROR);
        }
        AppUsers appUsers;
        appUsers = this.getOne(new LambdaQueryWrapper<AppUsers>().eq(AppUsers::getEmail, findPasswordDTO.getEmail()));
        if (appUsers == null) {
            return JsonResult.failureResult(ReturnMessageEnum.EMAIL_NOT_EXISTS);
        }
        appUsers.setPassword(AESUtil.encrypt(findPasswordDTO.getPassword(), appUsers.getSalt()));
        this.saveOrUpdate(appUsers);
        return JsonResult.successResult();
    }

    /**
     * 生成唯一邀请码
     */
    private String getInviteCode() {
        String inviteCode = IdUtil.generateShortUUID(8);
        AppUsers exists;
        while (true) {
            exists = this.getOne(new LambdaQueryWrapper<AppUsers>().eq(AppUsers::getInviteCode, inviteCode));
            if (exists != null) {
                inviteCode = IdUtil.generateShortUUID(8);
            } else {
                break;
            }
        }
        return inviteCode;
    }

    @Override
    public JsonResult<PageVO<AppUsersVO>> pageList(PageParam<AppUsers, AppUsersCondition> pageParam) {
        IPage<AppUsersVO> iPage = appUserMapper.page(pageParam.getPage(), pageParam.getSearchParam());
        //查询下级数量
        for (AppUsersVO appUsersVO : iPage.getRecords()) {
            appUsersVO.setSubordinateNumber(treePathService.countByAncestor(appUsersVO.getId()) - 1);
        }
        return JsonResult.successResult(new PageVO<>(iPage.getTotal(), iPage.getPages(), iPage.getRecords()));
    }

    @Override
    public JsonResult modifyUserDetail(AppUserDTO appUserDTO) {
        UpdateWrapper<AppUsers> updateWrapper = Wrappers.update();
        updateWrapper.lambda()
                .set(AppUsers::getNickName, appUserDTO.getNickName())
                .set(AppUsers::getAutograph, appUserDTO.getAutograph())
                .eq(AppUsers::getId, JwtTokenUtil.getUserIdFromToken(ThreadLocalManager.getToken()));
        boolean flag = this.update(updateWrapper);
        return JsonResult.build(flag);
    }

    @Override
    public JsonResult uploadHeadImage(MultipartFile file) throws Exception {
        Integer userId = JwtTokenUtil.getUserIdFromToken(ThreadLocalManager.getToken());

        AppUsers appUsers = this.selectColumnsByUserId(userId, AppUsers::getHeadImage);
        String headName = appUsers.getHeadImage().substring(appUsers.getHeadImage().lastIndexOf("/")+1);

        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        String fileName = userId + "_" + IdUtil.getSnowflakeNextId()+"." + suffix;
        String savePath = appParamProperties.getImageSavePath();
        FileUtil.del(new File(savePath,headName));


        String url = GlobalProperties.getVirtualPathURL() + StringUtil.replace(appParamProperties.getImageSavePath(), appParamProperties.getRootPath(), "") + fileName;
        //复制文件流到本地文件
        FileUtils.copyInputStreamToFile(file.getInputStream(), new File(savePath, fileName));
        UpdateWrapper<AppUsers> updateWrapper = Wrappers.update();
        updateWrapper.lambda()
                .set(AppUsers::getHeadImage, url)
                .eq(AppUsers::getId, userId);
        boolean flag = this.update(updateWrapper);

        if (flag) {
            JSONObject object = new JSONObject();
            object.put("url", url);
            return JsonResult.successResult(object);
        }
        return JsonResult.failureResult();
    }


    @Override
    public JsonResult updatePassword(UpdatePasswordDTO updatePasswordDTO) {
        Integer userId = JwtTokenUtil.getUserIdFromToken(ThreadLocalManager.getToken());
        AppUsers sysUser = this.selectColumnsByUserId(userId, AppUsers::getSalt, AppUsers::getPassword);

        String oldPwd = AESUtil.encrypt(updatePasswordDTO.getOldPassword(), sysUser.getSalt());
        if (sysUser.getPassword().equals(oldPwd)) {
            UpdateWrapper<AppUsers> wrapper = Wrappers.update();
            wrapper.lambda()
                    .set(AppUsers::getPassword, AESUtil.encrypt(updatePasswordDTO.getPassword(), sysUser.getSalt()))
                    .eq(AppUsers::getId, userId);
            update(wrapper);
            return JsonResult.successResult();
        }
        return JsonResult.failureResult(ReturnMessageEnum.ORIGINAL_PASSWORD_ERROR);
    }

    @Override
    public BusinessVO getBusinessVO() {
        BigDecimal totalRecharge = walletLogsMapper.sumTotalAmountByType(FlowingTypeEnum.RECHARGE.getValue());
        BigDecimal totalWithdraw = walletLogsMapper.sumTotalAmountByType(FlowingTypeEnum.WITHDRAWAL.getValue());

        BusinessVO businessVO = BusinessVO.builder()
                .onlineUserSize(sysLoginLogService.getTodayLoginCount())
                .totalUserSize((int) this.count())
                .totalTrusteeship(NumberUtil.toMoeny(walletsService.getPlatformTotalLockAmount()))
                .ToBeReviewedSize(evmWithdrawMapper.selectCount(new LambdaQueryWrapper<EvmWithdraw>().eq(EvmWithdraw::getAuditStatus, 0)).intValue())
                .totalRecharge(NumberUtil.toMoeny(totalRecharge))
                .totalWithdraw(NumberUtil.toMoeny(totalWithdraw))
                .build();
        return businessVO;
    }

    @Override
    public AppUsers selectColumnsByUserId(Integer userId, SFunction<AppUsers, ?>... columns) {
        return this.getOne(
                new LambdaQueryWrapper<AppUsers>()
                        .select(columns)
                        .eq(AppUsers::getId, userId)
        );
    }
}
