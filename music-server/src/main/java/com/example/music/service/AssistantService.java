package com.example.music.service;

import com.example.music.common.R;
import com.example.music.model.request.AssistantChatRequest;

import javax.servlet.http.HttpSession;

public interface AssistantService {
    R chat(AssistantChatRequest request, HttpSession session);
}
