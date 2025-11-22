package com.example.chronos.repository;

import com.example.chronos.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);
}
