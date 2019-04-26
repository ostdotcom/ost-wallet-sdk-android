/*
 * Copyright 2019 OST.com Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */

package ost.com.demoapp.entity;

import android.util.Log;

import com.ost.walletsdk.OstSdk;
import com.ost.walletsdk.models.entities.OstUser;

import org.json.JSONException;
import org.json.JSONObject;

public class LogInUser {
    private static final String TAG = "OstLogInUser";
    private static final String APP_USER_ID = "app_user_id";
    private static final String TOKEN_ID = "token_id";
    private static final String USER_NAME = "username";
    private static final String USER_ID = "user_id";
    private static final String USER_PIN_SALT = "user_pin_salt";

    private String id;
    private String ostUserId;
    private String tokenId;
    private String userPinSalt;
    private String userName;

    public static LogInUser newInstance(JSONObject jsonObject) {
        try {
            String id = jsonObject.getString(APP_USER_ID);
            String userName = jsonObject.getString(USER_NAME);
            String tokenId = jsonObject.getString(TOKEN_ID);
            String ostUserId = jsonObject.getString(USER_ID);
            String userPinSalt = jsonObject.getString(USER_PIN_SALT);
            return new LogInUser(id, userName, tokenId, ostUserId, userPinSalt);
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception", e.getCause());
        }
        return null;
    }

    public LogInUser(String id, String userName, String tokenId, String ostUserId, String userPinSalt) {
        this.id = id;
        this.userName = userName;
        this.tokenId = tokenId;
        this.ostUserId = ostUserId;
        this.userPinSalt = ostUserId;
    }

    public String getId() {
        return id;
    }

    public String getOstUserId() {
        return ostUserId;
    }

    public OstUser getOstUser() {
        return OstSdk.getUser(ostUserId);
    }


    public String getTokenId() {
        return tokenId;
    }

    public String getUserPinSalt() {
        return userPinSalt;
    }

    public String getUserName() {
        return userName;
    }
}