# 周期任务全栈工作台

> “每天当地九点”与“每隔二十四小时”不是同一条调度规则。本工作台让你并排创建两类任务，
> 生成**多个**未来触发实例，核对它们在普通日期、闰日与夏令时切换下的差异。

## 架构

| 层 | 技术 | 职责 |
| --- | --- | --- |
| 前端 | Vue 3 + TypeScript + Vite | 任务表单、未来触发时间线、本地 ↔ UTC 对照、暂停/恢复、演示时钟 |
| 后端 | Spring Boot + Quartz | 调度计算、实例生成、到点触发 |
| 存储 | PostgreSQL（`demo` profile 用 H2 便于本地起跳） | 任务定义 + 已生成的触发实例 |

## 核心语义

- **INTERVAL（固定间隔）**：锚定 UTC 的等差数列。夏令时不改变触发时刻，只改变它对应的本地挂钟时间。
- **DAILY（每日定点）**：每天在任务时区内解析本地挂钟时间：
  - 恰好一个偏移 → `NORMAL`
  - 零个偏移（春季拨快的“空洞”）→ `SKIPPED`，不触发，记录原因
  - 两个偏移（秋季回拨的“重叠”）→ `REPEATED_FIRST` / `REPEATED_SECOND`，触发两次

## 运行

```bash
# 1. 数据库（二选一）
docker compose up -d postgres                 # PostgreSQL
# 或不装数据库，直接用内嵌 H2：
#   后端启动时加 --spring.profiles.active=demo

# 2. 后端（默认连 localhost:5432 的 PostgreSQL）
cd backend && mvn spring-boot:run
# 免数据库演示：
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 3. 前端
cd frontend && npm install && npm run dev     # http://localhost:5173
```

## 验证调度语义（不依赖前端）

```bash
cd backend && mvn test
```

`InstanceGeneratorTest` 录制并核对了多组未来实例：

- 普通一周（上海 09:00，5 天全 NORMAL）
- 闰日 2024-02-29（2/27 → 3/2 五天序列）
- 夏令时·春：纽约 02:30，2026-03-08 该时刻不存在 → SKIPPED，3/9 起 UTC 提前一小时
- 夏令时·秋：纽约 01:30，2026-11-01 出现两次 → 05:30Z 与 06:30Z 两条实例
- 南半球反向：悉尼 2026-10-04 拨快
- 对照实验：同一锚点下“每天 09:00”与“每 86400 秒”在 3/8 之后 UTC 相差一小时

## 演示模式（可注入时钟）

后端时钟是一个可替换的 `MutableClock` Bean，演示接口默认开启：

```bash
curl localhost:8080/api/demo/clock                                  # 查看当前（UTC/纽约/柏林/上海）
curl -X POST localhost:8080/api/demo/clock/set \
  -H 'Content-Type: application/json' \
  -d '{"instant":"2026-03-06T12:00:00Z"}'                           # 冻结到夏令时切换前
curl -X POST localhost:8080/api/demo/clock/advance \
  -H 'Content-Type: application/json' -d '{"seconds":86400}'        # 拨快一天
curl -X POST localhost:8080/api/demo/clock/reset                    # 恢复真实时间
```

前端顶部的时钟条与“录制样例场景”面板（普通一周 / 闰日 / 春·跳过 / 秋·重复 / 间隔对照）
就是对这组接口的封装。

## API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/tasks` | 列出 / 创建任务 |
| DELETE | `/api/tasks/{id}` | 删除任务及其实例 |
| POST | `/api/tasks/{id}/pause` `/resume` | 暂停 / 恢复（同步 Quartz） |
| GET | `/api/tasks/{id}/preview?count=N` | 预览未来 N 天/条实例（不落库） |
| POST | `/api/tasks/{id}/generate?count=N` | 生成实例并持久化到 PostgreSQL |
| GET | `/api/tasks/{id}/instances` | 查询已生成实例 |
| GET/POST | `/api/demo/clock/**` | 演示时钟 |

## 说明与取舍

- Quartz 使用内存 JobStore，重启后由 `QuartzBootstrap` 从 PostgreSQL 重建调度；
  任务定义与触发实例始终落库，是事实来源。
- Quartz cron 自身的 DST 处理（平移空洞、重叠只触发一次）与本工作台的
  SKIPPED/REPEATED 语义不同；界面展示的实例列表以后端 `InstanceGenerator` 为准，
  这正是本工作台要可视化的差异点。
