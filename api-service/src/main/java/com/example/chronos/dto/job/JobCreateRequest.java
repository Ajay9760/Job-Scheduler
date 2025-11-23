package com.example.chronos.dto.job;

import com.example.chronos.domain.enums.HttpMethodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Valid
public class JobCreateRequest {



    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Pattern(
            regexp = "https?://.+",
            message = "targetUrl must be a valid HTTP/HTTPS URL"
    )
    @NotBlank
    @Size(max = 1000)
    private String targetUrl;

    @NotNull
    private HttpMethodType httpMethod;

    @Size(max = 4000)
    private String requestBody;

    @Min(0) @Max(10)
    private int priority = 5;

    @Min(1)
    private int timeoutSeconds = 30;

    @Min(0)
    private int maxRetries = 3;

    @Min(0)
    private long backoffSeconds = 30;

    @Size(max = 1000)
    private String webhookUrl;

    private String cronExpression;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public HttpMethodType getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethodType httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getBackoffSeconds() { return backoffSeconds; }
    public void setBackoffSeconds(long backoffSeconds) { this.backoffSeconds = backoffSeconds; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
}
