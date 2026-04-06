package com.example.music.config;

import com.example.music.utils.DefaultAvatarUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConsumerTableInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer tableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer'",
            Integer.class
        );
        if (tableExists == null || tableExists == 0) {
            return;
        }

        Integer deletedColumnExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer' AND COLUMN_NAME = 'deleted'",
            Integer.class
        );
        if (deletedColumnExists == null || deletedColumnExists == 0) {
            jdbcTemplate.execute(
                "ALTER TABLE consumer " +
                    "ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已注销' AFTER singer_id"
            );
        }

        jdbcTemplate.execute("UPDATE consumer SET deleted = 0 WHERE deleted IS NULL");
        migrateLegacyDefaultAvatars();
    }

    private void migrateLegacyDefaultAvatars() {
        List<Map<String, Object>> legacyConsumers = jdbcTemplate.queryForList(
                "SELECT id, username, singer_id " +
                        "FROM consumer " +
                        "WHERE avator IS NULL OR avator = '' OR avator = ?",
                DefaultAvatarUtils.LEGACY_DEFAULT_AVATAR
        );

        for (Map<String, Object> consumer : legacyConsumers) {
            Integer consumerId = consumer.get("id") instanceof Number ? ((Number) consumer.get("id")).intValue() : null;
            String username = consumer.get("username") == null ? null : String.valueOf(consumer.get("username"));
            Integer singerId = consumer.get("singer_id") instanceof Number ? ((Number) consumer.get("singer_id")).intValue() : null;

            if (consumerId == null) {
                continue;
            }

            String avatarPath = DefaultAvatarUtils.resolvePreparedDefaultAvatar(consumerId, username);
            jdbcTemplate.update("UPDATE consumer SET avator = ? WHERE id = ?", avatarPath, consumerId);

            if (singerId != null) {
                jdbcTemplate.update(
                        "UPDATE singer SET pic = ? WHERE id = ? AND (pic IS NULL OR pic = '' OR pic = ?)",
                        avatarPath,
                        singerId,
                        DefaultAvatarUtils.LEGACY_DEFAULT_AVATAR
                );
            }
        }
    }
}
