package com.example.music.model.response;

import lombok.Data;

@Data
public class AssistantSongPayload {
    private Integer id;

    private String name;

    private String singerName;

    private String songTitle;

    private String introduction;

    private String pic;

    private String lyric;

    private String url;

    private Integer type;
}
