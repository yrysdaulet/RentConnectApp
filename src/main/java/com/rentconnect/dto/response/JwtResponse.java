package com.rentconnect.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String picture;
    private List<String> roles;

    public JwtResponse(String token, Long id, String email, String firstName, String lastName, String picture, List<String> roles) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.picture = picture;
        this.roles = roles;
    }
}

