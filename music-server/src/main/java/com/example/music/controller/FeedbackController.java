package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.FeedbackRequest;
import com.example.music.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/feedback/add")
    public R addFeedback(@RequestBody FeedbackRequest feedbackRequest, HttpSession session) {
        return feedbackService.addFeedback(feedbackRequest, session);
    }

    @GetMapping("/feedback/my")
    public R myFeedback(HttpSession session) {
        return feedbackService.myFeedback(session);
    }

    @GetMapping("/feedback/admin/all")
    public R allFeedback(HttpSession session) {
        return feedbackService.allFeedback(session);
    }

    @PostMapping("/feedback/admin/status")
    public R updateFeedbackStatus(@RequestParam Integer id, @RequestParam Integer status, HttpSession session) {
        return feedbackService.updateFeedbackStatus(id, status, session);
    }

    @DeleteMapping("/feedback/admin/delete")
    public R deleteFeedback(@RequestParam Integer id, HttpSession session) {
        return feedbackService.deleteFeedback(id, session);
    }
}
