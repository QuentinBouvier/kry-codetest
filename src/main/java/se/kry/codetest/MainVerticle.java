package se.kry.codetest;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
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
    public Completable rxStart() {
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

        return retriever.rxGetConfig()
                .flatMap(config -> {
                    Integer configPort = config.getInteger("PORT");
                    if (null != configPort) {
                        log.info("Custom port requested by user: {}", configPort);
                        this.port = configPort;
                    }
                    return Single.just(config);
                })
                .doOnError(error -> handleInitError("Unable to retrieve the config"))
                .flatMap(config -> connector.start().toSingleDefault(true))
                .doOnError(error -> handleInitError("Unable to initialize DB"))
                .flatMap(upstream -> {
                    log.info("Connection to DB successful");
                    log.debug("Starting services");

                    serviceRepository = new ServiceStatusRepository(connector);
                    serviceStatusController = new ServiceStatusController(serviceRepository);
                    poller = new BackgroundPoller(serviceRepository, webClient);

                    router.route().handler(BodyHandler.create());
                    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
                    setRoutes(router);
                    log.debug("Services started");

                    return Single.just(true);
                })
                .flatMapCompletable(upstream -> {
                    log.debug("Starting server...");
                    return vertx.createHttpServer()
                            .requestHandler(router)
                            .rxListen(this.port)
                            .ignoreElement();
                })
                .doOnComplete(() -> log.info("KRY code test service started on port {}", this.port))
                .doOnError(error -> handleInitError("Unable to start the service poller"));
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

    private void handleInitError(String message) {
        log.error(message);
        vertx.close();
    }
}
