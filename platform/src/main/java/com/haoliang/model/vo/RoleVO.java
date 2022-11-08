package com.haoliang.model.vo;

import com.haoliang.model.SysMenu;
import com.haoliang.model.SysRole;
import lombok.Data;

import java.util.List;

@Data
public class RoleVO  extends SysRole {

    /**
     * 关联的用户名
     */
    private String userStr;

    /**
     * 角色拥有的菜单Id 用于修改角色时候自动勾选菜单
     */
    private List<Integer> menuIds;

    /**
     * 用户拥有的角色信息
     */
    private List<SysMenu> sysMenus;
}
