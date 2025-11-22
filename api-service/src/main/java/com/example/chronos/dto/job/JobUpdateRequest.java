package com.example.chronos.dto.job;

public class JobUpdateRequest {

    private String name;
    private String targetUrl;
    private String cronExpression;
    private Integer priority;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Long backoffSeconds;
    private String webhookUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Long getBackoffSeconds() { return backoffSeconds; }
    public void setBackoffSeconds(Long backoffSeconds) { this.backoffSeconds = backoffSeconds; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
