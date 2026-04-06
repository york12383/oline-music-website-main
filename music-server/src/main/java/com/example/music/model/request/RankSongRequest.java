package com.example.music.model.request;

import lombok.Data;



@Data
public class RankSongRequest {
    private Long id;

    private Integer songId;

    private Integer consumerId;

    private Integer score;
}
