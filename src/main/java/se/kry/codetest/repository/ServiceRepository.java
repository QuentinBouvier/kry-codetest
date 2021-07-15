package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    public PollService findByName(String name) throws InterruptedException {
        ResultSet result = dbConnector.queryExecutor("SELECT * FROM service WHERE name = ? LIMIT 1", new JsonArray(Collections.singletonList(name)));

        if (result.getNumRows() == 0) return null;

        return PollService.fromJson(result.getRows().get(0));
    }

    public List<PollService> findAll() throws InterruptedException {
        ResultSet query = dbConnector.queryExecutor("SELECT * FROM service");

        return query.getRows().stream().map(PollService::fromJson).collect(Collectors.toList());
    }

    public void createOne(PollService service) throws InterruptedException, InvalidParameterException {
        if (null != findByName(service.getName())) throw new InvalidParameterException("Service with this name already exist");

        ResultSet query = dbConnector.queryExecutor("INSERT INTO service values(?, ?, ?)",
                new JsonArray(Arrays.asList(service.getUrl(), service.getName(), service.getCreatedAt().getTime())));
    }

    public void deleteByName(String name) throws InterruptedException {
        dbConnector.queryExecutor("DELETE FROM service WHERE name = ?", new JsonArray(Collections.singletonList(name)));
    }
}
