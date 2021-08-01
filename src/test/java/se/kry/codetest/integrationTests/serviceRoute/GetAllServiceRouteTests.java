package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetAllServiceRouteTests extends BaseMainVerticleTest {

    @Override
    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        long date = new Date().getTime();
        this.connector.query("insert into service (url, name, created_at) " +
                        "values ('https://example.com', 'example', " + date + "), " +
                        "('https://foo.com', 'bar', " + date + ");")
                .setHandler(testContext.succeeding(x -> testContext.completeNow()));
    }

    @Test
    @DisplayName("GET /service and get 200 with list of 2 recorded services")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_get_should_send_a_200_status_with_a_list(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client
                .get(APP_PORT, "localhost", "/service")
                .send(testContext.succeeding(response -> {
                    // Assert
                    testContext.verify(() -> {
                        JsonArray body = response.bodyAsJsonArray(); // Must not throw
                        assertEquals(200, response.statusCode());
                        assertEquals(2, body.stream().count());
                        testContext.completeNow();
                    });
                }));
    }
}
