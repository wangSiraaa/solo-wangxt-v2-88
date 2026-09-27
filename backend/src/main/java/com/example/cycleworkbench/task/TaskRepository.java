package com.example.cycleworkbench.task;

import com.example.cycleworkbench.schedule.ScheduleSpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    public TaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("task_definitions")
                .usingGeneratedKeyColumns("id");
    }

    public TaskDefinition save(TaskDefinition task) {
        var params = new java.util.HashMap<String, Object>();
        params.put("name", task.getName());
        params.put("kind", task.getSchedule().kind().name());
        params.put("interval_seconds",
                task.getSchedule().intervalSeconds() == null ? null
                        : task.getSchedule().intervalSeconds());
        params.put("hour", task.getSchedule().hour());
        params.put("minute", task.getSchedule().minute());
        params.put("zone_id", task.getSchedule().zoneId());
        params.put("anchor_utc", Timestamp.from(task.getSchedule().anchorUtc()));
        params.put("state", task.getState().name());
        params.put("created_at", Timestamp.from(task.getCreatedAt()));
        params.put("updated_at", Timestamp.from(task.getUpdatedAt()));
        Number key = insert.executeAndReturnKey(params);
        task.setId(key.longValue());
        return task;
    }

    public void update(TaskDefinition task) {
        jdbc.update("""
                UPDATE task_definitions
                   SET name = ?, kind = ?, interval_seconds = ?, hour = ?, minute = ?,
                       zone_id = ?, anchor_utc = ?, state = ?, updated_at = ?
                 WHERE id = ?
                """,
                task.getName(),
                task.getSchedule().kind().name(),
                task.getSchedule().intervalSeconds(),
                task.getSchedule().hour(),
                task.getSchedule().minute(),
                task.getSchedule().zoneId(),
                Timestamp.from(task.getSchedule().anchorUtc()),
                task.getState().name(),
                Timestamp.from(task.getUpdatedAt()),
                task.getId());
    }

    public Optional<TaskDefinition> findById(long id) {
        List<TaskDefinition> rows = jdbc.query(
                "SELECT * FROM task_definitions WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public List<TaskDefinition> findAll() {
        return jdbc.query("SELECT * FROM task_definitions ORDER BY id", MAPPER);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM task_definitions WHERE id = ?", id);
    }

    private static final RowMapper<TaskDefinition> MAPPER = (rs, n) -> map(rs);

    static TaskDefinition map(ResultSet rs) throws SQLException {
        ScheduleSpec spec = switch (ScheduleKind.valueOf(rs.getString("kind"))) {
            case FIXED_INTERVAL -> ScheduleSpec.fixedInterval(
                    rs.getLong("interval_seconds"),
                    rs.getTimestamp("anchor_utc").toInstant());
            case DAILY_LOCAL -> ScheduleSpec.dailyLocal(
                    rs.getInt("hour"),
                    rs.getInt("minute"),
                    rs.getString("zone_id"),
                    rs.getTimestamp("anchor_utc").toInstant());
        };
        TaskDefinition t = new TaskDefinition();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setSchedule(spec);
        t.setState(TaskState.valueOf(rs.getString("state")));
        t.setCreatedAt(instant(rs, "created_at"));
        t.setUpdatedAt(instant(rs, "updated_at"));
        return t;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}
