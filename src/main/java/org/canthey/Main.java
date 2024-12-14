package org.canthey;

import static spark.Spark.*;

import org.canthey.http.Requests;

public class Main {

    public static void main(String[] args) {
        port(3001);
        post("/create_user", Requests::create_user);
        get("/query_users", Requests::query_users);
        get("/login_user", Requests::login);
    }
}
