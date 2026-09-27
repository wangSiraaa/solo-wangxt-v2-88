import type {
  ClockView,
  FormModel,
  TaskDefinition,
  TimelinePreview,
  TriggerInstanceRow,
  ScheduleRequest
} from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message ?? `${response.status} ${response.statusText}`)
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}

export function scheduleFromForm(form: FormModel): ScheduleRequest {
  return {
    kind: form.kind,
    intervalSeconds: form.kind === 'FIXED_INTERVAL' ? form.intervalSeconds : null,
    hour: form.kind === 'DAILY_LOCAL' ? form.hour : null,
    minute: form.kind === 'DAILY_LOCAL' ? form.minute : null,
    zoneId: form.kind === 'DAILY_LOCAL' ? form.zoneId : null,
    anchorUtc: form.anchorUtc
  }
}

export const api = {
  clock: () => request<ClockView>('/api/clock'),
  pinClock: (instant: string) =>
    request<ClockView>('/api/clock/demo', {
      method: 'POST',
      body: JSON.stringify({ instant })
    }),
  advanceClock: (advanceSeconds: number) =>
    request<ClockView>('/api/clock/demo', {
      method: 'POST',
      body: JSON.stringify({ advanceSeconds })
    }),
  liveClock: () => request<ClockView>('/api/clock/live', { method: 'POST' }),
  zones: () => request<string[]>('/api/clock/zones'),

  listTasks: () => request<TaskDefinition[]>('/api/tasks'),
  createTask: (name: string, schedule: ScheduleRequest, instances: number) =>
    request<TaskDefinition>('/api/tasks', {
      method: 'POST',
      body: JSON.stringify({ name, schedule, instances })
    }),
  updateTask: (id: number, name: string, schedule: ScheduleRequest, instances?: number) =>
    request<TaskDefinition>(`/api/tasks/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name, schedule, instances })
    }),
  deleteTask: (id: number) => request<void>(`/api/tasks/${id}`, { method: 'DELETE' }),
  pauseTask: (id: number) =>
    request<TaskDefinition>(`/api/tasks/${id}/pause`, { method: 'POST' }),
  resumeTask: (id: number) =>
    request<TaskDefinition>(`/api/tasks/${id}/resume`, { method: 'POST' }),
  taskInstances: (id: number) =>
    request<TriggerInstanceRow[]>(`/api/tasks/${id}/instances`),
  generateInstances: (id: number, limit: number) =>
    request<TriggerInstanceRow[]>(
      `/api/tasks/${id}/instances/generate?limit=${limit}`,
      { method: 'POST' }
    ),

  preview: (schedule: ScheduleRequest, limit: number, viewZoneId: string) =>
    request<TimelinePreview>('/api/tasks/preview', {
      method: 'POST',
      body: JSON.stringify({ schedule, limit, viewZoneId })
    })
}
