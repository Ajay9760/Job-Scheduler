package com.example.chronos.worker;

import static org.junit.jupiter.api.Assertions.*;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.support.UrlSafetyValidator;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpJobExecutorTest {
    static HttpServer server;
    static int port;
    static final AtomicReference<String> lastMethod = new AtomicReference<>();

    @BeforeAll
    static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", ex -> {
            lastMethod.set(ex.getRequestMethod());
            byte[] b = "hello".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.createContext("/boom", ex -> { ex.sendResponseHeaders(500, -1); ex.close(); });
        server.createContext("/slow", ex -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/big", ex -> {
            byte[] b = new byte[200_000];
            Arrays.fill(b, (byte) 'x');
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stop() { server.stop(0); }

    private Job job(String path, HttpMethodType m, int timeout) {
        Job j = new Job();
        j.setTargetUrl("http://127.0.0.1:" + port + path);
        j.setHttpMethod(m);
        j.setTimeoutSeconds(timeout);
        return j;
    }

    private final HttpJobExecutor allowLocal = new HttpJobExecutor(new UrlSafetyValidator(true));

    @Test
    void successRecordsStatusAndBody() {
        var r = allowLocal.execute(job("/ok", HttpMethodType.POST, 5));
        assertTrue(r.success());
        assertEquals(200, r.status());
        assertEquals("hello", r.body());
        assertEquals("POST", lastMethod.get());
    }

    @Test
    void non2xxIsFailureNotTimeout() {
        var r = allowLocal.execute(job("/boom", HttpMethodType.GET, 5));
        assertFalse(r.success());
        assertFalse(r.timeout());
        assertEquals(500, r.status());
    }

    @Test
    void perJobTimeoutIsEnforced() {
        var r = allowLocal.execute(job("/slow", HttpMethodType.GET, 1));
        assertFalse(r.success());
        assertTrue(r.timeout());
        assertTrue(r.durationMs() < 2500, "should abort near 1s, took " + r.durationMs());
    }

    @Test
    void redirectsAreNotFollowed() {
        var r = allowLocal.execute(job("/redirect", HttpMethodType.GET, 5));
        assertEquals(302, r.status());
        assertFalse(r.success());
    }

    @Test
    void responseBodyIsCapped() {
        var r = allowLocal.execute(job("/big", HttpMethodType.GET, 5));
        assertTrue(r.success());
        assertEquals(64 * 1024, r.body().length());
    }

    @Test
    void ssrfGuardBlocksLoopbackWhenPrivateTargetsDisallowed() {
        var strict = new HttpJobExecutor(new UrlSafetyValidator(false));
        var r = strict.execute(job("/ok", HttpMethodType.GET, 5));
        assertFalse(r.success());
        assertNull(r.status());
        assertNotNull(r.error());
    }
}
