package com.example.backend.auth.dto.Responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    private String message;
    private String profileImageUrl;

    // for messaging and error handling
    public RegisterResponse(String message){
        this.message = message;
    }
}
