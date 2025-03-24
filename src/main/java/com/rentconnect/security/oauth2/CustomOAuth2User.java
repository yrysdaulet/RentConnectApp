package com.rentconnect.security.oauth2;

import com.rentconnect.security.services.UserDetailsImpl;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    @Getter
    private UserDetailsImpl userDetails;
    private Map<String, Object> attributes;

    public CustomOAuth2User(UserDetailsImpl userDetails, Map<String, Object> attributes) {
        this.userDetails = userDetails;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userDetails.getAuthorities();
    }

    @Override
    public String getName() {
        return userDetails.getUsername();
    }

}