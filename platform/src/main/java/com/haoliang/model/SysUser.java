package com.haoliang.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.haoliang.common.base.BaseModelCID;
import lombok.Data;


/**
 * @author Dominick Li
 * @description 系统用户表
 **/
@Data
@TableName("sys_user")
public class SysUser extends BaseModelCID {

    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 加密用得盐
     */
    private String salt;
    /**
     * 用户昵称
     */
    private String name;
    /**
     * 用户状态 1=正常 0=禁用
     */
    private Integer enabled = 1;
    /**
     * 角色编号
     */
    private Integer roleId;
    /**
     * 机构编号
     */
    private Integer channelId;
    /**
     * 逻辑删除字段 1=删除 0=未删除
     */
    private Integer deleted = 0;


}

