package com.example.music.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RecommendSeedTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS consumer_recommend_seed ("
                + "id INT NOT NULL AUTO_INCREMENT,"
                + "user_id INT NOT NULL,"
                + "seed_type VARCHAR(32) NOT NULL,"
                + "ref_id INT NULL,"
                + "ref_value VARCHAR(120) NULL,"
                + "weight DOUBLE NOT NULL DEFAULT 1,"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id),"
                + "KEY idx_recommend_seed_user_id (user_id),"
                + "KEY idx_recommend_seed_type (seed_type)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
        );
    }
}
