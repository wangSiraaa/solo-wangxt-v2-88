package com.example.cycleworkbench.task;

import com.example.cycleworkbench.schedule.TimelineItem;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class TriggerInstanceRepository {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    public TriggerInstanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("trigger_instances")
                .usingGeneratedKeyColumns("id");
    }

    public void replaceForTask(long taskId, List<TimelineItem> items, Instant generatedAt) {
        jdbc.update("DELETE FROM trigger_instances WHERE task_id = ?", taskId);
        for (TimelineItem item : items) {
            var params = new java.util.HashMap<String, Object>();
            params.put("task_id", taskId);
            params.put("ordinal", item.ordinal());
            params.put("state", item.state().name());
            params.put("dst_note", item.note().name());
            params.put("nominal_local", Timestamp.valueOf(item.nominalLocal()));
            params.put("actual_utc",
                    item.actualUtc() == null ? null : Timestamp.from(item.actualUtc()));
            params.put("actual_local",
                    item.actualLocal() == null ? null : Timestamp.valueOf(item.actualLocal()));
            params.put("utc_offset", item.utcOffset() == null ? null : item.utcOffset().getId());
            params.put("explanation", item.explanation());
            params.put("generated_at", Timestamp.from(generatedAt));
            insert.execute(params);
        }
    }

    public List<TriggerInstanceRow> findByTask(long taskId) {
        return jdbc.query(
                """
                SELECT * FROM trigger_instances
                 WHERE task_id = ? ORDER BY ordinal
                """,
                (rs, n) -> {
                    TriggerInstanceRow row = new TriggerInstanceRow();
                    row.setId(rs.getLong("id"));
                    row.setTaskId(rs.getLong("task_id"));
                    row.setOrdinal(rs.getInt("ordinal"));
                    row.setState(InstanceState.valueOf(rs.getString("state")));
                    row.setDstNote(rs.getString("dst_note"));
                    row.setNominalLocal(rs.getTimestamp("nominal_local").toLocalDateTime());
                    Timestamp actual = rs.getTimestamp("actual_utc");
                    row.setActualUtc(actual == null ? null : actual.toInstant());
                    Timestamp actualLocal = rs.getTimestamp("actual_local");
                    row.setActualLocal(actualLocal == null ? null : actualLocal.toLocalDateTime());
                    row.setUtcOffset(rs.getString("utc_offset"));
                    row.setExplanation(rs.getString("explanation"));
                    row.setGeneratedAt(rs.getTimestamp("generated_at").toInstant());
                    return row;
                },
                taskId);
    }

    public void markFired(long taskId, Instant actualUtc) {
        // Quartz may fire on instants that were never persisted (only a window is generated);
        // update the closest SCHEDULED row at or before the fire instant.
        jdbc.update("""
                UPDATE trigger_instances
                   SET state = 'FIRED'
                 WHERE id = (
                       SELECT id FROM trigger_instances
                        WHERE task_id = ? AND state = 'SCHEDULED'
                          AND actual_utc <= ?
                        ORDER BY actual_utc DESC
                        FETCH FIRST 1 ROW ONLY)
                """, taskId, Timestamp.from(actualUtc));
    }
}
