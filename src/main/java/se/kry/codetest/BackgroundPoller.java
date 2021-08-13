package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BackgroundPoller {
    private final ServiceStatusRepository servicesRepository;
    private final WebClient webClient;

    public BackgroundPoller(ServiceStatusRepository repository, WebClient webClient) {
        this.servicesRepository = repository;
        this.webClient = webClient;
    }

    public void pollServices() {
        log.info("Polling services...");
        Future<List<ServiceStatus>> servicesQuery = servicesRepository.findAll();

        servicesQuery
                .onSuccess(serviceStatusList -> {
                    log.info("Found services: {}", serviceStatusList.stream().map(ServiceStatus::getName).collect(Collectors.joining(",")));

                    serviceStatusList.forEach(this::pollSingleService);
                });
    }

    private void pollSingleService(ServiceStatus service) {
        log.info("Polling service {}", service.getName());

        webClient
                .getAbs(service.getUrl())
                .send(res -> {
                    if (res.succeeded()) {
                        log.debug("Service {} ({}) has responded", service.getName(), service.getUrl());
                        servicesRepository.setStatus(service.getName(), "OK");
                    } else {
                        log.info("Service {} has failed", service.getUrl());
                        servicesRepository.setStatus(service.getName(), "FAIL");
                    }
                });
    }
}