package se.kry.codetest.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ServiceStatusController {

    ServiceStatusRepository serviceRepository;

    public ServiceStatusController(ServiceStatusRepository serviceStatusRepository) {
        this.serviceRepository = serviceStatusRepository;
    }

    public void servicePost(RoutingContext req) {
        JsonObject jsonBody = req.getBodyAsJson();

        log.info("POST /service HTTP received");
        log.debug("\twith body: {}", jsonBody.toString());

        ServiceStatus newService = ServiceStatus.fromJson(jsonBody);
        if (newService.isComplete()) {
            if (newService.isUrlValid()) {
                serviceRepository.createOne(newService)
                        .setHandler(repoResponse -> {
                            if (repoResponse.failed()) {
                                if (repoResponse.cause() instanceof InvalidParameterException) {
                                    req.response().setStatusCode(400).end(repoResponse.cause().getMessage());
                                } else {
                                    log.error("Error: {}", repoResponse.cause().getMessage());
                                    repoResponse.cause().printStackTrace();
                                    req.response().setStatusCode(500).end(repoResponse.cause().getMessage());
                                }
                            } else req.response().setStatusCode(201).end();
                        });
            } else {
                req.response().setStatusCode(400).end("The provided url is invalid");
            }
        } else {
            req.response().setStatusCode(400).end("url and name are mandatory");
        }
    }

    public void serviceGet(RoutingContext req) {
        log.info("GET /service HTTP received");
        serviceRepository.findAll().setHandler(repoResult -> {
            if (repoResult.failed()) {
                log.error("Error: {}", repoResult.cause().getMessage());
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

    public void serviceDelete(RoutingContext req) {
        String serviceName = req.pathParam("name");
        log.info("DELETE /service HTTP received");
        log.debug("\tfor name {}", serviceName);

        if (null == serviceName) {
            req.response().setStatusCode(400).end("name path param is mandatory");
        } else {
            serviceRepository.deleteByName(serviceName).setHandler(repoResult -> {
                if (repoResult.succeeded()) {
                    req.response().setStatusCode(204).end();
                } else {
                    if (repoResult.cause() instanceof InvalidParameterException) {
                        req.response().setStatusCode(400).end(repoResult.cause().getMessage());
                    } else {
                        log.error("Error: {}", repoResult.cause().getMessage());
                        repoResult.cause().printStackTrace();
                        req.response().setStatusCode(500).end(repoResult.cause().getMessage());
                    }
                }
            });
        }
    }
}