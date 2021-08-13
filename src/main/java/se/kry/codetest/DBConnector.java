package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.Getter;

public class DBConnector {

    private static final String DB_PATH = "poller.db";

    @Getter
    private final JDBCPool pool;

    public DBConnector(Vertx vertx, String path) {
        pool = JDBCPool.pool(vertx,
                new JsonObject()
                        .put("url", "jdbc:sqlite:" + path)
                        .put("driver_class", "org.sqlite.JDBC")
                        .put("max_pool_size", 30)
        );
    }

    public Future<Void> start() {
        return query("CREATE TABLE IF NOT EXISTS service " +
                "(url VARCHAR(128) NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "created_at INTEGER(8) NOT NULL, " +
                "status VARCHAR(8) DEFAULT \"UNKNOWN\");")
                .mapEmpty();
    }

    public DBConnector(Vertx vertx) {
        this(vertx, DB_PATH);
    }

    public Future<RowSet<Row>> query(String query) {
        return query(query, new JsonArray());
    }

    public Future<RowSet<Row>> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        return pool.preparedQuery(query)
                .execute(Tuple.from(params.stream().toArray()));
    }
}