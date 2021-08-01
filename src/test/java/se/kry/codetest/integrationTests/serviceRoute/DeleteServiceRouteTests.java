package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteServiceRouteTests extends BaseMainVerticleTest {
    private static final String DELETE_SERVICE_NAME = "foo";
    private static final String DIFFERENT_NAME = "bar";

    @Override
    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        long date = new Date().getTime();
        this.connector.query("insert into service (url, name, created_at) " +
                        "values ('https://example.com', '" + DELETE_SERVICE_NAME + "', " + date + ");")
                .setHandler(testContext.succeeding(result -> testContext.completeNow()));
    }

    @Test
    @DisplayName("DELETE /service/:name and gets 204 if the service was found. The service is deleted from the base")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_delete_should_remove_entry_when_name_is_found_and_return_204(Vertx vertx, VertxTestContext testContext) {
        // Act
        WebClient.create(vertx)
                .delete(APP_PORT, "localhost", "/service/" + DELETE_SERVICE_NAME)
                .send(testContext.succeeding(response -> {
                    // Assert
                    testContext.verify(() -> {
                        // Asserts http response is NO CONTENT
                        assertEquals(204, response.statusCode());

                        // Asserts the entry was removed from base
                        this.connector.query("select * from service where name = '" + DELETE_SERVICE_NAME + "'")
                                .setHandler(queryResult -> {
                                    List<JsonObject> resultRows = queryResult.result().getRows();
                                    assertEquals(0, (long) resultRows.size());

                                    testContext.completeNow();
                                });
                    });
                }));
    }

    @Test
    @DisplayName("DELETE /service/:name and get 400 if the service name does not exist")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_delete_should_return_400_when_name_is_not_found(Vertx vertx, VertxTestContext testContext) {
        // Act
        WebClient.create(vertx)
                .delete(APP_PORT, "localhost", "/service/" + DIFFERENT_NAME)
                .send(testContext.succeeding(response -> {
                    // Assert
                    testContext.verify(() -> {
                        // Asserts http response is BAD REQUEST
                        assertEquals(400, response.statusCode());
                        assertEquals("No service was found with this name", response.toString());
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("DELETE /service/:name and get 404 if the service name was not provided")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_delete_should_return_404_when_name_is_not_provided(Vertx vertx, VertxTestContext testContext) {
        // Act
        WebClient.create(vertx)
                .delete(APP_PORT, "localhost", "/service/")
                .send(testContext.succeeding(response -> {
                    // Assert
                    testContext.verify(() -> {
                        // Asserts http response is NOT FOUND
                        assertEquals(404, response.statusCode());
                        testContext.completeNow();
                    });
                }));
    }
}
