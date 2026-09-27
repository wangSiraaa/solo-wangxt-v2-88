# 周期任务全栈工作台

演示两条**不同**调度规则的区别：

- **固定间隔**：从锚点起每隔 N 秒触发，锚定 UTC（“每隔 24 小时”）。
- **每日本地时间**：在指定时区每天固定墙上钟点触发（“每天当地 09:00”）。

跨夏令时（DST）时两者行为完全不同：每日本地规则在 DST 切换日会出现 23/25 小时的实际
UTC 间隔，落在春令“空档”的本地时刻会被 Quartz 跳过，秋令“重叠”的本地时刻只触发一次；
固定间隔规则从不跳过/重复，但在非 UTC 时区的墙上显示时刻会漂移一小时。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + TypeScript + Vite（任务表单、未来触发时间线、当地/UTC 对照、暂停/恢复） |
| 调度 | Spring Boot 3 + Quartz（CronTrigger 带时区每日规则、SimpleTrigger 固定间隔） |
| 存储 | PostgreSQL（任务定义 `task_definitions`、已生成触发实例 `trigger_instances`） |
| 演示 | 可注入的 `MutableClock`：固定到任意瞬时、快进、恢复实时 |

## 关键设计

- 未来时间线中的**实际触发瞬时来自真实的 Quartz Trigger**（`getFireTimeAfter` 循环），
  DST 标注（`DstNote`）独立使用 `java.time` 的 `ZoneRules` 计算，两者分开，便于核对。
- 每个时间线条目同时给出：名义本地时间、实际本地时间、UTC 偏移、实际 UTC 瞬时、距上一
  实例的秒数，以及中文“处理原因”。
- 春令空档日会额外生成一条 `SUPPRESSED / GAP_SKIPPED` 记录（`actual_utc` 为 NULL）并
  写入 PostgreSQL，让“被跳过的那一天”在时间线上可见，而不是简单消失。
- Quartz 的实际 DST 行为（已用程序实测录制，见下方“实测结论”）：
  - 春令空档（如纽约 02:30）：该日**整日不触发**（不是顺延到 03:30）；
  - 秋令重叠（当地钟点必须真正落在重叠窗口，如纽约 01:30；02:30 已经是唯一的 EST 时刻）：
    只在**较晚的一次**（EST，06:30Z）触发一次，不重复执行，相邻实例相隔 25 小时；
  - 纽约 09:00 这类不落在空档的时刻：本地钟点不变，UTC 瞬时在 14:00Z/13:00Z 之间切换，
    相邻间隔为 23 或 25 小时。

## 录制样例（不是只看下一次运行）

页面顶部一键样例，随后生成并核对**多个**未来实例：

1. 普通日期：上海每天 09:00（全部严格 24 小时间隔，偏移 +08:00）。
2. 普通日期对照：UTC 固定 24 小时 vs 每日本地 09:00（在 UTC 时区结果一致）。
3. 闰日：2024-02-27 起每日 09:00 包含 **2024-02-29**；2025 平年同日直接到 03-01。
4. 闰日固定间隔：2024-02-29 起每 365 天 → 2025-02-28、2026-02-28。
5. DST 春令空档：2026-03 纽约每天 02:30，03-08 被抑制，实际间隔 47 小时。
6. DST 秋令重叠：2026-11-01 纽约 01:30 出现两次，只在 06:30Z（较晚的 EST）触发一次。
7. 纽约每天 09:00：03-08 起 UTC 从 14:00Z 变为 13:00Z（23 小时）；11-01 变回（25 小时）。
8. 固定 24 小时间隔 × 纽约：UTC 刚性每 24h，纽约墙上时间 07:00 → 08:00 漂移。
9. 南半球：悉尼 2026-10-04 02:30 春令空档被跳过。

后端 `ScheduleRecordingTest` 对以上样例逐个断言（含多实例序列、偏移、间隔、空档/重叠），
`PersistenceRecordingTest` 验证含 NULL `actual_utc` 的抑制行能往返数据库。

## 运行

### 1. 启动 PostgreSQL

```bash
docker compose up -d postgres
```

### 2. 启动后端（自动执行 db/schema.sql 建表）

```bash
cd backend
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
# 可用 DB_URL / DB_USERNAME / DB_PASSWORD 覆盖数据库连接
```

不想安装 PostgreSQL 时可用内置 H2（PostgreSQL 兼容模式）本地体验：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173 ，/api 代理到 8080
```

### 演示模式

页面顶部“可注入时钟”面板（或 `POST /api/clock/demo`）可把时钟固定到 2024 年闰日或 2026
年 DST 切换日并快进；`POST /api/clock/live` 恢复系统实时时钟。演示模式只改变调度计算与
实例生成所基于的“现在”，方便录制历史/未来样例。

## API 摘要

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/clock` | 当前时钟（是否演示模式、UTC 瞬时） |
| POST | `/api/clock/demo` | 固定时钟 `{instant}` 或快进 `{advanceSeconds}` |
| POST | `/api/clock/live` | 恢复实时 |
| POST | `/api/tasks/preview` | 不入库预览多实例时间线 |
| POST | `/api/tasks` | 保存任务并在 Quartz 注册，生成触发实例 |
| POST | `/api/tasks/{id}/pause` `/resume` | 暂停 / 恢复 Quartz Trigger |
| GET | `/api/tasks/{id}/instances` | 查看已持久化实例 |
| POST | `/api/tasks/{id}/instances/generate?limit=N` | 用当前时钟重新生成实例 |

## 测试

```bash
cd backend
mvn test
```

- `ScheduleRecordingTest`：Quartz 触发瞬时 + java.time 独立核对的多实例录制（普通日期、
  闰日、春令空档、秋令重叠、南北半球、固定间隔漂移）。
- `PersistenceRecordingTest`：Spring + JDBC，在 H2 的 PostgreSQL 兼容模式下验证存储。
