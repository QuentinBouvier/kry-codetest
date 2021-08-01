package se.kry.codetest.integrationTests;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.DBConnector;
import se.kry.codetest.MainVerticle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@ExtendWith(VertxExtension.class)
public abstract class BaseMainVerticleTest {

    static final protected String DB_NAME = "pollerTest.db";
    static final protected int APP_PORT = 8083;
    protected DBConnector connector = null;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(APP_PORT, DB_NAME), testContext.succeeding(id -> {
            this.connector = new DBConnector(vertx, DB_NAME);
            this.connector.query("delete from service;")
                    .setHandler(result -> {
                        if (result.succeeded()) {
                            prepareDb(vertx, testContext);
                        } else {
                            testContext.failNow(result.cause());
                        }
                    });
        }));

    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        this.connector.getClient().close(event -> {
            vertx.close(shutdown -> {
                File dbFile = new File(DB_NAME);
                try {
                    Files.deleteIfExists(dbFile.toPath());
                } catch (IOException ex) {
                    System.err.printf("Something went wrong when deleting the test db file. You might want to delete %s manually%n", DB_NAME);
                    ex.printStackTrace();
                }
                testContext.completeNow();
            });
        });
    }

    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }
}
