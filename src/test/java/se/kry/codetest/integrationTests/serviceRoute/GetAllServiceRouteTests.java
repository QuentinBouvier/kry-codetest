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
import se.kry.codetest.DBConnector;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetAllServiceRouteTests extends BaseMainVerticleTest {

    @Override
    protected void prepareDb(Vertx vertx, VertxTestContext testContext, DBConnector connector) {
        long date = new Date().getTime();
        connector.query("insert into service (url, name, created_at) values ('http://example.com', 'example', " + date + "), ('https://foo.com', 'bar', " + date + ");")
                .setHandler(result2 -> {
                    if (result2.succeeded()) {
                        testContext.completeNow();
                    } else {
                        testContext.failNow(result2.cause());
                    }
                    connector.getClient().close();
                });
    }

    @Test
    @DisplayName("GET /service and get 200 with list of 2 recorded services")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_get_should_send_a_200_status_with_a_list(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);
        client
                .get(APP_PORT, "localhost", "/service")
                .send(response -> {
                    HttpResponse<Buffer> result = response.result();

                    // Assert
                    testContext.verify(() -> {
                        JsonArray body = result.bodyAsJsonArray(); // Must not throw
                        assertEquals(200, response.result().statusCode());
                        assertEquals(2, body.stream().count());
                        testContext.completeNow();
                    });
                });
    }
}
