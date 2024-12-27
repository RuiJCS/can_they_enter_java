package org.canthey.databases;

import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.canthey.jooq.generated.tables.records.UsersRecord;
import org.json.JSONObject;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

public class InMemoryDB implements Database {

  private static final Logger logger = LogManager.getLogger(InMemoryDB.class);

  private static final JedisClientConfig config = DefaultJedisClientConfig.builder()
      .user("default")
      .build();
  private static Long lastIndex = 1L;
  private static final String redisIndex = "users";
  private static final String redisKey = "user";

  private final UnifiedJedis jedis = new UnifiedJedis(
      URI.create("redis://localhost:6379"),
      config);

  @Override
  public UsersRecord insertUser(String userName, String password) {
    UsersRecord us = new UsersRecord();
    us.setUsername(userName);
    us.setPassword(password);
    us.setId(lastIndex);

    JSONObject user = new JSONObject().put(
            "username",
            userName)
        .put(
            "password",
            password)
        .put(
            "index",
            lastIndex);
    String result = jedis.jsonSet(
        redisKey + ":" + lastIndex,
        new Path2("$"),
        user.toString());
    lastIndex++;

    return us;
  }

  @Override
  public List<UsersRecord> queryUsers() {
    String queryText = "*";
    SearchResult citiesResult = jedis.ftSearch(
        redisIndex,
        new Query(queryText).returnFields(
            "index",
            "username"));

    return citiesResult.getDocuments()
        .stream()
        .map(d -> new UsersRecord(
            d.getString("username"),
            "",
            Long.parseLong(d.getString("index"))))
        .toList();
  }

  @Override
  public List<UsersRecord> queryUser(String userName) {
    String queryText = "@username:\"" + userName + "\"";
    SearchResult citiesResult = jedis.ftSearch(
        redisIndex,
        new Query(queryText).returnFields(
            "index",
            "username",
            "password"));

    return citiesResult.getDocuments()
        .stream()
        .map(d -> new UsersRecord(
            d.getString("username"),
            d.getString("password"),
            Long.parseLong(d.getString("index"))))
        .toList();
  }
}
