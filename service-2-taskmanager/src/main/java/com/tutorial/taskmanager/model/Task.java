package com.tutorial.taskmanager.model;

import java.time.LocalDateTime;

public class Task {
    
    private long id;
    private String title;
    private String description;
    private boolean completed;
    private String createdAt;

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public String getCreatedAt() { return createdAt; }

    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Task(long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = false;
        this.createdAt = LocalDateTime.now().toString();
    }
}
