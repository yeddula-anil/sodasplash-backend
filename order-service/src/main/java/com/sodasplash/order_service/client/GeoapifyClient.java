package com.sodasplash.order_service.client;



import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeoapifyClient {

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyClient(
            @Value("${geoapify.base-url}") String baseUrl,
            @Value("${geoapify.api-key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    public JsonNode geocode(String address) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/geocode/search")
                        .queryParam("text", address)
                        .queryParam("filter", "countrycode:in")
                        .queryParam("format", "json")
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
}
