package com.sodasplash.order_service.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "delivery.location")
public class DeliveryProperties {

    private double latitude;
    private double longitude;
    private double maxDistanceKm;
}
