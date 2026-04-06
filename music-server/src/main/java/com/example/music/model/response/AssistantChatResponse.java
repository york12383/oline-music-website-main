package com.example.music.model.response;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatResponse {
    private String reply;

    private String action;

    private String actionTarget;

    private String searchKeyword;

    private Boolean usedImport;

    private Boolean needsConfirmation;

    private Double confidence;

    private String confirmationPrompt;

    private String confirmationAction;

    private String confirmationTarget;

    private AssistantSongPayload song;

    private List<AssistantSongPayload> candidates;
}
