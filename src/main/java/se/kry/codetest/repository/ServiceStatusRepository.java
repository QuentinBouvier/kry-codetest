package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.joda.time.DateTime;
import se.kry.codetest.DBConnector;
import se.kry.codetest.model.ServiceStatus;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ServiceStatusRepository {
    final private DBConnector dbConnector;

    public ServiceStatusRepository(DBConnector connector) {
        this.dbConnector = connector;
    }

    /**
     * Get the first result where name = {name}
     *
     * @param name The name to search against
     * @return A future holding a PollService instance or null if no service is found
     */
    public Future<ServiceStatus> findByName(String name) {
        Future<RowSet<Row>> query = dbConnector.query("SELECT * FROM service WHERE name = ? LIMIT 1", new JsonArray(Collections.singletonList(name)));

        return query.map(rows -> {
            List<JsonObject> results = StreamSupport
                    .stream(rows.spliterator(), false)
                    .map(Row::toJson)
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                return null;
            }

            return ServiceStatus.fromJson(results.get(0));
        });
    }

    /**
     * Get all the services in the base
     *
     * @return A future holding the list of found PollService
     */
    public Future<List<ServiceStatus>> findAll() {
        Future<RowSet<Row>> query = dbConnector.query("SELECT * FROM service");

        return query.map(rows ->
                StreamSupport
                        .stream(rows.spliterator(), false)
                        .map(x -> ServiceStatus.fromJson(x.toJson()))
                        .collect(Collectors.toList()));
    }

    /**
     * Insert a new ServiceStatus in the base
     *
     * @param service The service to insert
     * @return A future holding the success of the operation
     * @throws InvalidParameterException A service with this name already exists
     */
    public Future<Void> createOne(ServiceStatus service) {
        Future<ServiceStatus> findQuery = findByName(service.getName());

        return findQuery
                .compose(serviceStatus -> {
                    if (null != serviceStatus) {
                        return Future.failedFuture(new InvalidParameterException("Service with this name already exist"));
                    }

                    return dbConnector.query("INSERT INTO service (url, name, created_at) values(?, ?, ?)",
                            new JsonArray(Arrays.asList(service.getUrl(), service.getName(), new DateTime().getMillis())));
                })
                .mapEmpty();
    }

    /**
     * Set the status value in the line where the name = {name}
     *
     * @param name   The name to search against in the base
     * @param status The status to set
     * @return A future holding the success of the operation
     * @throws InvalidParameterException No service with this name was found
     */
    public Future<Void> setStatus(String name, String status) {
        Future<ServiceStatus> findQuery = findByName(name);

        return findQuery
                .compose(serviceStatus -> {
                    if (null == serviceStatus) {
                        return Future.failedFuture(new InvalidParameterException("Service with this name does not exist"));
                    }
                    return dbConnector.query(
                            "UPDATE service SET status = ? WHERE name = ?",
                            new JsonArray(Arrays.asList(status, name))
                    );
                })
                .mapEmpty();
    }

    /**
     * Delete a status from its name
     *
     * @param name the name to search against
     * @return A future holding the success of the operation
     */
    public Future<Void> deleteByName(String name) {
        return findByName(name)
                .compose(serviceStatus -> {
                    if (null == serviceStatus) {
                        return Future.failedFuture(new InvalidParameterException("Service with this name does not exist"));
                    }
                    return dbConnector.query("DELETE FROM service WHERE name = ?",
                            new JsonArray(Collections.singletonList(name)));
                })
                .mapEmpty();
    }
}