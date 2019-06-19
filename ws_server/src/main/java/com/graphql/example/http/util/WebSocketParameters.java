package com.graphql.example.http.util;

import java.util.Map;

public final class WebSocketParameters {

    private final String type;
    private final String id;
    private final QueryParameters payload;

    /**
     * Send by apollo client when WebSocket connection is established
     */
    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public QueryParameters getPayload() {
        return payload;
    }

    private WebSocketParameters(String type, String id, QueryParameters payload) {
        this.type = type;
        this.id = id;
        this.payload = payload;
    }

    public static WebSocketParameters from(String text) {
        final Map<String, Object> json = JsonKit.toMap(text);
        final String type = (String) json.get("type");
        final String id = (String) json.get("id");
        final QueryParameters payload = QueryParameters.from((Map<String, Object>) json.get("payload"));
        return new WebSocketParameters(type, id, payload);
    }
}
