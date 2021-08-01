package se.kry.codetest;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceRepository;

import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private ServiceRepository serviceRepository;
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
                startFuture.fail(confResult.cause());
            } else {
                JsonObject config = confResult.result();
                if (null != config.getInteger("PORT")) {
                    this.port = config.getInteger("PORT");
                }

                DBConnector connector = new DBConnector(vertx, this.dbPath);
                connector.start().setHandler(dbStart -> {
                    if (dbStart.failed()) {
                        startFuture.fail(dbStart.cause());
                    } else {

                        WebClient webClient = WebClient.create(vertx);

                        serviceRepository = new ServiceRepository(connector);
                        poller = new BackgroundPoller(serviceRepository, webClient);

                        Router router = Router.router(vertx);
                        router.route().handler(BodyHandler.create());
                        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
                        setRoutes(router);

                        vertx.createHttpServer()
                                .requestHandler(router)
                                .listen(this.port, result -> {
                                    if (result.succeeded()) {
                                        System.out.println("KRY code test service started");
                                        startFuture.complete();
                                    } else {
                                        startFuture.fail(result.cause());
                                    }
                                });
                    }
                });

            }
        });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(this::serviceGet);
        router.post("/service").handler(this::servicePost);
        router.delete("/service/:name").handler(this::serviceDelete);
    }

    private void servicePost(RoutingContext req) {
        JsonObject jsonBody = req.getBodyAsJson();

        System.out.println("POST /service HTTP received with body: " + jsonBody.toString());

        ServiceStatus newService = ServiceStatus.fromJson(jsonBody);
        if (newService.isComplete()) {
            if (newService.isUrlValid()) {
                serviceRepository.createOne(newService)
                        .setHandler(repoResponse -> {
                            if (repoResponse.failed()) {
                                System.out.println("Error: " + repoResponse.cause().getMessage());
                                repoResponse.cause().printStackTrace();
                                req.response().setStatusCode(500).end(repoResponse.cause().getMessage());
                            } else req.response().setStatusCode(201).end();
                        });
            } else {
                req.response().setStatusCode(400).end("The provided url is invalid");
            }
        } else {
            req.response().setStatusCode(400).end("url and name are mandatory");
        }
    }

    private void serviceGet(RoutingContext req) {
        serviceRepository.findAll().setHandler(repoResult -> {
            if (repoResult.failed()) {
                System.out.println("Error: " + repoResult.cause().getMessage());
                repoResult.cause().printStackTrace();
                req.response().setStatusCode(500).end(repoResult.cause().getMessage());
            } else {
                List<JsonObject> jsonServices = repoResult.result()
                        .stream()
                        .map(ServiceStatus::toJson)
                        .collect(Collectors.toList());

                req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(new JsonArray(jsonServices).encode());
            }
        });
    }

    private void serviceDelete(RoutingContext req) {
        String serviceName = req.pathParam("name");

        if (null == serviceName) {
            req.response().setStatusCode(400).end("name path param is mandatory");
        } else {
            serviceRepository.deleteByName(serviceName).setHandler(repoResult -> {
                if (repoResult.succeeded()) {
                    req.response().setStatusCode(204).end();
                } else {
                    System.out.println("Error: " + repoResult.cause().getMessage());
                    repoResult.cause().printStackTrace();
                    req.response().setStatusCode(500).end(repoResult.cause().getMessage());
                }
            });
        }
    }
}



