package com.example.music.model.request;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatRequest {
    private String message;

    private List<AssistantHistoryMessage> history;

    private Boolean allowImport;
}
