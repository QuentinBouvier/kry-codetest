package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;
import se.kry.codetest.model.ServiceStatus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostServiceRoute extends BaseMainVerticleTest {
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
        client.post(APP_PORT, "localhost", "/service")
                .sendJson(body.toJson(), response -> {
                    // Assert (1)
                    assertEquals(201, response.result().statusCode());

                    client.get(APP_PORT, "localhost", "/service")
                            .send(response2 -> {
                                boolean hasNewService = response2.result().bodyAsJsonArray().stream()
                                        .anyMatch(x -> ((JsonObject) x).getString("name").equals(randomName));

                                // Assert (2)
                                assertTrue(hasNewService);
                                testContext.completeNow();
                            });

                });
    }
}
