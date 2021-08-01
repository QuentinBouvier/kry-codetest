package se.kry.codetest.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ServiceStatusController {

    ServiceStatusRepository serviceRepository;

    public ServiceStatusController(ServiceStatusRepository serviceStatusRepository) {
        this.serviceRepository = serviceStatusRepository;
    }

    public void servicePost(RoutingContext req) {
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

    public void serviceGet(RoutingContext req) {
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

    public void serviceDelete(RoutingContext req) {
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