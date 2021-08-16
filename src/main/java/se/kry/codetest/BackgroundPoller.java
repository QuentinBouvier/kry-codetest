package se.kry.codetest;

import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
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
        log.debug("Instantiating {}...", this.getClass().getName());
        this.servicesRepository = repository;
        this.webClient = webClient;
    }

    public void pollServices() {
        servicesRepository.findAll()
                .subscribe(new SingleObserver<>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        log.info("Services polling requested...");
                    }

                    @Override
                    public void onSuccess(@NonNull List<ServiceStatus> serviceStatuses) {
                        log.info("Found services: {}", serviceStatuses.stream().map(ServiceStatus::getName).collect(Collectors.joining(",")));

                        serviceStatuses.forEach(serviceStatus -> pollSingleService(serviceStatus));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        log.debug("Error in polling service: {}", e.getMessage());
                        e.printStackTrace();
                    }
                });
    }

    private void pollSingleService(ServiceStatus service) {
        webClient.getAbs(service.getUrl())
                .rxSend()
                .subscribe(new SingleObserver<>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        log.info("Polling service {}", service.getName());
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<Buffer> bufferHttpResponse) {
                        log.debug("Service {} ({}) has responded", service.getName(), service.getUrl());
                        servicesRepository.setStatus(service.getName(), "OK")
                                .subscribe();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        log.info("Service {} has failed to respond", service.getUrl());
                        servicesRepository.setStatus(service.getName(), "FAIL")
                                .subscribe();
                    }
                });
    }
}
