package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.service.WebhookNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebhookNotificationServiceTest {

    @Test
    void notifyDoesNothingWhenWebhookUrlIsBlank() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient client = mock(WebClient.class);
        when(builder.build()).thenReturn(client);

        WebhookNotificationService service = new WebhookNotificationService(builder);

        Job job = new Job();
        job.setId(1L);
        job.setWebhookUrl("   "); // blank

        service.notify(job, true, "ok");

        // should not try to send HTTP at all
        verify(client, never()).post();
    }

    @Test
    void notifySendsPostWhenWebhookUrlPresent() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient client = mock(WebClient.class);
        when(builder.build()).thenReturn(client);

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        // We don't care what this returns, just that the chain doesn't blow up
        when(client.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);


        WebhookNotificationService service = new WebhookNotificationService(builder);

        Job job = new Job();
        job.setId(42L);
        job.setExternalId("ext-1");
        job.setWebhookUrl("https://example.com/hook");

        service.notify(job, true, "result");

        // Only verify essential behavior: POST to the right URL
        verify(client).post();
        verify(uriSpec).uri("https://example.com/hook");
        // no expectations on retrieve(), toBodilessEntity(), etc.
    }

    @Test
    void notifySwallowsExceptionsFromHttpCall() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient client = mock(WebClient.class);
        when(builder.build()).thenReturn(client);

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(client.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.bodyValue(any())).thenThrow(new RuntimeException("boom"));

        WebhookNotificationService service = new WebhookNotificationService(builder);

        Job job = new Job();
        job.setId(99L);
        job.setWebhookUrl("https://example.com/hook");

        // Even if the HTTP call explodes, notify() should not propagate
        assertThatCode(() -> service.notify(job, false, "fail"))
                .doesNotThrowAnyException();
    }
}
