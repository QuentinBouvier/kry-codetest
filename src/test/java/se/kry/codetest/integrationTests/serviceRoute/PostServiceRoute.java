package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import se.kry.codetest.integrationTests.BaseMainVerticleIntegrationTest;
import se.kry.codetest.model.ServiceStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class PostServiceRoute extends BaseMainVerticleIntegrationTest {
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

        // Act
        Future<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, "/service")
                .sendJson(bodyAsJson);

        // Assert
        responseFuture
                .onSuccess(response -> testContext.verify(() ->
                        assertEquals(201, response.statusCode()))
                )
                // Verify the DB
                .compose(x -> this.connector.query("select * from service where name = '" + randomName + "';"))
                .onSuccess(rows -> testContext.verify(() -> {
                    List<JsonObject> results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .collect(Collectors.toList());

                    assertEquals(1, (long) results.size());
                    assertEquals(randomName, results.get(0).getString("name"));

                    testContext.completeNow();
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
        Future<RowSet<Row>> prepareQuery = this.connector.query("insert into service (url, name, created_at) values ('https://foo.com', '" + randomName + "', " + date + ")");

        // Act
        Future<HttpResponse<Buffer>> responseFuture = prepareQuery
                .compose(rows -> WebClient.create(vertx)
                        .post(APP_PORT, BASE_HOST, "/service")
                        .sendJsonObject(newService.toJson())
                );

        // Assert
        responseFuture
                .onSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Service with this name already exist", response.body().toString());
                    testContext.completeNow();
                }))
                .onFailure(testContext::failNow);
    }

    @ParameterizedTest(name = "POST /service and get 400 with invalid url \"{0}\"")
    @DisplayName("POST /service and get 400 when url is invalid")
    @ValueSource(strings = {"www.example.com", "example.com", "123", "toto", "toto/"})
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_when_url_is_invalid(String url, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String randomName = UUID.randomUUID().toString();
        ServiceStatus body = new ServiceStatus();
        body.setName(randomName);
        body.setUrl(url);
        JsonObject bodyAsJson = body.toJson();

        // Act
        Future<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, "/service")
                .sendJsonObject(bodyAsJson);

        // Assert
        responseFuture
                .onSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("The provided url is invalid", response.bodyAsString());
                    testContext.completeNow();
                }))
                .onFailure(testContext::failNow);
    }

    @ParameterizedTest(name = "POST /service and get 400 when {1} is missing")
    @DisplayName("POST /service and get 400 when mandatory field is missing")
    @MethodSource("incompleteRequestBody")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_if_mandatory_parameter_is_missing(ServiceStatus body, String unusedName, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        JsonObject bodyAsJson = body.toJson();

        // Act
        Future<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, "/service")
                .sendJsonObject(bodyAsJson);

        // Assert
        responseFuture
                .onSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("url and name are mandatory", response.bodyAsString());
                    testContext.completeNow();
                }))
                .onFailure(testContext::failNow);
    }

    static Stream<Arguments> incompleteRequestBody() {
        ServiceStatus noUrl = new ServiceStatus();
        noUrl.setName("foo");
        ServiceStatus noName = new ServiceStatus();
        noName.setUrl("http://example.com");
        return Stream.of(
                Arguments.of(noUrl, "url"),
                Arguments.of(noName, "name")
        );
    }
}
