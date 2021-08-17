package se.kry.codetest.controller;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import se.kry.codetest.exception.BadRequestException;
import se.kry.codetest.exception.ControllerException;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ServiceStatusController {

    ServiceStatusRepository serviceRepository;

    public ServiceStatusController(ServiceStatusRepository serviceStatusRepository) {
        log.debug("Instantiating {}...", this.getClass().getName());
        this.serviceRepository = serviceStatusRepository;
    }

    public void servicePost(RoutingContext req) {
        try {
            JsonObject jsonBody = req.getBodyAsJson();

            log.info("HTTP POST received on /service");
            log.debug("\twith body: {}", jsonBody.toString());

            ServiceStatus newService = ServiceStatus.fromJson(jsonBody);

            Single.fromCallable(() -> !newService.isValid())
                    .flatMapCompletable(serviceIsInvalid -> {
                        if (serviceIsInvalid)
                            return Completable.error(new BadRequestException("url and name are mandatory"));
                        if (!newService.isUrlValid())
                            return Completable.error(new BadRequestException("The provided url is invalid"));

                        return serviceRepository.createOne(newService);
                    })
                    .onErrorResumeNext(cause -> {
                        if (cause instanceof ControllerException) {
                            req.response().setStatusCode(((ControllerException) cause).getCode()).end(cause.getMessage());
                            return Completable.never();
                        }
                        if (cause instanceof InvalidParameterException) {
                            req.response().setStatusCode(400).end(cause.getMessage());
                            return Completable.never();
                        }
                        return Completable.error(cause);
                    })
                    .doOnError(cause -> {
                        log.error("Error: {}", cause.getMessage());
                        cause.printStackTrace();
                        req.response().setStatusCode(500).end(cause.getMessage());
                    })
                    .doOnComplete(() -> req.response().setStatusCode(201).end()).subscribe();
        } catch (DecodeException ex) {
            req.response().setStatusCode(400).end("Invalid payload format. Must be json");
        }
    }

    public void serviceGet(RoutingContext req) {
        log.info("HTTP GET received on /service");
        serviceRepository.findAll()
                .doOnSuccess(serviceStatusList -> {
                    List<JsonObject> jsonServices = serviceStatusList
                            .stream()
                            .map(ServiceStatus::toJson)
                            .collect(Collectors.toList());

                    req.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(new JsonArray(jsonServices).encode());
                })
                .doOnError(cause -> {
                    log.error("Error: {}", cause.getMessage());
                    cause.printStackTrace();
                    req.response().setStatusCode(500).end(cause.getMessage());
                })
                .subscribe();
    }

    public void serviceDelete(RoutingContext req) {
        String serviceName = req.pathParam("name");
        log.info("HTTP DELETE received on /service/{}", serviceName);

        Single.fromCallable(() -> StringUtils.isNotBlank(serviceName))
                .flatMapCompletable(hasServiceName -> {
                    if (!hasServiceName)
                        return Completable.error(new BadRequestException("name path param is mandatory"));

                    return serviceRepository.deleteByName(serviceName);
                })
                .onErrorResumeNext(cause -> {
                    if (cause instanceof ControllerException) {
                        req.response().setStatusCode(((ControllerException) cause).getCode()).end(cause.getMessage());
                        return Completable.never();
                    }
                    if (cause instanceof InvalidParameterException) {
                        req.response().setStatusCode(404).end(cause.getMessage());
                        return Completable.never();
                    }
                    return Completable.error(cause);
                })
                .doOnError(cause -> {
                    log.error("Error: {}", cause.getMessage());
                    cause.printStackTrace();
                    req.response().setStatusCode(500).end(cause.getMessage());
                })
                .doOnComplete(() -> req.response().setStatusCode(204).end()).subscribe();
    }

    public void serviceUpdate(RoutingContext req) {
        try {
            String serviceName = req.pathParam("name");
            log.info("HTTP PUT received on /service/{}", serviceName);

            JsonObject jsonBody = req.getBodyAsJson();
            log.debug("\twith body: {}", jsonBody.toString());
            ServiceStatus newService = ServiceStatus.fromJson(jsonBody);

            Single.fromCallable(() -> StringUtils.isNotBlank(serviceName))
                    .flatMapCompletable(hasName -> { // validate input
                        if (!hasName)
                            return Completable.error(new BadRequestException("name path param is mandatory"));
                        if (!newService.isValid())
                            return Completable.error(new BadRequestException("url and name are mandatory"));
                        if (!newService.isUrlValid())
                            return Completable.error(new BadRequestException("The url provided is invalid"));

                        return this.serviceRepository.update(serviceName, newService);
                    })
                    .onErrorResumeNext(cause -> {
                        if (cause instanceof BadRequestException) {
                            req.response().setStatusCode(400).end(cause.getMessage());
                            return Completable.never();
                        } else {
                            return Completable.error(cause);
                        }
                    })
                    .doOnComplete(() ->
                            req.response().setStatusCode(200).end()
                    )
                    .doOnError(cause -> {
                        log.error("Error: {}", cause.getMessage());
                        cause.printStackTrace();
                        req.response().setStatusCode(500).end(cause.getMessage());
                    })
                    .subscribe();

        } catch (DecodeException ex) {
            req.response().setStatusCode(400).end("Invalid payload format. Must be json");
        }
    }
}
