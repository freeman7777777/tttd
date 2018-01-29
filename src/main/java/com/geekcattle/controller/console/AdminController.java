/*
 * Copyright (c) 2017 <l_iupeiyu@qq.com> All rights reserved.
 */

package com.geekcattle.controller.console;

import com.geekcattle.model.console.Admin;
import com.geekcattle.model.console.AdminRole;
import com.geekcattle.model.console.Role;
import com.geekcattle.service.console.AdminRoleService;
import com.geekcattle.service.console.AdminService;
import com.geekcattle.service.console.RoleService;
import com.geekcattle.util.*;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
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
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Example;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * author geekcattle
 * date 2016/10/21 0021 下午 15:58
 */
@Controller
@RequestMapping("/console/admin")
public class AdminController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRoleService adminRoleService;

    @Autowired
    private RoleService roleService;

    /**
     * 
     * @Title: index
     * @Description: 请求显示管理员管理页面
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("admin:index")
    @RequestMapping(value = "/index", method = {RequestMethod.GET})
    public String index(Model model) {
        return "console/admin/index";
    }

    /**
     * 
     * @Title: from
     * @Description: 显示添加/修改管理员页面
     * @param admin
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("admin:edit")
    @RequestMapping(value = "/from", method = {RequestMethod.GET})
    public String from(Admin admin, Model model) {
        String checkRoleId = "";
        if (!StringUtils.isEmpty(admin.getUid())) { // 修改
            admin = adminService.getById(admin.getUid()); // 获取要修改的管理员信息
            if (!"null".equals(admin)) {
                AdminRole adminRole = new AdminRole();
                adminRole.setAdminId(admin.getUid());
                List<AdminRole> adminRoleLists = adminRoleService.getRoleList(adminRole); // 获取要修改的管理员的当前权限列表
                admin.setUpdatedAt(DateUtil.getCurrentTime());
                ArrayList<String> checkRoleIds = new ArrayList<String>();
                for (AdminRole adminRoleList : adminRoleLists) {
                    checkRoleIds.add(adminRoleList.getRoleId());
                }
                checkRoleId = String.join(",", checkRoleIds);
            }
        } else { // 添加
            admin.setIsSystem(0);
        }
        model.addAttribute("checkRoleId", checkRoleId);
        model.addAttribute("roleLists", this.getRoleList());
        model.addAttribute("admin", admin);
        return "console/admin/from";
    }

    /**
     * 
     * @Title: getRoleList
     * @Description: 获取所有启用的角色
     * @return 参数
     * @return List<Role> 返回类型
     * @throws
     */
    private List<Role> getRoleList() {
        List<Role> roleList = roleService.getFromAll();
        return roleList;
    }

    /**
     * 
     * @Title: list
     * @Description: 获取管理员列表
     * @param admin
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequiresPermissions("admin:index")
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public ModelMap list(Admin admin) {
        ModelMap map = new ModelMap();
        List<Admin> Lists = adminService.getPageList(admin); // 获取管理员列表
        for (Admin list : Lists) {
            List<Role> rolelist = roleService.selectRoleListByAdminId(list.getUid()); // 获取每个管理员的角色列表
            list.setRoleList(rolelist);
        }
        map.put("pageInfo", new PageInfo<Admin>(Lists));
        map.put("queryParam", admin);
        return ReturnUtil.Success("加载成功", map, null);
    }

    /**
     * 
     * @Title: save
     * @Description: 保存添加/修改的管理员信息
     * @param admin
     * @param result
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @Transactional
    @RequiresPermissions("admin:save")
    @RequestMapping(value = "/save", method = {RequestMethod.POST})
    @ResponseBody
    public ModelMap save(@Valid Admin admin, BindingResult result) {
        try {
            if (result.hasErrors()) {
                for (ObjectError er : result.getAllErrors())
                    return ReturnUtil.Error(er.getDefaultMessage(), null, null);
            }
            if (StringUtils.isEmpty(admin.getUid())) { // 添加
                Example example = new Example(Admin.class);
                example.createCriteria().andCondition("username = ", admin.getUsername());
                Integer userCount = adminService.getCount(example); // 查看是否有同名管理员
                if (userCount > 0) {
                    return ReturnUtil.Error("用户名已存在", null, null);
                }
                if (StringUtils.isEmpty(admin.getPassword())) {
                    return ReturnUtil.Error("密码不能为空", null, null);
                }
                String Id = UuidUtil.getUUID();
                admin.setUid(Id);
                String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
                admin.setSalt(salt);
                String password = PasswordUtil.createAdminPwd(admin.getPassword(), admin.getCredentialsSalt());
                admin.setPassword(password);
                admin.setIsSystem(0);
                admin.setCreatedAt(DateUtil.getCurrentTime());
                admin.setUpdatedAt(DateUtil.getCurrentTime());
                adminService.insert(admin);
            } else { // 修改
                Admin updateAdmin = adminService.getById(admin.getUid());
                if (!"null".equals(updateAdmin)) {
                    admin.setSalt(updateAdmin.getSalt());
                    if (!StringUtils.isEmpty(admin.getPassword())) { // 如果填写了密码，则进行加密
                        String password = PasswordUtil.createAdminPwd(admin.getPassword(), updateAdmin.getCredentialsSalt());
                        admin.setPassword(password);
                    } else { // 如果没填写密码，则把原来的密码取出来，赋值到带有更新信息的管理员对象上
                        admin.setPassword(updateAdmin.getPassword());
                    }
                    admin.setUpdatedAt(DateUtil.getCurrentTime());
                    adminService.save(admin);
                } else {
                    return ReturnUtil.Error("操作失败", null, null);
                }
            }
            if (admin.getRoleId() != null) { // 若选择了角色，则更新管理员的角色
                adminRoleService.deleteAdminId(admin.getUid()); // 先删除原来关联的角色
                for (String roleid : admin.getRoleId()) {
                    AdminRole adminRole = new AdminRole();
                    adminRole.setAdminId(admin.getUid());
                    adminRole.setRoleId(roleid);
                    adminRoleService.insert(adminRole);
                }
            } else { // 若没选择角色，则删除管理员的角色
                adminRoleService.deleteAdminId(admin.getUid());
            }
            return ReturnUtil.Success("操作成功", null, "/console/admin/index");
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    @RequiresPermissions("admin:editpwd")
    @RequestMapping(value = "/savepwd", method = {RequestMethod.POST})
    @ResponseBody
    public ModelMap editPwd(String uid, String password) {
        try {
            if (StringUtils.isNotEmpty(uid) && StringUtils.isNotEmpty(password)) {
                Admin admin = adminService.getById(uid);
                if (!"null".equals(admin)) {
                    String newPassword = PasswordUtil.createAdminPwd(password, admin.getSalt());
                    Admin pwdAdmin = new Admin();
                    pwdAdmin.setPassword(newPassword);
                    Example example = new Example(Admin.class);
                    example.createCriteria().andCondition("uid", uid);
                    adminService.updateExample(pwdAdmin, example);
                    return ReturnUtil.Success("操作成功", null, null);
                } else {
                    return ReturnUtil.Error("对像不存在，修改失败", null, null);
                }
            } else {
                return ReturnUtil.Error("参数错误，修改失败", null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("修改失败", null, null);
        }
    }

    /**
     * 
     * @Title: delete
     * @Description: 删除管理员
     * @param ids
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @Transactional
    @RequiresPermissions("admin:delete")
    @RequestMapping(value = "/delete", method = {RequestMethod.GET})
    @ResponseBody
    public ModelMap delete(String[] ids) {
        try {
            if (ids != null) {
                if (StringUtils.isNotBlank(ids.toString())) {
                    for (String id : ids) {
                        adminRoleService.deleteAdminId(id); // 删除管理员关联的角色
                        adminService.deleteById(id); // 删除管理员
                    }
                }
                return ReturnUtil.Success("删除成功", null, null);
            } else {
                return ReturnUtil.Error("删除失败", null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("删除失败", null, null);
        }
    }

}
