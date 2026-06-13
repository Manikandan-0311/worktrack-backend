package com.spearhead.ufc;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.ResultSet;

@Component
public class DatabaseConnectionChecker implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseConnectionChecker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        String sql = "SELECT username FROM base.user WHERE user_id = 2";
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                String userName = rs.getString("username");
                System.out.println("Database connection successful! user_name = " + userName);
            } else {
                System.out.println("Query executed, but no results found.");
            }
        } catch (Exception e) {
            System.err.println("Database connection or query failed: " + e.getMessage());
        }
    }
}