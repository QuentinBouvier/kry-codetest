package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import se.kry.codetest.model.PollService;
import se.kry.codetest.repository.ServiceRepository;

import java.util.List;
import java.util.stream.Collectors;

public class BackgroundPoller {
  private ServiceRepository servicesRepository;
  private WebClient webClient;

  public BackgroundPoller(ServiceRepository repository, WebClient webClient) {
    this.servicesRepository = repository;
    this.webClient = webClient;
  }

  public void pollServices() {
    System.out.println("Polling services");
    Future<List<PollService>> servicesQuery = servicesRepository.findAll();

    servicesQuery.setHandler(servicesResult -> {
      if (servicesResult.succeeded()) {
        List<PollService> services = servicesResult.result();
        System.out.println("Found services: " + services.stream().map(PollService::getName).collect(Collectors.joining(",")));

        services.forEach(this::pollSingleService);
      }
    });
  }

  private void pollSingleService(PollService service) {
    System.out.println("Polling service " + service.getName());

    String url = StringUtils.stripEnd(service.getUrl().replaceAll("^https?:\\/\\/", ""), "/");

    webClient
            .get(443, url, "/")
            .ssl(true)
            .send(res -> {
      if (res.succeeded()) {
        System.out.printf("Service %s has responded%n", service.getUrl());
        servicesRepository.setStatus(service.getName(), "OK");
      } else {
        System.out.printf("Service %s has failed%n", service.getUrl());
        servicesRepository.setStatus(service.getName(), "FAIL");
      }
    });
  }
}
