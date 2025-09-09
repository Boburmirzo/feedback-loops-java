package com.example.feedbackloops.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PodcastRecommendation {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("similarity")
    private float similarity;
    
    public PodcastRecommendation() {}
    
    public PodcastRecommendation(String id, String title, String summary, float similarity) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.similarity = similarity;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public float getSimilarity() {
        return similarity;
    }
    
    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }
}
