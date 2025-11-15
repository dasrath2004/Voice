package com.example.voicetaskmanager;

public class Task {
    private String id;
    private String title;
    private String description;
    private long deadline;
    private String priority; // High / Medium / Low
    private String status;   // ongoing / completed / failed
    private long createdAt;
    private long reminderTime;
    private String userId; // for production rules

    public Task() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
