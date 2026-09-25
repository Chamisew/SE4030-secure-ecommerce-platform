package com.example.backend.auth.dto.Requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ExchangeRequest {

    @NotBlank(message = "OAuth provider is required (e.g. google)")
    private String provider;

    @NotBlank(message = "ID token or code is required")
    private String idToken;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    private String name;
    private String picture;
}
