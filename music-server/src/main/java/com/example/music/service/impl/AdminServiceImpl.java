package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.AdminMapper;
import com.example.music.model.domain.Admin;
import com.example.music.model.request.AdminRequest;
import com.example.music.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

import static com.example.music.constant.Constants.SALT;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    private void markAdminSession(HttpSession session, Admin admin) {
        session.setAttribute("name", admin.getName());
        session.setAttribute("adminName", admin.getName());
        session.setAttribute("adminId", admin.getId());
    }

    private void clearAdminSession(HttpSession session) {
        session.removeAttribute("name");
        session.removeAttribute("adminName");
        session.removeAttribute("adminId");
    }

    private Admin getCurrentAdmin(HttpSession session) {
        Object adminNameAttr = session.getAttribute("adminName");
        String sessionAdminName = adminNameAttr instanceof String ? (String) adminNameAttr : null;
        if (StringUtils.isBlank(sessionAdminName)) {
            Object legacyNameAttr = session.getAttribute("name");
            sessionAdminName = legacyNameAttr instanceof String ? (String) legacyNameAttr : null;
        }

        if (StringUtils.isBlank(sessionAdminName)) {
            return null;
        }

        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", sessionAdminName);
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin == null) {
            clearAdminSession(session);
            return null;
        }

        markAdminSession(session, admin);
        return admin;
    }

    @Override
    public R verityPasswd(AdminRequest adminRequest, HttpSession session) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", adminRequest.getUsername());
        queryWrapper.eq("password", DigestUtils.md5DigestAsHex((SALT + adminRequest.getPassword()).getBytes(StandardCharsets.UTF_8)));
        Admin admin = adminMapper.selectOne(queryWrapper);
        if (admin != null) {
            markAdminSession(session, admin);
            return R.success("登录成功");
        }

        clearAdminSession(session);
        return R.error("用户名或密码错误");
    }

    @Override
    public R addAdmin(AdminRequest adminRequest, HttpSession session) {
        Admin currentAdmin = getCurrentAdmin(session);
        if (currentAdmin == null) {
            return R.error("请先登录管理员账户");
        }

        String sessionAdminName = currentAdmin.getName();
        if (sessionAdminName.equals(adminRequest.getUsername())) {
            return R.error("不能添加与当前登录管理员同名的账户");
        }

        QueryWrapper<Admin> newAdminQuery = new QueryWrapper<>();
        newAdminQuery.eq("name", adminRequest.getUsername());
        if (adminMapper.selectCount(newAdminQuery) > 0) {
            return R.error("该管理员用户名已存在");
        }

        Admin admin = new Admin();
        admin.setName(adminRequest.getUsername());
        admin.setPassword(DigestUtils.md5DigestAsHex((SALT + adminRequest.getPassword()).getBytes(StandardCharsets.UTF_8)));
        if (adminMapper.insert(admin) > 0) {
            return R.success("添加成功");
        }

        return R.error("添加失败");
    }

    @Override
    public R getAdministrator(HttpSession session) {
        Admin currentAdmin = getCurrentAdmin(session);
        if (currentAdmin == null) {
            return R.error("未登录");
        }
        return R.success("用户验证成功，可以执行动作", adminMapper.selectList(null));
    }

    @Override
    public R getSessionAdmin(HttpSession session) {
        Admin currentAdmin = getCurrentAdmin(session);
        if (currentAdmin == null) {
            return R.error("请先登录管理员账户");
        }
        return R.success("登录状态有效", currentAdmin);
    }

    @Override
    public R deleteAdmin(Integer id, HttpSession session) {
        Admin currentAdmin = getCurrentAdmin(session);
        if (currentAdmin == null) {
            return R.error("请先登录管理员账户");
        }

        String sessionAdminName = currentAdmin.getName();
        Admin adminToDelete = adminMapper.selectById(id);
        if (adminToDelete == null) {
            return R.error("要删除的管理员账户不存在");
        }
        if (adminToDelete.getName().equals(sessionAdminName)) {
            return R.error("不能删除当前登录的管理员账户");
        }
        if (adminMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }
}
