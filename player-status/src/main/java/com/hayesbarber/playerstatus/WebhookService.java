package com.hayesbarber.playerstatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WebhookService {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String WEBHOOK_URL = "";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<Integer> sendMessage(WebhookPayload payload) {
        final String json = gson.toJson(payload);
        final HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .uri(URI.create(WEBHOOK_URL))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode);
    }
}
