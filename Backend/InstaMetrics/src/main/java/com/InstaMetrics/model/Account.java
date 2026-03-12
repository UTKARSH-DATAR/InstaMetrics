package com.InstaMetrics.model;

public class Account {
	
	// Basic representation of an Instagram account used throughout the app.
    // username  -> Instagram handle (from "value" or "title" in JSON)
    // profileUrl -> Link to the profile ("href" in JSON export)
    // timestamp  -> Optional, used mainly for pending follow requests
    private String username;
    private String profileUrl;
    private Long timestamp;

    public Account() {
    }

    public Account(String username, String profileUrl, Long timestamp) {
        this.username = username;
        this.profileUrl = profileUrl;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}