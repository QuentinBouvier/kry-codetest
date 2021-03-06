package se.kry.codetest.integrationTests;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.DBConnector;
import se.kry.codetest.MainVerticle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@ExtendWith(VertxExtension.class)
public abstract class BaseMainVerticleIntegrationTest {

    static final protected String DB_NAME = "pollerTest.db";
    static final protected int APP_PORT = 8084;
    static final protected String BASE_HOST = "localhost";

    protected final static String BASE_URI = "/api/v1/";

    protected DBConnector connector = null;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.rxDeployVerticle(new MainVerticle(APP_PORT, DB_NAME))
                .flatMapMaybe(id -> {
                    this.connector = new DBConnector(vertx, DB_NAME);
                    return this.connector.query("delete from service;");
                })
                .doOnError(testContext::failNow)
                .doOnSuccess(result ->
                        prepareDb(vertx, testContext)
                )
                .subscribe();
    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        vertx.rxClose()
                .doOnComplete(() -> {
                    File dbFile = new File(DB_NAME);
                    try {
                        Files.deleteIfExists(dbFile.toPath());
                    } catch (IOException ex) {
                        log.error("Something went wrong when deleting the test db file. You might want to delete {} manually", DB_NAME);
                        ex.printStackTrace();
                    }
                    testContext.completeNow();
                }).subscribe();
    }

    protected void prepareDb(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }
}
