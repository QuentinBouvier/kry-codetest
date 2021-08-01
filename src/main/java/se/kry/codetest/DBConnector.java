package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;

public class DBConnector {

  private static final String DB_PATH = "poller.db";
  private final SQLClient client;

  public DBConnector(Vertx vertx, String path){
    JsonObject config = new JsonObject()
        .put("url", "jdbc:sqlite:" + path)
        .put("driver_class", "org.sqlite.JDBC")
        .put("max_pool_size", 30);

    client = JDBCClient.createShared(vertx, config);
  }

  public Future<ResultSet> start() {
    return query("CREATE TABLE IF NOT EXISTS service " +
            "(url VARCHAR(128) NOT NULL, " +
            "name VARCHAR(255) NOT NULL, " +
            "created_at INTEGER(8) NOT NULL, " +
            "status VARCHAR(8) DEFAULT \"UNKNOWN\");");
  }

  public DBConnector(Vertx vertx) {
    this(vertx, DB_PATH);
  }

  public Future<ResultSet> query(String query) {
    return query(query, new JsonArray());
  }


  public Future<ResultSet> query(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    if(!query.endsWith(";")) {
      query = query + ";";
    }

    Future<ResultSet> queryResultFuture = Future.future();

    client.queryWithParams(query, params, result -> {
      if(result.failed()){
        queryResultFuture.fail(result.cause());
      } else {
        queryResultFuture.complete(result.result());
      }
    });
    return queryResultFuture;
  }
}
