import type { ClockView, TaskDefinition, TriggerInstance } from './types'

const BASE = '/api'

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error ?? `HTTP ${res.status}`)
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export const api = {
  listTasks: () => req<TaskDefinition[]>('/tasks'),
  createTask: (body: Partial<TaskDefinition>) =>
    req<TaskDefinition>('/tasks', { method: 'POST', body: JSON.stringify(body) }),
  deleteTask: (id: string) => req<void>(`/tasks/${id}`, { method: 'DELETE' }),
  pause: (id: string) => req<TaskDefinition>(`/tasks/${id}/pause`, { method: 'POST' }),
  resume: (id: string) => req<TaskDefinition>(`/tasks/${id}/resume`, { method: 'POST' }),
  preview: (id: string, count: number) =>
    req<TriggerInstance[]>(`/tasks/${id}/preview?count=${count}`),
  generate: (id: string, count: number) =>
    req<TriggerInstance[]>(`/tasks/${id}/generate?count=${count}`, { method: 'POST' }),
  instances: (id: string) => req<TriggerInstance[]>(`/tasks/${id}/instances`),

  clock: () => req<ClockView>('/demo/clock'),
  setClock: (instant: string) =>
    req<ClockView>('/demo/clock/set', { method: 'POST', body: JSON.stringify({ instant }) }),
  advance: (seconds: number) =>
    req<ClockView>('/demo/clock/advance', { method: 'POST', body: JSON.stringify({ seconds }) }),
  resetClock: () => req<ClockView>('/demo/clock/reset', { method: 'POST' })
}
