package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Feedback;
import com.example.music.model.request.FeedbackRequest;

import javax.servlet.http.HttpSession;

public interface FeedbackService extends IService<Feedback> {

    R addFeedback(FeedbackRequest feedbackRequest, HttpSession session);

    R myFeedback(HttpSession session);

    R allFeedback(HttpSession session);

    R updateFeedbackStatus(Integer id, Integer status, HttpSession session);

    R deleteFeedback(Integer id, HttpSession session);
}
