/*
 * Copyright (c) 2017 <l_iupeiyu@qq.com> All rights reserved.
 */

package com.geekcattle.controller.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.geekcattle.model.console.Menu;
import com.geekcattle.model.console.Role;
import com.geekcattle.model.console.RoleMenu;
import com.geekcattle.service.console.AdminRoleService;
import com.geekcattle.service.console.MenuService;
import com.geekcattle.service.console.RoleMenuService;
import com.geekcattle.service.console.RoleService;
import com.geekcattle.util.DateUtil;
import com.geekcattle.util.ReturnUtil;
import com.geekcattle.util.UuidUtil;
import com.geekcattle.util.console.MenuTreeUtil;
import com.github.pagehelper.PageInfo;

/**
 * author geekcattle
 * date 2016/10/21 0021 下午 15:58
 */
@Controller
@RequestMapping("/console/role")
public class RoleController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RoleService roleService;

    @Autowired
    private AdminRoleService adminRoleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 
     * @Title: index
     * @Description: 请求角色管理页面
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("role:index")
    @RequestMapping(value = "/index", method = {RequestMethod.GET})
    public String index(Model model) {
        return "console/role/index";
    }
    
    /**
     * 
     * @Title: list
     * @Description: 获取角色列表
     * @param role
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequiresPermissions("role:index")
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public ModelMap list(Role role) {
        ModelMap map = new ModelMap();
        List<Role> Lists = roleService.getPageList(role);
        for (Role list : Lists) {
            List<Menu> menuList = menuService.selectMenuByRoleId(list.getRoleId());
            list.setMenuList(menuList);
        }
        map.put("pageInfo", new PageInfo<Role>(Lists));
        map.put("queryParam", role);
        return ReturnUtil.Success("加载成功", map, null);
    }

    /**
     * 
     * @Title: from
     * @Description: 打开添加/修改角色页面
     * @param role
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("role:edit")
    @RequestMapping(value = "/from", method = {RequestMethod.GET})
    public String from(Role role, Model model) {
        if (!StringUtils.isEmpty(role.getRoleId())) {
            role = roleService.getById(role.getRoleId());
        }
        model.addAttribute("role", role);
        return "console/role/from";
    }

    /**
     * 
     * @Title: save
     * @Description: 保存角色信息
     * @param role
     * @param result
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequiresPermissions("role:save")
    @RequestMapping(value = "/save", method = {RequestMethod.POST})
    @ResponseBody
    public ModelMap save(@Valid Role role, BindingResult result) {
        if (result.hasErrors()) {
            for (ObjectError er : result.getAllErrors()) return ReturnUtil.Error(er.getDefaultMessage(), null, null);
        }
        try {
            if (StringUtils.isEmpty(role.getRoleId())) {
                role.setRoleId(UuidUtil.getUUID());
                role.setCreatedAt(DateUtil.getCurrentTime());
                role.setUpdatedAt(DateUtil.getCurrentTime());
                roleService.insert(role);
            } else {
                role.setUpdatedAt(DateUtil.getCurrentTime());
                roleService.save(role);
            }
            return ReturnUtil.Success("操作成功", null, "/console/role/index");
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    /**
     * 
     * @Title: delete
     * @Description: 删除角色信息
     * @param ids
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @Transactional
    @RequiresPermissions("role:delete")
    @RequestMapping(value = "/delete", method = {RequestMethod.GET})
    @ResponseBody
    public ModelMap delete(String[] ids) {
        try {
            if ("null".equals(ids) || "".equals(ids)) {
                return ReturnUtil.Error("Error", null, null);
            } else {
                for (String id : ids) {
                    adminRoleService.deleteRoleId(id); // 删除角色-管理员外键关联
                    roleMenuService.deleteRoleId(id); // 删除角色-菜单外键关联
                    roleService.deleteById(id); // 删除角色
                }
                return ReturnUtil.Success("操作成功", null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    @RequestMapping(value = "/combobox", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ModelMap comboBox() {
        ModelMap map = new ModelMap();
        List<Role> roleList = roleService.getFromAll();
        map.put("roleList", roleList);
        return ReturnUtil.Success(null, map, null);
    }

    /**
     * 
     * @Title: comboTree
     * @Description: TODO 改为：获取当前登录后台管理员所拥有的权限菜单树形列表
     * @param id
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequestMapping(value = "/menutree", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ModelMap comboTree(String id) {
        List<Menu> menuLists = menuService.getComboTree(null); // TODO
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(id);
        List<RoleMenu> roleMenuLists = roleMenuService.getRoleList(roleMenu);
        MenuTreeUtil menuTreeUtil = new MenuTreeUtil(menuLists, roleMenuLists);
        List<Map<String, Object>> mapList = menuTreeUtil.buildTree();
        return ReturnUtil.Success(null, mapList, null);
    }

    /**
     * 
     * @Title: grant
     * @Description: 保存授权信息
     * @param roleId
     * @param menuIds
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @Transactional
    @RequiresPermissions("role:grant")
    @RequestMapping(value = "/grant", method = {RequestMethod.POST})
    @ResponseBody
    public ModelMap grant(String roleId, String[] menuIds) {
        try {
            if (menuIds != null && StringUtils.isNotEmpty(roleId)) {
                if (StringUtils.isNotEmpty(menuIds.toString())) {
                    roleMenuService.deleteRoleId(roleId); // 先删除原来的角色授权
                    for (String menuId : menuIds) {
                        RoleMenu roleMenu = new RoleMenu();
                        roleMenu.setMenuId(menuId);
                        roleMenu.setRoleId(roleId);
                        roleMenuService.insert(roleMenu);
                    }
                }
            } else if (menuIds == null && StringUtils.isNotEmpty(roleId)) {
                roleMenuService.deleteRoleId(roleId); // 删除角色的授权
            }
            
            // TODO clears 将角色授权重新放入redis
            Subject subject = SecurityUtils.getSubject(); 
            PrincipalCollection principalCollection = subject.getPrincipals();  
            
            
            return ReturnUtil.Success("操作成功", null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    /**
     * 
     * @Title: grantForm
     * @Description: 请求显示角色授权页面
     * @param roleId
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("role:grant")
    @RequestMapping(value = "/grant", method = {RequestMethod.GET})
    public String grantForm(String roleId, Model model) {
        model.addAttribute("roleId", roleId);
        return "console/role/grant";
    }

    @RequestMapping(value = "/menulist", method = {RequestMethod.GET})
    @ResponseBody
    public ModelMap menulist(String id) {
        ModelMap map = new ModelMap();
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(id);
        List<RoleMenu> roleMenuLists = roleMenuService.getRoleList(roleMenu);
        ArrayList<String> roleList = new ArrayList<>();
        for (RoleMenu roleMenuList : roleMenuLists) {
            roleList.add(roleMenuList.getMenuId());
        }
        map.put("id", id);
        map.put("roleList", roleList);
        return ReturnUtil.Success("操作成功", map, null);
    }
}
