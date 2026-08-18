package com.sodasplash.auth_service.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private Long id;

    private String username;

    private String email;

    private String role;
}
