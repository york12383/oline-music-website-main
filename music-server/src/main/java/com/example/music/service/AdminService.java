package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Admin;
import com.example.music.model.request.AdminRequest;

import javax.servlet.http.HttpSession;

public interface AdminService extends IService<Admin> {
    /**
     * ???????
     * @param adminRequest ????????????????
     * @param session HTTP???????????
     * @return ????????????????
     */
    R verityPasswd(AdminRequest adminRequest, HttpSession session);

    R addAdmin(AdminRequest adminRequest, HttpSession session);

    R getAdministrator(HttpSession session);

    R getSessionAdmin(HttpSession session);
    
    /**
     * ???????
     * @param id ???????ID
     * @param session HTTP?????????
     * @return ????
     */
    R deleteAdmin(Integer id, HttpSession session);
}
