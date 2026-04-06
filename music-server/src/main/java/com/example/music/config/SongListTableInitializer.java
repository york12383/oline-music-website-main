package com.example.music.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SongListTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer tableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'song_list'",
            Integer.class
        );
        if (tableExists == null || tableExists == 0) {
            return;
        }

        Integer createTimeColumnExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'song_list' AND COLUMN_NAME = 'create_time'",
            Integer.class
        );
        if (createTimeColumnExists == null || createTimeColumnExists == 0) {
            jdbcTemplate.execute(
                "ALTER TABLE song_list " +
                    "ADD COLUMN create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间' AFTER consumer"
            );
        }

        Integer createTimeIndexExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'song_list' AND INDEX_NAME = 'idx_song_list_create_time'",
            Integer.class
        );
        if (createTimeIndexExists == null || createTimeIndexExists == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_song_list_create_time ON song_list(create_time)");
        }

        jdbcTemplate.execute(
            "UPDATE song_list sl " +
                "LEFT JOIN (" +
                "    SELECT inferred.id, MIN(inferred.candidate_time) AS inferred_create_time " +
                "    FROM (" +
                "        SELECT c.song_list_id AS id, c.create_time AS candidate_time " +
                "        FROM collect c " +
                "        WHERE c.song_list_id IS NOT NULL AND c.type = 1 " +
                "        UNION ALL " +
                "        SELECT cm.song_list_id AS id, cm.create_time AS candidate_time " +
                "        FROM comment cm " +
                "        WHERE cm.song_list_id IS NOT NULL " +
                "        UNION ALL " +
                "        SELECT ls.song_list_id AS id, s.create_time AS candidate_time " +
                "        FROM list_song ls " +
                "        INNER JOIN song s ON s.id = ls.song_id " +
                "        WHERE s.create_time IS NOT NULL " +
                "        UNION ALL " +
                "        SELECT sl2.id AS id, c2.create_time AS candidate_time " +
                "        FROM song_list sl2 " +
                "        INNER JOIN consumer c2 ON c2.id = sl2.consumer " +
                "        WHERE c2.create_time IS NOT NULL " +
                "    ) inferred " +
                "    WHERE inferred.candidate_time IS NOT NULL " +
                "    GROUP BY inferred.id " +
                ") mapping ON mapping.id = sl.id " +
                "SET sl.create_time = mapping.inferred_create_time " +
                "WHERE sl.create_time IS NULL"
        );

        jdbcTemplate.execute(
            "ALTER TABLE song_list " +
                "MODIFY COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'"
        );
    }
}
