package com.example.music.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeedbackTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS feedback ("
                + "id INT NOT NULL AUTO_INCREMENT,"
                + "user_id INT NULL,"
                + "username VARCHAR(100) NULL,"
                + "feedback_type VARCHAR(32) NOT NULL DEFAULT 'other',"
                + "title VARCHAR(120) NOT NULL,"
                + "content TEXT NOT NULL,"
                + "contact VARCHAR(120) NULL,"
                + "page_path VARCHAR(255) NULL,"
                + "status TINYINT NOT NULL DEFAULT 0,"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id),"
                + "KEY idx_feedback_user_id (user_id),"
                + "KEY idx_feedback_create_time (create_time)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
        );
    }
}
