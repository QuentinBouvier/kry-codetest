package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.DBConnector;
import se.kry.codetest.model.PollService;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceRepository {
    final private DBConnector dbConnector;

    public ServiceRepository(DBConnector connector) {
        this.dbConnector = connector;
    }

    /**
     * Get the first result where name = {name}
     *
     * @param name The name to search against
     * @return A future holding a PollService instance or null if no service is found
     */
    public Future<PollService> findByName(String name) {
        Future<ResultSet> query = dbConnector.query("SELECT * FROM service WHERE name = ? LIMIT 1", new JsonArray(Collections.singletonList(name)));

        Future<PollService> futureResult = Future.future();

        query.setHandler(queryResult -> {
            if (queryResult.succeeded()) {
                if (queryResult.result().getNumRows() > 0) {
                    futureResult.complete(PollService.fromJson(queryResult.result().getRows().get(0)));
                } else {
                    futureResult.complete(null);
                }
            } else {
                futureResult.fail(queryResult.cause());
            }
        });

        return futureResult;
    }

    /**
     * Get all the services in the base
     *
     * @return A future holding the list of found PollService
     */
    public Future<List<PollService>> findAll() {
        Future<ResultSet> query = dbConnector.query("SELECT * FROM service");

        Future<List<PollService>> futureResult = Future.future();

        query.setHandler(queryResult -> {
            if (queryResult.succeeded()) {
                futureResult.complete(
                        queryResult.result().getRows().stream()
                                .map(PollService::fromJson).collect(Collectors.toList()));
            } else {
                futureResult.fail(queryResult.cause());
            }
        });

        return futureResult;
    }

    /**
     * Insert a new PollService in the base
     *
     * @param service The service to insert
     * @return A future holding the success of the operation
     * @throws InvalidParameterException A service with this name already exists
     */
    public Future<Void> createOne(PollService service) {
        Future<PollService> findQuery = findByName(service.getName());

        Future<Void> futureResult = Future.future();

        findQuery.setHandler(findResult -> {
            if (findQuery.isComplete()) {
                if (null != findQuery.result()) {
                    futureResult.fail(new InvalidParameterException("Service with this name already exist"));
                } else {
                    Future<ResultSet> insertQuery = dbConnector.query("INSERT INTO service (url, name, created_at) values(?, ?, ?)",
                            new JsonArray(Arrays.asList(service.getUrl(), service.getName(), new Date().getTime())));

                    insertQuery.setHandler(insertResult -> {
                        if (insertQuery.succeeded()) {
                            futureResult.complete();
                        } else {
                            futureResult.fail(insertResult.cause());
                        }
                    });
                }
            } else {
                futureResult.fail(findResult.cause());
            }
        });

        return futureResult;
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
        Future<PollService> findQuery = findByName(name);

        Future<Void> futureResult = Future.future();

        findQuery.setHandler(findResult -> {
            if (findQuery.isComplete()) {
                if (null == findQuery.result()) {
                    futureResult.fail(new InvalidParameterException("Service with this name does not exist"));
                } else {
                    Future<ResultSet> updateQuery = dbConnector.query(
                            "UPDATE service SET status = ? WHERE name = ?",
                            new JsonArray(Arrays.asList(status, name))
                    );

                    updateQuery.setHandler(insertResult -> {
                        if (updateQuery.succeeded()) {
                            futureResult.complete();
                        } else {
                            futureResult.fail(insertResult.cause());
                        }
                    });
                }
            } else {
                futureResult.fail(findResult.cause());
            }
        });

        return futureResult;
    }

    /**
     * Delete a status from its name
     *
     * @param name the name to search against
     * @return A future holding the success of the operation
     */
    public Future<Void> deleteByName(String name) {
        Future<ResultSet> query = dbConnector.query(
                "DELETE FROM service WHERE name = ?",
                new JsonArray(Collections.singletonList(name))
        );

        Future<Void> futureResult = Future.future();

        query.setHandler(deleteResult -> {
            if (deleteResult.succeeded()) futureResult.complete();
            else futureResult.fail(deleteResult.cause());
        });

        return futureResult;
    }
}
