package se.kry.codetest.integrationTests;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.MainVerticle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@ExtendWith(VertxExtension.class)
public abstract class BaseMainVerticleTest {

    static final protected String DB_NAME = "pollerTest.db";
    static final protected int APP_PORT = 8083;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(APP_PORT, DB_NAME), testContext.succeeding(id -> testContext.completeNow()));
    }

    @AfterEach
    void destroy_database(Vertx vertx, VertxTestContext testContext) {
        vertx.close(shutdown -> {
            File dbFile = new File(DB_NAME);
            try {
                Files.deleteIfExists(dbFile.toPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            testContext.completeNow();
        });
    }
}
