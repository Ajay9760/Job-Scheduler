package com.example.chronos.worker;

import com.example.chronos.domain.Job;
import com.example.chronos.support.UrlSafetyValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

/**
 * Performs the HTTP call of a job. Safety properties: SSRF re-check right before the call, redirects are
 * NOT followed (a redirect could point at an internal address), per-job timeout, response body capped.
 */
@Component
public class HttpJobExecutor {

    public record Result(boolean success, boolean timeout, Integer status, String body, String error, long durationMs) {}

    private static final int MAX_BODY_BYTES = 64 * 1024;
    private final UrlSafetyValidator urlValidator;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public HttpJobExecutor(UrlSafetyValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    public Result execute(Job job) {
        long start = System.nanoTime();
        try {
            urlValidator.validate(job.getTargetUrl());
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(job.getTargetUrl()))
                    .timeout(Duration.ofSeconds(job.getTimeoutSeconds()));
            if (job.getRequestHeaders() != null) {
                job.getRequestHeaders().forEach(b::header);
            }
            String body = job.getRequestBody();
            HttpRequest.BodyPublisher pub = body == null || body.isEmpty()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);
            b.method(job.getHttpMethod().name(), pub);

            HttpResponse<InputStream> resp = client.sendAsync(b.build(), HttpResponse.BodyHandlers.ofInputStream())
                    .orTimeout(job.getTimeoutSeconds(), TimeUnit.SECONDS)
                    .join();
            String text;
            try (InputStream in = resp.body()) {
                text = new String(in.readNBytes(MAX_BODY_BYTES), StandardCharsets.UTF_8);
            }
            int code = resp.statusCode();
            boolean ok = code >= 200 && code < 300;
            return new Result(ok, false, code, text, ok ? null : "HTTP " + code, ms(start));
        } catch (CompletionException e) {
            Throwable c = e.getCause() == null ? e : e.getCause();
            boolean to = c instanceof TimeoutException || c instanceof java.net.http.HttpTimeoutException
                    || c instanceof HttpConnectTimeoutException;
            return new Result(false, to, null, null, c.getClass().getSimpleName() + ": " + c.getMessage(), ms(start));
        } catch (IllegalArgumentException | IOException e) {
            return new Result(false, false, null, null, e.getClass().getSimpleName() + ": " + e.getMessage(), ms(start));
        }
    }

    private static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
