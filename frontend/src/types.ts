export type ScheduleKind = 'FIXED_INTERVAL' | 'DAILY_LOCAL'
export type TaskState = 'ACTIVE' | 'PAUSED'
export type InstanceState = 'SCHEDULED' | 'SUPPRESSED' | 'FIRED'
export type DstNote =
  | 'NORMAL'
  | 'GAP_SKIPPED'
  | 'OVERLAP_FIRED_ONCE'
  | 'OFFSET_SHIFT'
  | 'INTERVAL_OFFSET_DRIFT'

export interface ScheduleSpec {
  kind: ScheduleKind
  intervalSeconds: number | null
  hour: number | null
  minute: number | null
  zoneId: string
  anchorUtc: string
}

export interface ScheduleRequest {
  kind: ScheduleKind
  intervalSeconds: number | null
  hour: number | null
  minute: number | null
  zoneId: string | null
  anchorUtc: string
}

export interface TimelineItem {
  ordinal: number
  state: InstanceState
  note: DstNote
  nominalLocal: string
  actualUtc: string | null
  actualLocal: string | null
  utcOffset: string | null
  explanation: string
  gapSecondsFromPrevious: number | null
}

export interface TimelinePreview {
  schedule: ScheduleSpec
  generatedAt: string
  fromUtc: string
  items: TimelineItem[]
  ruleSummary: string
}

export interface TaskDefinition {
  id: number
  name: string
  schedule: ScheduleSpec
  state: TaskState
  createdAt: string
  updatedAt: string
}

export interface TriggerInstanceRow {
  id: number
  taskId: number
  ordinal: number
  state: InstanceState
  dstNote: DstNote
  nominalLocal: string
  actualUtc: string | null
  actualLocal: string | null
  utcOffset: string | null
  explanation: string
  generatedAt: string
}

export interface ClockView {
  demo: boolean
  instant: string
  utcTime: string
}

export interface FormModel {
  name: string
  kind: ScheduleKind
  intervalSeconds: number
  hour: number
  minute: number
  zoneId: string
  anchorUtc: string
  viewZoneId: string
  limit: number
}
