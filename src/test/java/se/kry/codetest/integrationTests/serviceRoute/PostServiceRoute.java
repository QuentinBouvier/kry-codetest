package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;
import se.kry.codetest.model.ServiceStatus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostServiceRoute extends BaseMainVerticleTest {
    @Test
    @DisplayName("POST /service and get 201 and a new service is added")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_201_and_add_a_service(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String randomName = UUID.randomUUID().toString();
        ServiceStatus body = new ServiceStatus();
        body.setName(randomName);
        body.setUrl("https://example.com/");
        JsonObject bodyAsJson = body.toJson();
        WebClient client = WebClient.create(vertx);

        // Act
        client.post(APP_PORT, "localhost", "/service")
                .sendJson(bodyAsJson, response -> {
                    // Assert (1)
                    testContext.verify(() -> {
                        assertEquals(201, response.result().statusCode());
                    });

                    client.get(APP_PORT, "localhost", "/service")
                            .send(response2 -> {
                                boolean hasNewService = response2.result().bodyAsJsonArray().stream()
                                        .anyMatch(x -> ((JsonObject) x).getString("name").equals(randomName));

                                // Assert (2)
                                testContext.verify(() -> {
                                    assertTrue(hasNewService);
                                    testContext.completeNow();
                                });
                            });

                });
    }

    @ParameterizedTest(name = "POST /service and get 400 because the url \"{0}\" is invalid")
    @ValueSource(strings = {"www.example.com", "example.com", "123", "toto", "toto/"})
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_when_url_is_invalid(String url, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String randomName = UUID.randomUUID().toString();
        ServiceStatus body = new ServiceStatus();
        body.setName(randomName);
        body.setUrl(url);
        JsonObject bodyAsJson = body.toJson();
        WebClient client = WebClient.create(vertx);

        // Act
        client.post(APP_PORT, "localhost", "/service")
                .sendJson(bodyAsJson, response -> {
                    // Assert (1)
                    testContext.verify(() -> {
                        assertEquals(400, response.result().statusCode());
                        assertEquals("The provided url is invalid", response.result().bodyAsString());
                        testContext.completeNow();
                    });
                });
    }

    @ParameterizedTest(name = "POST /service and get 400 when {1} is missing")
    @MethodSource("incompleteRequestBody")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_if_mandatory_parameter_is_missing(ServiceStatus body, String unusedName, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        JsonObject bodyAsJson = body.toJson();

        // Act
        WebClient.create(vertx)
                .post(APP_PORT, "localhost", "/service")
                .sendJson(bodyAsJson, response -> {
                    // Assert
                    testContext.verify(() -> {
                        assertEquals(400, response.result().statusCode());
                        assertEquals("url and name are mandatory", response.result().bodyAsString());
                    });

                    testContext.completeNow();
                });
    }

    static Stream<Arguments> incompleteRequestBody() {
        ServiceStatus noUrl = new ServiceStatus();
        noUrl.setName("toto");
        ServiceStatus noName = new ServiceStatus();
        noName.setUrl("http://example.com");
        return Stream.of(
                Arguments.of(noUrl, "url"),
                Arguments.of(noName, "name")
        );
    }
}
