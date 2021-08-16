package se.kry.codetest.integrationTests;

import io.reactivex.Single;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest extends BaseMainVerticleIntegrationTest {
    @Test
    @DisplayName("query url /example gives 404 http response")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .get(APP_PORT, BASE_HOST, "/example")
                .rxSend();

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(404, response.statusCode());
                    testContext.completeNow();
                })).subscribe();
    }

    @Test
    @DisplayName("Query url / gives 200 http response")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void root_route_returns_200_http_response(Vertx vertx, VertxTestContext testContext) {
        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .get(APP_PORT, BASE_HOST, "/")
                .rxSend();

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    testContext.completeNow();
                })).subscribe();
    }
}
