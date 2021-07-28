package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.model.ServiceStatus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(8083), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Start a web server on localhost (8083), query url /example and get a 404 response")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(8083, "::1", "/example")
                .send(response -> testContext.verify(() -> {
                    assertEquals(404, response.result().statusCode());
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Start a web server on localhost (8083), query POST /service and get 201 and a service is added")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_201_and_add_a_service(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String randomName = UUID.randomUUID().toString();
        ServiceStatus body = new ServiceStatus();
        body.setName(randomName);
        body.setUrl("https://example.com/");
        WebClient client = WebClient.create(vertx);

        // Act
        client.post(8083, "localhost", "/service")
                .sendJson(body.toJson(), response -> {
                    // Assert (1)
                    assertEquals(201, response.result().statusCode());

                    client.get(8083, "localhost", "/service")
                            .send(response2 -> {
                                boolean hasNewService = response2.result().bodyAsJsonArray().stream()
                                        .anyMatch(x -> ((JsonObject) x).getString("name").equals(randomName));

                                // Assert (2)
                                assertTrue(hasNewService);
                                testContext.completeNow();
                            });

                });
    }

    @Test
    @DisplayName("Start a web server on localhost (8083), query GET /service and get 200 with list")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_get_should_send_a_200_status_with_a_list(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(8083, "localhost", "/service")
                .send(response -> {
                    HttpResponse<Buffer> result = response.result();

                    // Assert
                    assertDoesNotThrow(result::bodyAsJsonArray);
                    assertEquals(200, response.result().statusCode());
                    testContext.completeNow();
                });

    }
}
