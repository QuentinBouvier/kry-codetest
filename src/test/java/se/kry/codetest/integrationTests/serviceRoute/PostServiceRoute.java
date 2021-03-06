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
import io.vertx.reactivex.sqlclient.RowSet;
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

    private final static String URI = BASE_URI + "/service";

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
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, URI)
                .rxSendJson(bodyAsJson);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() ->
                        assertEquals(201, response.statusCode()))
                )
                // Verify the DB
                .flatMap(x -> this.connector.query("select * from service where name = '" + randomName + "';").toSingle())
                .doOnSuccess(rows -> testContext.verify(() -> {
                    List<JsonObject> results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .collect(Collectors.toList());

                    assertEquals(1, (long) results.size());
                    assertEquals(randomName, results.get(0).getString("name"));

                    testContext.completeNow();
                })).subscribe();
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
        Single<RowSet<Row>> prepareQuery =
                this.connector.query("insert into service (url, name, created_at) values ('https://foo.com', '" + randomName + "', " + date + ")")
                        .toSingle();

        // Act
        Single<HttpResponse<Buffer>> responseFuture = prepareQuery
                .flatMap(rows ->
                        WebClient.create(vertx)
                                .post(APP_PORT, BASE_HOST, URI)
                                .rxSendJsonObject(newService.toJson())
                );

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Service with this name already exist", response.body().toString());
                    testContext.completeNow();
                }))
                .doOnError(testContext::failNow)
                .subscribe();
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
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, URI)
                .rxSendJsonObject(bodyAsJson);

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

    @ParameterizedTest(name = "POST /service and get 400 when {1} is missing")
    @DisplayName("POST /service and get 400 when mandatory field is missing")
    @MethodSource("incompleteRequestBody")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_post_should_return_400_if_mandatory_parameter_is_missing(ServiceStatus body, String unusedName, Vertx vertx, VertxTestContext testContext) {
        // Arrange
        JsonObject bodyAsJson = body.toJson();

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, URI)
                .rxSendJsonObject(bodyAsJson);

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("url and name are mandatory", response.bodyAsString());
                    testContext.completeNow();
                }))
                .doOnError(testContext::failNow)
                .subscribe();
    }

    @Test
    @DisplayName("POST /service/ returns 400 if the payload is not JSON")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_put_should_return_http_400_if_body_is_not_json(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        String body = "foobar";

        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .post(APP_PORT, BASE_HOST, URI)
                .rxSendBuffer(Buffer.buffer(body));

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    testContext.completeNow();
                })).subscribe();
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
