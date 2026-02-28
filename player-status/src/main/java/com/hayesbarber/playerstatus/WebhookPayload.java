package com.hayesbarber.playerstatus;

import lombok.Builder;

import java.util.List;

@Builder
public class WebhookPayload {
    private List<Embed> embeds;

    @Builder
    static class Embed {
        private String title;
        private String description;
        private Integer color;
        private Thumbnail thumbnail;
        private Footer footer;
    }

    @Builder
    static class Thumbnail {
        private String url;
    }

    @Builder
    static class Footer {
        private String text;
    }
}
