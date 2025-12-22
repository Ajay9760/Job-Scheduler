package com.example.chronos.dto.auth;

@lombok.Data
@lombok.Builder
public  class BulkTriggerResponse {
    private int jobsRequested;
    private int jobsTriggered;
    private int jobsSkipped;
}
