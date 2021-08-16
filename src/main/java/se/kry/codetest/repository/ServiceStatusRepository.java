package se.kry.codetest.repository;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.sqlclient.Row;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import se.kry.codetest.DBConnector;
import se.kry.codetest.model.ServiceStatus;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class ServiceStatusRepository {
    final private DBConnector dbConnector;

    public ServiceStatusRepository(DBConnector connector) {
        log.debug("Instantiating ServiceStatusRepository...");
        this.dbConnector = connector;
    }

    /**
     * Get the first result where name = {name}
     *
     * @param name The name to search against
     * @return A future holding a PollService instance or null if no service is found
     */
    public Maybe<ServiceStatus> findByName(String name) {
        return dbConnector.query("SELECT * FROM service WHERE name = ? LIMIT 1", new JsonArray(Collections.singletonList(name)))
                .flatMap(rows -> {
                    List<JsonObject> results = StreamSupport
                            .stream(rows.spliterator(), false)
                            .map(Row::toJson)
                            .collect(Collectors.toList());

                    if (results.isEmpty()) {
                        return Maybe.empty();
                    }

                    return Maybe.just(ServiceStatus.fromJson(results.get(0)));
                });
    }

    /**
     * Get all the services in the base
     *
     * @return A future holding the list of found PollService
     */
    public Single<List<ServiceStatus>> findAll() {
        return dbConnector.query("SELECT * FROM service")
                .flatMap(rows -> {
                            List<ServiceStatus> rowsJson = StreamSupport
                                    .stream(rows.spliterator(), false)
                                    .map(x -> ServiceStatus.fromJson(x.toJson()))
                                    .collect(Collectors.toList());

                            return Maybe.just(rowsJson);
                        })
                .toSingle();
    }

    /**
     * Insert a new ServiceStatus in the base
     *
     * @param service The service to insert
     * @return A future holding the success of the operation
     *
     * @throws InvalidParameterException A service with this name already exists
     */
    public Completable createOne(ServiceStatus service) {
        return findByName(service.getName())
                .isEmpty()
                .flatMap(serviceEmpty -> {
                    if (!serviceEmpty) {
                        return Single.error(new InvalidParameterException("Service with this name already exist"));
                    } else {
                        return dbConnector.query("INSERT INTO service (url, name, created_at) values(?, ?, ?)",
                                        new JsonArray(Arrays.asList(service.getUrl(), service.getName(), new DateTime().getMillis())))
                                .toSingle();
                    }
                })
                .ignoreElement();
    }

    /**
     * Set the status value in the line where the name = {name}
     *
     * @param name   The name to search against in the base
     * @param status The status to set
     * @return A future holding the success of the operation
     *
     * @throws InvalidParameterException No service with this name was found
     */
    public Completable setStatus(String name, String status) {
        return findByName(name)
                .isEmpty()
                .flatMap(serviceEmpty -> {
                    if (serviceEmpty) {
                        return Single.error(new InvalidParameterException("Service with this name does not exist"));
                    }
                    return dbConnector.query(
                            "UPDATE service SET status = ? WHERE name = ?",
                            new JsonArray(Arrays.asList(status, name))
                    ).toSingle();
                })
                .ignoreElement();
    }

    /**
     * Delete a status from its name
     *
     * @param name the name to search against
     * @return A future holding the success of the operation
     */
    public Completable deleteByName(String name) {
        return findByName(name)
                .isEmpty()
                .flatMap(serviceEmpty -> {
                    if (serviceEmpty) {
                        return Single.error(new InvalidParameterException("Service with this name does not exist"));
                    }
                    return dbConnector.query("DELETE FROM service WHERE name = ?",
                            new JsonArray(Collections.singletonList(name))).toSingle();
                })
                .ignoreElement();
    }
}
