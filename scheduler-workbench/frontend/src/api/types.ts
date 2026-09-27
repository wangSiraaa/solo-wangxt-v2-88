export type ScheduleType = 'INTERVAL' | 'DAILY'
export type InstanceStatus = 'NORMAL' | 'SKIPPED' | 'REPEATED_FIRST' | 'REPEATED_SECOND'

export interface TaskDefinition {
  id: string
  name: string
  scheduleType: ScheduleType
  intervalSeconds: number | null
  dailyTime: string | null
  timezone: string
  paused: boolean
  createdAt: string
}

export interface TriggerInstance {
  id: string
  taskId: string
  seq: number
  fireTimeUtc: string | null
  localDateTime: string
  zoneOffset: string | null
  status: InstanceStatus
  reason: string | null
  firedAt: string | null
}

export interface ClockView {
  utc: string
  newYork: string
  berlin: string
  shanghai: string
}
