package com.example.feedbackloops.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserHistoryRequest {
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("listeningHistory")
    private String listeningHistory;
    
    public UserHistoryRequest() {}
    
    public UserHistoryRequest(String userId, String listeningHistory) {
        this.userId = userId;
        this.listeningHistory = listeningHistory;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getListeningHistory() {
        return listeningHistory;
    }
    
    public void setListeningHistory(String listeningHistory) {
        this.listeningHistory = listeningHistory;
    }
}
