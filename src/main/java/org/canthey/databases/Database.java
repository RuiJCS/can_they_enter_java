package org.canthey.databases;

import java.util.List;
import org.canthey.jooq.generated.tables.records.UsersRecord;

public interface Database {
    UsersRecord insertUser(String userName, String password);
    List<UsersRecord> queryUsers();
    List<UsersRecord> queryUser(String userName);
}
