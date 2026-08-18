package com.sodasplash.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<CreateOrderItemRequest> items;

    @NotBlank(message = "Referral source is required")
    private String referralSource;

    private String referralName;

    @Email(message = "Invalid referral email")
    private String referralEmail;

    private String deliveryNote;
}