package com.example.music.model.request;

import lombok.Data;

import java.util.List;

@Data
public class RecommendBootstrapRequest {
    private List<Integer> likedSingerIds;
    private List<Integer> likedSongIds;
    private List<String> likedStyles;
}
