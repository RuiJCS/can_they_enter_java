package org.canthey.databases;

import java.util.*;
import java.util.stream.Collectors;
import org.canthey.jooq.generated.tables.records.UsersRecord;

public class InMemoryDB implements Database {

    private static List<UsersRecord> users = new ArrayList<>();
    private static Long lastIndex = 1L;

    @Override
    public UsersRecord insertUser(String userName, String password) {
        UsersRecord us = new UsersRecord();
        us.setUsername(userName);
        us.setPassword(password);
        us.setId(lastIndex++);
        users.add(us);
        return us;
    }

    @Override
    public List<UsersRecord> queryUsers() {
        return users;
    }

    @Override
    public List<UsersRecord> queryUser(String userName) {
        return users
            .stream()
            .filter(u -> u.getUsername().equals(userName))
            .collect(Collectors.toList());
    }
}
