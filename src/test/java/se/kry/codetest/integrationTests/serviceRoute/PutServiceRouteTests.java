package se.kry.codetest.integrationTests.serviceRoute;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Row;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import se.kry.codetest.integrationTests.BaseMainVerticleIntegrationTest;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.model.ServiceStatusValueEnum;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class PutServiceRouteTests extends BaseMainVerticleIntegrationTest {

    private final ServiceStatus defaultService = ServiceStatus.fromJson(new JsonObject()
            .put("url", "https://example.com")
            .put("name", "foo")
            .put("status", "OK"));

    @Override
    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        this.connector.query("insert into service (url, name, created_at, status) " +
                        String.format(
                                "values ('%s', '%s', %d, '%s');",
                                defaultService.getUrl(), defaultService.getName(),
                                new DateTime().getMillis(), defaultService.getStatus().name()
                        )
                )
                .doOnSuccess(result -> testContext.completeNow())
                .subscribe();
    }

    @Test
    @DisplayName("PUT /service/:name get 200 and a the given service is updated when success")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_put_should_return_200_and_edit_entity_on_success(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String newName = "bar";
        JsonObject newStatus = defaultService.toJson();
        newStatus.put("name", newName);

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .put(APP_PORT, BASE_HOST, String.format("/service/%s", defaultService.getName()))
                .rxSendJsonObject(newStatus);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() ->
                        assertEquals(200, response.statusCode()))
                )
                // Verify in the DB
                .flatMap(x -> this.connector.query("select * from service where name = '" + newName + "'").toSingle())
                .doOnSuccess(rows -> testContext.verify(() -> {
                    JsonObject results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .findFirst()
                            .orElse(null);

                    assertNotNull(results);
                    assertEquals(results.getString("name"), newName);

                    testContext.completeNow();
                })).subscribe();
    }

    @Test
    @DisplayName("PUT /service/:name resets the status of the service to UNKNOWN")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_put_should_reset_service_status_to_unknown(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String newName = "bar";
        JsonObject newStatus = defaultService.toJson();
        newStatus.put("name", newName);

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .put(APP_PORT, BASE_HOST, String.format("/service/%s", defaultService.getName()))
                .rxSendJsonObject(newStatus);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() ->
                        assertEquals(200, response.statusCode()))
                )
                // Verify in the DB
                .flatMap(x -> this.connector.query("select * from service where name = '" + newName + "'").toSingle())
                .doOnSuccess(rows -> testContext.verify(() -> {
                    JsonObject results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .findFirst()
                            .orElse(null);

                    assertNotNull(results);
                    assertEquals(results.getString("status"), ServiceStatusValueEnum.UNKNOWN.name());

                    testContext.completeNow();
                })).subscribe();
    }

    @Test
    @DisplayName("PUT /service/:name returns 400 if the payload is not JSON compliant")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_put_should_return_http_400_if_body_is_not_json(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String body = "foobar";

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .put(APP_PORT, BASE_HOST, String.format("/service/%s", defaultService.getName()))
                .rxSendBuffer(Buffer.buffer(body));

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    testContext.completeNow();
                })).subscribe();
    }

    @ParameterizedTest(name = "PUT /service/:name and get 400 when {1} is missing")
    @DisplayName("PUT /service/:name and get 400 when mandatory field is missing")
    @MethodSource("incompleteRequestBody")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_put_should_return_http_400_if_name_or_url_are_missing(ServiceStatus body, String unusedName, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        JsonObject bodyJson = body.toJson();

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .put(APP_PORT, BASE_HOST, String.format("/service/%s", defaultService.getName()))
                .rxSendJsonObject(bodyJson);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    testContext.completeNow();
                })).subscribe();
    }

    @ParameterizedTest(name = "PUT /service:name returns 400 with invalid url \"{0}\"")
    @DisplayName("PUT /service:name returns 400 when url is invalid")
    @ValueSource(strings = {"www.example.com", "example.com", "123", "toto", "toto/"})
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_when_url_is_invalid(String url, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        JsonObject body = new JsonObject()
                .put("name", "foo")
                .put("url", url);

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, "/service")
                .rxSendJsonObject(body);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("The provided url is invalid", response.bodyAsString());
                    testContext.completeNow();
                }))
                .doOnError(testContext::failNow)
                .subscribe();
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
