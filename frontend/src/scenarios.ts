import type { FormModel } from './types'

export interface Scenario {
  key: string
  label: string
  pinInstant: string
  form: Partial<FormModel>
}

/**
 * 录制用样例：覆盖普通日期、闰日以及两种 DST 切换（北半球/南半球、空档/重叠），
 * 并同时提供固定间隔作对照。
 */
export const scenarios: Scenario[] = [
  {
    key: 'plain-daily',
    label: '普通日期 · 上海每天 09:00',
    pinInstant: '2026-09-27T00:00:00Z',
    form: {
      name: '上海每日报表',
      kind: 'DAILY_LOCAL',
      hour: 9,
      minute: 0,
      zoneId: 'Asia/Shanghai',
      viewZoneId: 'Asia/Shanghai',
      anchorUtc: '2026-09-27T00:00:00Z',
      limit: 14
    }
  },
  {
    key: 'plain-interval',
    label: '普通日期对照 · 每 24 小时',
    pinInstant: '2026-09-27T01:00:00Z',
    form: {
      name: 'UTC 固定 24 小时轮询',
      kind: 'FIXED_INTERVAL',
      intervalSeconds: 86400,
      zoneId: 'UTC',
      viewZoneId: 'Asia/Shanghai',
      anchorUtc: '2026-09-27T01:00:00Z',
      limit: 14
    }
  },
  {
    key: 'leap-daily',
    label: '闰日 2024 · 2 月底每日 09:00',
    pinInstant: '2024-02-27T00:00:00Z',
    form: {
      name: '闰日提醒（2024）',
      kind: 'DAILY_LOCAL',
      hour: 9,
      minute: 0,
      zoneId: 'Asia/Shanghai',
      viewZoneId: 'Asia/Shanghai',
      anchorUtc: '2024-02-27T00:00:00Z',
      limit: 8
    }
  },
  {
    key: 'leap-year-interval',
    label: '闰日固定间隔 · 每 365 天',
    pinInstant: '2024-02-29T00:00:00Z',
    form: {
      name: '年度任务（从闰日起）',
      kind: 'FIXED_INTERVAL',
      intervalSeconds: 365 * 86400,
      zoneId: 'UTC',
      viewZoneId: 'UTC',
      anchorUtc: '2024-02-29T00:00:00Z',
      limit: 4
    }
  },
  {
    key: 'ny-spring-gap',
    label: 'DST 春令空档 · 纽约每天 02:30',
    pinInstant: '2026-03-07T00:00:00Z',
    form: {
      name: '纽约凌晨批处理（落在空档）',
      kind: 'DAILY_LOCAL',
      hour: 2,
      minute: 30,
      zoneId: 'America/New_York',
      viewZoneId: 'America/New_York',
      anchorUtc: '2026-03-07T00:00:00Z',
      limit: 8
    }
  },
  {
    key: 'ny-spring-nine',
    label: 'DST 春令 · 纽约每天 09:00（UTC 漂移）',
    pinInstant: '2026-03-06T12:00:00Z',
    form: {
      name: '纽约开盘任务 09:00',
      kind: 'DAILY_LOCAL',
      hour: 9,
      minute: 0,
      zoneId: 'America/New_York',
      viewZoneId: 'America/New_York',
      anchorUtc: '2026-03-06T12:00:00Z',
      limit: 8
    }
  },
  {
    key: 'ny-fall-overlap',
    label: 'DST 秋令重叠 · 纽约每天 01:30',
    pinInstant: '2026-10-31T00:00:00Z',
    form: {
      name: '纽约凌晨批处理（落在重叠）',
      kind: 'DAILY_LOCAL',
      hour: 1,
      minute: 30,
      zoneId: 'America/New_York',
      viewZoneId: 'America/New_York',
      anchorUtc: '2026-10-31T00:00:00Z',
      limit: 6
    }
  },
  {
    key: 'interval-dst-drift',
    label: '固定 24h × DST · 纽约墙上漂移',
    pinInstant: '2026-03-07T12:00:00Z',
    form: {
      name: '24 小时同步任务（观察纽约漂移）',
      kind: 'FIXED_INTERVAL',
      intervalSeconds: 86400,
      zoneId: 'UTC',
      viewZoneId: 'America/New_York',
      anchorUtc: '2026-03-07T12:00:00Z',
      limit: 6
    }
  },
  {
    key: 'sydney-spring-gap',
    label: '南半球 DST · 悉尼每天 02:30',
    pinInstant: '2026-10-03T12:00:00Z',
    form: {
      name: '悉尼凌晨任务（南半球空档）',
      kind: 'DAILY_LOCAL',
      hour: 2,
      minute: 30,
      zoneId: 'Australia/Sydney',
      viewZoneId: 'Australia/Sydney',
      anchorUtc: '2026-10-03T12:00:00Z',
      limit: 6
    }
  }
]
