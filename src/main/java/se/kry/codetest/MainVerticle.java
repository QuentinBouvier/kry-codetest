package se.kry.codetest;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.controller.ServiceStatusController;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.util.Arrays;
import java.util.HashSet;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    private final static String DEFAULT_DB = "poller.db";
    private final static int DEFAULT_PORT = 8080;

    private ServiceStatusRepository serviceRepository;
    private ServiceStatusController serviceStatusController;
    private BackgroundPoller poller;

    private Integer port;
    private final String dbPath;

    public MainVerticle() {
        this(DEFAULT_PORT, DEFAULT_DB);
    }

    public MainVerticle(int port) {
        this(port, DEFAULT_DB);
    }

    public MainVerticle(String dbpath) {
        this(DEFAULT_PORT, dbpath);
    }

    public MainVerticle(int port, String dbPath) {
        this.port = port;
        this.dbPath = dbPath;
    }

    @Override
    public void start(Promise<Void> startFuture) {
        final Router router = Router.router(vertx);
        final DBConnector connector = new DBConnector(vertx, this.dbPath);
        final WebClient webClient = WebClient.create(
                vertx,
                new WebClientOptions()
                        .setFollowRedirects(true)
                        .setVerifyHost(false)
                        .setTrustAll(true)
        );

        JsonObject configKeys = new JsonObject().put("keys", new JsonArray().add("PORT"));

        ConfigRetriever retriever = ConfigRetriever
                .create(
                        vertx,
                        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions()
                                .setType("env")
                                .setConfig(configKeys))
                );

        retriever.getConfig()
                .onFailure(err -> {
                    log.error("Unable to retrieve the config");
                    startFuture.fail(err.getCause());
                    vertx.close();
                })
                .onSuccess(config -> {
                    if (null != config.getInteger("PORT")) {
                        this.port = config.getInteger("PORT");
                    }
                })
                // Start DB connector
                .compose(event ->
                        connector.start()
                )
                .onFailure(err -> {
                    log.error("Unable to connect to DB");
                    startFuture.fail(err.getCause());
                    vertx.close();
                })
                .onSuccess(event -> {
                    log.info("Connection to DB successful");

                    serviceRepository = new ServiceStatusRepository(connector);
                    serviceStatusController = new ServiceStatusController(serviceRepository);
                    poller = new BackgroundPoller(serviceRepository, webClient);

                    router.route().handler(BodyHandler.create());
                    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
                    setRoutes(router);
                })
                // Start the web server
                .compose(event ->
                        vertx.createHttpServer()
                                .requestHandler(router)
                                .listen(this.port)
                )
                .onSuccess(success -> {
                    log.info("KRY code test service started on port {}", this.port);
                    startFuture.complete();
                })
                .onFailure(cause -> {
                    log.error("Unable to start the service poller");
                    startFuture.fail(cause);
                    vertx.close();
                });
    }

    private void setRoutes(Router router) {
        // Resource distribution handler
        router.route("/*").handler(StaticHandler.create());

        // Cors
        router.routeWithRegex("\\/service(\\/.*)?").handler(
                CorsHandler.create("^(https?:\\/\\/)?localhost(:[0-9]{1,5})?")
                        .allowedMethods(new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.OPTIONS)))
        );

        // Routes
        router.get("/service").handler(this.serviceStatusController::serviceGet);
        router.post("/service").handler(this.serviceStatusController::servicePost);
        router.delete("/service/:name").handler(this.serviceStatusController::serviceDelete);
    }
}
