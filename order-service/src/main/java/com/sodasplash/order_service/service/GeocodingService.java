package com.sodasplash.order_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sodasplash.order_service.client.GeoapifyClient;
import com.sodasplash.order_service.config.DeliveryProperties;
import com.sodasplash.order_service.dto.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeoapifyClient geoapifyClient;
    private final DeliveryProperties deliveryProperties;

    public GeocodingResult geocode(
            String address,
            String pincode
    ) {
        try {
            String query = address + ", " + pincode + ", India";
            JsonNode response = geoapifyClient.geocode(query);

            if (response != null) {
                JsonNode results = response.get("results");
                if (results != null && !results.isEmpty()) {
                    JsonNode firstResult = results.get(0);
                    double latitude = firstResult.get("lat").asDouble();
                    double longitude = firstResult.get("lon").asDouble();
                    return new GeocodingResult(latitude, longitude);
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for address: {}, pincode: {}. Using default store location fallback. Error: {}", address, pincode, e.getMessage());
        }

        // Fallback to store coordinates if geocoding fails or external service is unavailable
        return new GeocodingResult(
                deliveryProperties.getLatitude() != 0 ? deliveryProperties.getLatitude() : 14.4673,
                deliveryProperties.getLongitude() != 0 ? deliveryProperties.getLongitude() : 78.8242
        );
    }
}

