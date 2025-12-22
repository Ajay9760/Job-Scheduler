package com.example.chronos.dto.job;


import java.util.List;

@lombok.Data
public class BulkTriggerRequest {
    private List<Long> jobIds;
}


