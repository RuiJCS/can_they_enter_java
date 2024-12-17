package org.canthey.databases;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.canthey.jooq.generated.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.canthey.jooq.generated.Tables.USERS;

public class PostgresDB implements Database {

    private static final Logger logger = LogManager.getLogger(PostgresDB.class);

    public static final String USER_NAME = "username";
    public static final String PASSWORD = "password";
    public static final String CAN_THEY_ENTER_JAVA = "jdbc:postgresql://localhost:5432/can_they_enter_java";

    // Custom functional interface
    @FunctionalInterface
    interface UsersQuery {
        List<UsersRecord> executeQuery(DSLContext create);
    }

    private List<UsersRecord> getConnection(UsersQuery query) {
        // Connection is the only JDBC resource that we need
        // PreparedStatement and ResultSet are handled by jOOQ, internally
        try (Connection conn = DriverManager.getConnection(CAN_THEY_ENTER_JAVA, USER_NAME, PASSWORD)) {
            DSLContext create = DSL.using(conn, SQLDialect.POSTGRES );
            List<UsersRecord> result = query.executeQuery(create);
            conn.close();
            return result;
        } catch (Exception e) {
            logger.error("Something went wrong running SQL query {}", e.getMessage());
        }
        return null;
    }

    @Override
    public UsersRecord insertUser(String userName, String password) {
        UsersQuery query = create -> {
            UsersRecord user = new UsersRecord();
            user.setUsername(userName);
            user.setPassword(password);
            create.insertInto(USERS, USERS.USERNAME, USERS.PASSWORD).values(userName, password).execute();
            return create.select(USERS.ID, USERS.USERNAME).from(USERS).where(USERS.USERNAME.eq(userName)).fetchInto(UsersRecord.class);
        };
        List<UsersRecord> insertUserResult = getConnection(query);
        UsersRecord us = null;
        if (insertUserResult != null && !insertUserResult.isEmpty()) {
            us = insertUserResult.stream().findFirst().get();
            logger.debug("Created User {}", us.getUsername());
        } else {
            logger.debug("Wasn't able to create user {}", userName);
            logger.debug("insertUser result was null: {} or empty: {}", insertUserResult == null, insertUserResult.isEmpty());
        }
        return us;
    }

    @Override
    public List<UsersRecord> queryUsers() {
        UsersQuery query = create -> create.select(USERS.ID, USERS.USERNAME, USERS.PASSWORD).from(USERS).fetchInto(UsersRecord.class);

        List<UsersRecord> queryUsersResult = getConnection(query);

        List<UsersRecord> result = null;
        if (queryUsersResult != null && !queryUsersResult.isEmpty()) {
            result = queryUsersResult;
        } else {
            logger.debug("Wasn't able to query users");
            logger.debug("queryUsers result was null: {} or empty: {}", queryUsersResult == null, queryUsersResult.isEmpty());
        }

        return result;
    }

    @Override
    public List<UsersRecord> queryUser(String userName) {
        UsersQuery query = create -> create.select(USERS.ID, USERS.USERNAME, USERS.PASSWORD).from(USERS).where(USERS.USERNAME.eq(userName)).fetchInto(UsersRecord.class);

        List<UsersRecord> queryUserResult = getConnection(query);

        List<UsersRecord> result = null;
        if (queryUserResult != null && !queryUserResult.isEmpty()) {
            result = queryUserResult;
        } else {
            result = new ArrayList<>();
            logger.debug("Wasn't able to get user {}", userName);
            logger.debug("queryUser result was null: {} or empty: {}", queryUserResult == null, queryUserResult.isEmpty());
        }

        return result;
    }
}
