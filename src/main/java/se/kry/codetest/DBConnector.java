package se.kry.codetest;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.jdbcclient.JDBCPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.reactivex.sqlclient.Tuple;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidParameterException;
import java.util.stream.StreamSupport;

@Slf4j
public class DBConnector {

    private static final String DB_PATH = "poller.db";

    @Getter
    private final JDBCPool pool;

    public DBConnector(Vertx vertx, String path) {
        log.debug("Instantiating {}", this.getClass().getName());
        String dbUrl = "jdbc:sqlite:" + path;
        String driverClass = "org.sqlite.JDBC";
        int maxPoolSize = 30;
        log.debug("Creating JDBC connection pool with params: [url: {}, driver_class: {}, max_pool_size: {}]...", dbUrl, driverClass, maxPoolSize);
        pool = JDBCPool.pool(vertx,
                new JsonObject()
                        .put("url", dbUrl)
                        .put("driver_class", driverClass)
                        .put("max_pool_size", maxPoolSize)
        );
    }

    /**
     * Execute the queries to prepare the DB
     *
     * @return Async result
     */
    public Completable start() {
        log.info("Called DBConnector.start() -> Initializing DB...");
        return query("CREATE TABLE IF NOT EXISTS service " +
                "(url VARCHAR(128) NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "created_at INTEGER(8) NOT NULL, " +
                "status VARCHAR(8) DEFAULT \"UNKNOWN\");")
                .doOnSubscribe(disposable -> log.debug("Query for DB preparation..."))
                .doAfterSuccess(rows -> log.info("DB Initialised successfully"))
                .ignoreElement();
    }

    public DBConnector(Vertx vertx) {
        this(vertx, DB_PATH);
    }

    /**
     * Execute a sql query on the sqlite DB without parameters
     *
     * @param query The SQLite query
     * @return The sql result as a RowSet
     */
    public Maybe<RowSet<Row>> query(String query) {
        return query(query, new JsonArray());
    }

    /**
     * Execute a sql query on the SQLite DB and returns the result
     *
     * @param query  The SQLite query to prepare
     * @param params A list to replace the placeholders in the query
     * @return The sql result as a RowSet
     */
    public Maybe<RowSet<Row>> query(String query, JsonArray params) {
        if (StringUtils.isBlank(query)) {
            return Maybe.error(new InvalidParameterException("Query is null or empty"));
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        log.debug("Query: ({}) Params: {}", query, params.toString());

        String finalQuery = query;

        return pool.rxWithConnection((Function<SqlConnection, Maybe<RowSet<Row>>>) sqlConnection ->
                sqlConnection
                        .preparedQuery(finalQuery)
                        .rxExecute(Tuple.from(params.stream().toArray()))
                        .doOnSuccess(rows -> log.debug("Query success. Fetched {} row(s)", StreamSupport.stream(rows.spliterator(), false).count()))
                        .toMaybe()
        );
    }
}
