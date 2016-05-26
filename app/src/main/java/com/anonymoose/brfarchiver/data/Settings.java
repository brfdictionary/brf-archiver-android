package com.anonymoose.brfarchiver.data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Copyright (c) 2016, Armen Tanzarian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of Armen Tanzarian nor the names of other
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class Settings {
    private String saveFilename;
    private String url;
    private String username;
    private String password;
    private String users;
    private boolean squelchNoise;
    private boolean downloadImages;
    private boolean isHttps;
    private Activity activity;
    private Context context;
    private Set<String> wantedUsers;

    /**
     * Constructor
     * @param saveFilename - The filename to save as.
     * @param url - URL
     * @param username - The username to login
     * @param password - The password to login
     * @param users - A list of users
     * @param squelchNoise - boolean
     * @param downloadImages - boolean
     * @param isHttps - boolean
     * @param activity - Handle to the activity object to update
     * @param context - Handle to Activity object to get resources from
     * @param wantedUsers - Set
     */
    private Settings(
            String saveFilename,
            String url,
            String username,
            String password,
            String users,
            boolean squelchNoise,
            boolean downloadImages,
            boolean isHttps,
            Activity activity,
            Context context,
            Set<String> wantedUsers
    ) {
        this.saveFilename = saveFilename;
        this.url = url;
        this.username = username;
        this.password = password;
        this.users = users;
        this.squelchNoise = squelchNoise;
        this.downloadImages = downloadImages;
        this.isHttps = isHttps;
        this.activity = activity;
        this.context = context;
        this.wantedUsers = wantedUsers;
    }

    /**
     * Do we want to keep this user's writings or not
     * @param user - The user to check against
     * @return true or false
     */
    public boolean isWantedUser(String user) {
        if (wantedUsers == null) {
            // All users are allowed if allowedUsers is null
            return true;
        }

        return wantedUsers.contains(user);
    }

    public static class Builder {
        private String saveFilename     = null;
        private String url              = null;
        private String username         = null;
        private String password         = null;
        private String users            = null;
        private boolean squelchNoise    = false;
        private boolean downloadImages  = false;
        private boolean isHttps         = false;
        private Activity activity       = null;
        private Context context         = null;
        private Set<String> wantedUsers = null;

        public Builder withSaveFilename(String value) {
            saveFilename = value;
            return this;
        }

        public Builder withUrl(String value) {
            url = value;
            return this;
        }

        public Builder withUsername(String value) {
            username = value;
            return this;
        }

        public Builder withPassword(String value) {
            password = value;
            return this;
        }

        public Builder withUsers(String value) {
            if (value != null && !value.isEmpty()) {
                users = value;
                wantedUsers = new HashSet<String>();
                String tmpUsers[] = users.split(",");
                for (String tmpUser : tmpUsers) {
                    tmpUser = tmpUser.trim().toLowerCase();
                    wantedUsers.add(tmpUser);
                }
            }
            return this;
        }

        public Builder withSquelchNoise(boolean value) {
            squelchNoise = value;
            return this;
        }

        public Builder withDownloadImages(boolean value) {
            downloadImages = value;
            return this;
        }

        public Builder withHttps(boolean value) {
            isHttps = value;
            return this;
        }

        public Builder withActivity(Activity value) {
            activity = value;
            return this;
        }

        public Builder withContext(Context value) {
            context = value;
            return this;
        }

        public Settings build() {
            return new Settings(saveFilename, url, username, password, users, squelchNoise, downloadImages, isHttps, activity, context, wantedUsers);
        }
    }

    public String getSaveFilename() { return saveFilename; }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUsers() {
        return users;
    }

    public boolean isSquelchNoise() {
        return squelchNoise;
    }

    public boolean isDownloadImages() {
        return downloadImages;
    }

    public boolean isHttps() { return isHttps; }

    public Activity getActivity() {
        return activity;
    }

    public Context getContext() { return context; }
}
