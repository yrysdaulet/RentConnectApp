package com.rentconnect.security.oauth2.user;

import java.util.Map;

public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getFirstName() {
        String name = (String) attributes.get("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }
        return name;
    }

    @Override
    public String getLastName() {
        String name = (String) attributes.get("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[1];
        }
        return "";
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}

