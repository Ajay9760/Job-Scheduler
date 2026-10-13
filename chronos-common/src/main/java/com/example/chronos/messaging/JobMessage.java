package com.example.chronos.messaging;

/** Message on the broker. Deliberately tiny: the database row is the source of truth. */
public record JobMessage(Long instanceId, Long jobId) {}
