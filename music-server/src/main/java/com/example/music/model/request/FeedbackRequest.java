package com.example.music.model.request;

import lombok.Data;

@Data
public class FeedbackRequest {
    private String feedbackType;

    private String title;

    private String content;

    private String contact;

    private String pagePath;
}
