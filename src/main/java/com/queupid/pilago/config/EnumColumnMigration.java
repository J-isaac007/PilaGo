package com.queupid.pilago.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps the enum columns of {@code document_request} (status, document type, purpose, hold reason)
 * open to new values.
 *
 * <p>Why: when Hibernate first creates the table it locks each enum column to the values that existed
 * then: a CHECK rule on PostgreSQL, an ENUM type on H2. {@code ddl-auto=update} never widens those
 * rules, so adding a value later (like the captain's new return reasons) would make saving fail on any
 * database created before. Java's enums already allow only valid values, so on every startup this
 * removes the database's copy of the rule. It is safe to run again and again.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnumColumnMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnumColumnMigration.class);

    static final String TABLE = "document_request";
    static final List<String> ENUM_COLUMNS = List.of("status", "document_type", "purpose", "hold_reason");

    private final JdbcTemplate jdbc;

    public EnumColumnMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String database = jdbc.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
        if ("PostgreSQL".equalsIgnoreCase(database)) {
            dropPostgresCheckRules();
        } else if ("H2".equalsIgnoreCase(database)) {
            turnH2EnumsIntoText();
        }
    }

    /** PostgreSQL: drop the CHECK rules Hibernate added for the enum columns. */
    private void dropPostgresCheckRules() {
        List<String> rules = jdbc.queryForList("""
                select con.conname
                from pg_constraint con
                join pg_class tbl on tbl.oid = con.conrelid
                where tbl.relname = ? and con.contype = 'c'
                """, String.class, TABLE);
        for (String rule : rules) {
            jdbc.execute("alter table " + TABLE + " drop constraint if exists \"" + rule.replace("\"", "") + "\"");
            log.info("Removed old value rule {} from {}", rule, TABLE);
        }
    }

    /** H2: the enum columns use H2's ENUM type with a fixed list; change them to plain text. */
    private void turnH2EnumsIntoText() {
        for (String column : ENUM_COLUMNS) {
            List<String> types = jdbc.queryForList("""
                    select data_type from information_schema.columns
                    where upper(table_name) = upper(?) and upper(column_name) = upper(?)
                    """, String.class, TABLE, column);
            if (!types.isEmpty() && "ENUM".equalsIgnoreCase(types.get(0))) {
                jdbc.execute("alter table " + TABLE + " alter column " + column + " set data type varchar(40)");
                log.info("Changed {}.{} from a fixed list to text", TABLE, column);
            }
        }
    }
}
