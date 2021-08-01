package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceStatusRepository;

import java.util.List;
import java.util.stream.Collectors;

public class BackgroundPoller {
  private final ServiceStatusRepository servicesRepository;
  private final WebClient webClient;

  public BackgroundPoller(ServiceStatusRepository repository, WebClient webClient) {
    this.servicesRepository = repository;
    this.webClient = webClient;
  }

  public void pollServices() {
    System.out.println("Polling services");
    Future<List<ServiceStatus>> servicesQuery = servicesRepository.findAll();

    servicesQuery.setHandler(servicesResult -> {
      if (servicesResult.succeeded()) {
        List<ServiceStatus> services = servicesResult.result();
        System.out.println("Found services: " + services.stream().map(ServiceStatus::getName).collect(Collectors.joining(",")));

        services.forEach(this::pollSingleService);
      }
    });
  }

  private void pollSingleService(ServiceStatus service) {
    System.out.println("Polling service " + service.getName());

    String url = StringUtils.stripEnd(service.getUrl().replaceAll("^https?:\\/\\/", ""), "/");

    webClient
            .get(443, url, "/")
            .ssl(true)
            .send(res -> {
      if (res.succeeded()) {
        System.out.printf("Service %s (%s) has responded%n", service.getName(), service.getUrl());
        servicesRepository.setStatus(service.getName(), "OK");
      } else {
        System.out.printf("Service %s has failed%n", service.getUrl());
        servicesRepository.setStatus(service.getName(), "FAIL");
      }
    });
  }
}
