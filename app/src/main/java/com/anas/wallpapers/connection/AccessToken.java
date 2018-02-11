package com.anas.wallpapers.connection;

import com.google.gson.JsonObject;

public class AccessToken {
    private final String access_token;
    private final int expires_in;
    private final String scope;
    private final String token_type;

    public AccessToken(String access_token, String token_type, int expires_in, String scope) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.scope = scope;
    }

    public String getAccessToken() {
        return access_token;
    }

    public String toString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("access_token", access_token);
        jsonObject.addProperty("token_type", token_type);
        jsonObject.addProperty("expires_in", expires_in);
        jsonObject.addProperty("scope", scope);
        return jsonObject.toString();
    }
}
