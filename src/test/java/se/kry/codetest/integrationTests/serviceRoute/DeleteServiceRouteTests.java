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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.integrationTests.BaseMainVerticleIntegrationTest;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class DeleteServiceRouteTests extends BaseMainVerticleIntegrationTest {
    private static final String DELETE_SERVICE_NAME = "foo";
    private static final String DIFFERENT_NAME = "bar";

    @Override
    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        long date = new Date().getTime();
        this.connector.query("insert into service (url, name, created_at) " +
                        "values ('https://example.com', '" + DELETE_SERVICE_NAME + "', " + date + ");")
                .doOnSuccess(result -> testContext.completeNow())
                .subscribe();
    }

    @Test
    @DisplayName("DELETE /service/:name and gets 204 if the service was found. The service is deleted from the base")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_delete_should_remove_entry_when_name_is_found_and_return_204(Vertx vertx, VertxTestContext testContext) {
        // Act
        Single<HttpResponse<Buffer>> responseFuture = WebClient.create(vertx)
                .delete(APP_PORT, BASE_HOST, "/service/" + DELETE_SERVICE_NAME)
                .rxSend();

        // Assert
        responseFuture
                .doOnSuccess(response -> testContext.verify(() ->
                        assertEquals(204, response.statusCode()))
                )
                // Verify in the DB
                .flatMap(x -> this.connector.query("select * from service where name = '" + DELETE_SERVICE_NAME + "'").toSingle())
                .doOnSuccess(rows -> testContext.verify(() -> {
                    List<JsonObject> results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .collect(Collectors.toList());

                    assertEquals(0, (long) results.size());

                    testContext.completeNow();
                })).subscribe();
    }

    @Test
    @DisplayName("DELETE /service/:name and get 404 if the service name does not exist")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_delete_should_return_404_when_name_is_not_found(Vertx vertx, VertxTestContext testContext) {
        // Act
        Single<HttpResponse<Buffer>> httpResponse = WebClient.create(vertx)
                .delete(APP_PORT, BASE_HOST, "/service/" + DIFFERENT_NAME)
                .rxSend();

        // Assert
        httpResponse.doOnSuccess(response -> {
            // Assert
            testContext.verify(() -> {
                // Asserts http response is BAD REQUEST
                assertEquals(404, response.statusCode());
                assertEquals("Service with this name does not exist", response.body().toString());
                testContext.completeNow();
            });
        }).subscribe();
    }
}
