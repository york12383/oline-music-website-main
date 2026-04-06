package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.AssistantChatRequest;
import com.example.music.service.AssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Autowired
    private AssistantService assistantService;

    @PostMapping("/chat")
    public R chat(@RequestBody AssistantChatRequest request, HttpSession session) {
        return assistantService.chat(request, session);
    }
}
