package com.hayesbarber.playerstatus;

import lombok.Builder;

import java.util.List;

@Builder
public class WebhookPayload {
    private String content;
    private String username;
    private List<Embed> embeds;

    @Builder
    static class Embed {
        private String title;
        private String description;
        private List<Field> fields;
    }

    @Builder
    static class Field {
        private String name;
        private String value;
    }
}
