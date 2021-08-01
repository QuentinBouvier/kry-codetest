package se.kry.codetest.integrationTests.serviceRoute;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.kry.codetest.integrationTests.BaseMainVerticleTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetAllServiceRoute extends BaseMainVerticleTest {
    @Test
    @DisplayName("Start a web server on localhost (8083), query GET /service and get 200 with list")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void route_service_as_get_should_send_a_200_status_with_a_list(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(APP_PORT, "localhost", "/service")
                .send(response -> {
                    HttpResponse<Buffer> result = response.result();

                    // Assert
                    assertDoesNotThrow(result::bodyAsJsonArray);
                    assertEquals(200, response.result().statusCode());
                    testContext.completeNow();
                });

    }
}
