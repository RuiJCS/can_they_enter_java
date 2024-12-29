package org.canthey.http;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.canthey.databases.Database;
import org.canthey.databases.InMemoryDB;
import org.canthey.databases.PostgresDB;
import org.canthey.jooq.generated.tables.records.UsersRecord;
import org.canthey.utils.PasswordUtils;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Requests {

  private static final int OK_CODE = 200;
  private static final int ERROR_CODE = 400;
  private static final int UNAUTHORIZED_CODE = 401;

  private static final Logger logger = LogManager.getLogger(Requests.class);
  private static final Database db = new InMemoryDB();

  public static String create_user(Request request, Response response) {
    String log = "Entering create user!";
    logger.debug(log);
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonResult = "";
    try {
      JsonNode jsonNode = objectMapper.readTree(request.body());
      String userName = jsonNode.get("username").asText();
      String password = jsonNode.get("password").asText();

      if (userName == null || userName.isBlank()) {
        response.status(ERROR_CODE);
        log = "Bad user name!";
        logger.debug(log);
        return "";
      }

      if (password == null || password.isBlank()) {
        response.status(ERROR_CODE);
        log = "Bad password!";
        logger.debug(log);
        return "";
      }

      String hashedPassword = PasswordUtils.hashPassword(password);

      List<UsersRecord> usersWithSameUserName = db.queryUser(userName);
      UsersRecord us = null;

      if (usersWithSameUserName.isEmpty()) {
        log = "Creating user " + userName + "!";
        logger.debug(log);

        us = db.insertUser(userName, hashedPassword);

        log =
            "Created user id: " +
                us.getId() +
                "; user name: " +
                us.getUsername() +
                "; password hash: " +
                us.getPassword() +
                "!";
        logger.debug(log);
        response.status(OK_CODE);
      } else {
        us = usersWithSameUserName.stream().findFirst().get();
        log =
            "User already created with user id: " +
                us.getId() +
                "; user name: " +
                us.getUsername() +
                "; password hash: " +
                us.getPassword() +
                "!";
        logger.debug(log);
        response.status(ERROR_CODE);
      }

      Map<String, String> result = new HashMap<>();
      result.put("id", us.getId().toString());
      result.put("username", us.getUsername());

      jsonResult = objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(result);
      response.type("application/json");
    } catch (JsonProcessingException e) {
      logger.error("Error reading request body!");
      logger.error(e.getMessage());
    }
    return jsonResult;
  }

  public static String query_users(Request request, Response response) {
    String log = "Entering query users!";
    logger.debug(log);
    List<UsersRecord> users = db.queryUsers();
    JsonFactory jsonFactory = new JsonFactory();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    JsonGenerator jsonGenerator;
    final StringWriter sw = new StringWriter();
    try {
      jsonGenerator = jsonFactory.createGenerator(byteArrayOutputStream);
      jsonGenerator.writeStartArray();
      for (UsersRecord us : users) {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", us.getId());
        jsonGenerator.writeStringField("username", us.getUsername());
        jsonGenerator.writeEndObject();
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.close();
    } catch (IOException e) {
      logger.error("Query users failed due to IOException: {}", e.getMessage());
      throw new RuntimeException(e);
    }
    response.type("application/json");
    return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
  }

  public static String login(Request request, Response response) {
    response.type("application/json");
    String log = "Entering login!";
    logger.debug(log);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode;
    String userName = "";
    String password = "";
    try {
      jsonNode = objectMapper.readTree(request.body());
      userName = jsonNode.get("username").asText();
      password = jsonNode.get("password").asText();
    } catch (JsonProcessingException e) {
      log = "Error creating object mapper";
      logger.debug(log);
    }
    List<UsersRecord> usersWithSameUserNameList = db.queryUser(userName);
    final String finalPassword = password;
    long usersWithSameUserNameAndPassword = usersWithSameUserNameList
        .stream()
        .filter(us ->
                PasswordUtils.verifyPassword(finalPassword, us.getPassword())
               )
        .count();
    if (usersWithSameUserNameAndPassword == 1) {
      response.status(OK_CODE);
      return "{\"message\":\"Logged in successfully!\"}";
    } else {
      response.status(UNAUTHORIZED_CODE);
      return "{\"message\":\"Log in credentials are wrong!\"}";
    }
  }
}
