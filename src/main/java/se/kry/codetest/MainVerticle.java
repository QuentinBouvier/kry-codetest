package se.kry.codetest;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.controller.ServiceStatusController;
import se.kry.codetest.repository.ServiceStatusRepository;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    private ServiceStatusRepository serviceRepository;
    private ServiceStatusController serviceStatusController;
    private BackgroundPoller poller;
    private Integer port;
    private final String dbPath;

    public MainVerticle() {
        this(8080, "poller.db");
    }

    public MainVerticle(int port) {
        this(port, "poller.db");
    }

    public MainVerticle(int port, String dbPath) {
        this.port = port;
        this.dbPath = dbPath;
    }

    @Override
    public void start(Future<Void> startFuture) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(
                        new ConfigStoreOptions().setType("env")
                                .setConfig(new JsonObject().put("keys", new JsonArray().add("PORT")))
                ));

        retriever.getConfig(confResult -> {
            if (confResult.failed()) {
                log.error("Unable to retrieve the config");
                startFuture.fail(confResult.cause());
                vertx.close();
            } else {
                JsonObject config = confResult.result();
                if (null != config.getInteger("PORT")) {
                    this.port = config.getInteger("PORT");
                }

                DBConnector connector = new DBConnector(vertx, this.dbPath);
                connector.start().setHandler(dbStart -> {
                    if (dbStart.failed()) {
                        log.error("Unable to connect to DB");
                        startFuture.fail(dbStart.cause());
                        vertx.close();
                    } else {
                        log.info("Connection to DB successful");
                        WebClient webClient = WebClient.create(vertx);

                        serviceRepository = new ServiceStatusRepository(connector);
                        serviceStatusController = new ServiceStatusController(serviceRepository);
                        poller = new BackgroundPoller(serviceRepository, webClient);

                        Router router = Router.router(vertx);
                        router.route().handler(BodyHandler.create());
                        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
                        setRoutes(router);

                        vertx.createHttpServer()
                                .requestHandler(router)
                                .listen(this.port, result -> {
                                    if (result.succeeded()) {
                                        log.info("KRY code test service started on port {}", this.port);
                                        startFuture.complete();
                                    } else {
                                        log.error("Unable to start the service poller");
                                        startFuture.fail(result.cause());
                                        vertx.close();
                                    }
                                });
                    }
                });

            }
        });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(this.serviceStatusController::serviceGet);
        router.post("/service").handler(this.serviceStatusController::servicePost);
        router.delete("/service/:name").handler(this.serviceStatusController::serviceDelete);
    }
}



