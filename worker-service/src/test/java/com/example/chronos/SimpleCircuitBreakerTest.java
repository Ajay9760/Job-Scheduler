package com.example.chronos;

import com.example.chronos.service.SimpleCircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCircuitBreakerTest {

    @Test
    void isOpenIsFalseBeforeFailureThreshold() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
        String key = "https://example.com";

        // initially closed
        assertThat(cb.isOpen(key)).isFalse();

        // 1st failure
        cb.onFailure(key);
        assertThat(cb.isOpen(key)).isFalse();

        // 2nd failure (still below threshold = 3)
        cb.onFailure(key);
        assertThat(cb.isOpen(key)).isFalse();
    }

    @Test
    void becomesOpenAfterFailureThreshold() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
        String key = "https://example.com";

        cb.onFailure(key);
        cb.onFailure(key);
        cb.onFailure(key); // 3rd failure reaches threshold

        assertThat(cb.isOpen(key)).isTrue();
    }

    @Test
    void onSuccessResetsState() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
        String key = "https://example.com";

        cb.onFailure(key);
        cb.onFailure(key);
        cb.onFailure(key);
        assertThat(cb.isOpen(key)).isTrue();

        cb.onSuccess(key);

        assertThat(cb.isOpen(key)).isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
        String key1 = "https://a.example.com";
        String key2 = "https://b.example.com";

        cb.onFailure(key1);
        cb.onFailure(key1);
        cb.onFailure(key1);

        assertThat(cb.isOpen(key1)).isTrue();
        assertThat(cb.isOpen(key2)).isFalse();
    }
}
