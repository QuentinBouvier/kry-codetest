package se.kry.codetest.integrationTests;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainVerticleTest extends BaseMainVerticleTest {
    @Test
    @DisplayName("query url /example gives 404 http response")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(APP_PORT, "::1", "/example")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(404, response.statusCode());
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("Query url / gives 200 http response")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void root_route_returns_200_http_response(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(APP_PORT, "localhost", "/")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    testContext.completeNow();
                })));
    }
}