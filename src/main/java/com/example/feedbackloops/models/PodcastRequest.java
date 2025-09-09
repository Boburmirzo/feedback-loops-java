package com.example.feedbackloops.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PodcastRequest {
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("transcript")
    private String transcript;
    
    public PodcastRequest() {}
    
    public PodcastRequest(String title, String transcript) {
        this.title = title;
        this.transcript = transcript;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTranscript() {
        return transcript;
    }
    
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
}
