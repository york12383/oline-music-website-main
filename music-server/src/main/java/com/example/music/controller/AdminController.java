package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.AdminRequest;
import com.example.music.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * ?????????
 */
@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;

    // ????????
    @PostMapping("/admin/login/status")
    public R loginStatus(@RequestBody AdminRequest adminRequest, HttpSession session) {
        return adminService.verityPasswd(adminRequest, session);
    }

    @PostMapping("/admin/add")
    public R addAdmin(@RequestBody AdminRequest adminRequest, HttpSession session) {
        return adminService.addAdmin(adminRequest,session);
    }

    // ???????
    @GetMapping("/admin/administrator")
    public R getAdministrator(HttpSession session) {
        return adminService.getAdministrator(session);
    }

    @GetMapping("/admin/session")
    public R getSessionAdmin(HttpSession session) {
        return adminService.getSessionAdmin(session);
    }

    @PostMapping("/admin/logout")
    public R logout(HttpSession session) {
        session.removeAttribute("adminId");
        session.removeAttribute("adminName");
        session.removeAttribute("name");
        session.invalidate();
        return R.success("??????");
    }

    @DeleteMapping("/admin/delete")
    public R deleteAdmin(@RequestParam Integer id, HttpSession session) {
        return adminService.deleteAdmin(id, session);
    }

}
