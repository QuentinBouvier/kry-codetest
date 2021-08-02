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

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostServiceRoute extends BaseMainVerticleTest {
    @Test
    @DisplayName("POST /service and get 201 and a new service is added")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_201_and_add_a_service(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        final String randomName = UUID.randomUUID().toString();
        ServiceStatus body = new ServiceStatus();
        body.setName(randomName);
        body.setUrl("https://example.com/");
        JsonObject bodyAsJson = body.toJson();
        WebClient client = WebClient.create(vertx);

        // Act
        client.post(APP_PORT, "localhost", "/service")
                .sendJson(bodyAsJson, testContext.succeeding(response -> {
                    // Assert (1)
                    testContext.verify(() -> {
                        assertEquals(201, response.statusCode());
                    });

                    // Assert (2)
                    this.connector.query("select * from service where name = '" + randomName + "';")
                            .setHandler(result -> {
                                testContext.verify(() -> {
                                    List<JsonObject> results = result.result().getRows();
                                    assertEquals(1, (long) results.size());
                                    assertEquals(randomName, results.get(0).getString("name"));

                                    testContext.completeNow();
                                });
                            });

                }));
    }

    @Test
    @DisplayName("POST /service and get 400 when a the service's name already exists")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_if_the_service_name_already_exists_in_base(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String randomName = UUID.randomUUID().toString();
        long date = new Date().getTime();
        final ServiceStatus newService = new ServiceStatus();
        newService.setUrl("https://bar.com");
        newService.setName(randomName);
        this.connector.query("insert into service (url, name, created_at) values ('https://foo.com', '" + randomName + "', " + date + ")")
                .setHandler(queryResult -> {
                    if (queryResult.failed()) {
                        testContext.failNow(queryResult.cause());
                    } else {
                        // Act
                        WebClient.create(vertx)
                                .post(APP_PORT, "localhost", "/service")
                                .sendJson(newService, testContext.succeeding(response -> {
                                    // Assert
                                    testContext.verify(() -> {
                                       assertEquals(400, response.statusCode());
                                       assertEquals("Service with this name already exist", response.body().toString());
                                       testContext.completeNow();
                                    });
                                }));
                    }
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
                .sendJson(bodyAsJson, testContext.succeeding(response -> {
                    // Assert (1)
                    testContext.verify(() -> {
                        assertEquals(400, response.statusCode());
                        assertEquals("The provided url is invalid", response.bodyAsString());
                        testContext.completeNow();
                    });
                }));
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
                .sendJson(bodyAsJson, testContext.succeeding(response -> {
                    // Assert
                    testContext.verify(() -> {
                        assertEquals(400, response.statusCode());
                        assertEquals("url and name are mandatory", response.bodyAsString());
                        testContext.completeNow();
                    });
                }));
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