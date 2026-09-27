<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, scheduleFromForm } from './api'
import type {
  FormModel,
  TaskDefinition,
  TimelinePreview,
  TriggerInstanceRow
} from './types'
import type { Scenario } from './scenarios'
import ClockBar from './components/ClockBar.vue'
import ScenarioBar from './components/ScenarioBar.vue'
import TaskForm from './components/TaskForm.vue'
import TaskList from './components/TaskList.vue'
import TimelinePanel from './components/TimelinePanel.vue'

const clockBar = ref<InstanceType<typeof ClockBar> | null>(null)
const zones = ref<string[]>([])
const tasks = ref<TaskDefinition[]>([])
const preview = ref<TimelinePreview | null>(null)
const persisted = ref<TriggerInstanceRow[]>([])
const selectedTask = ref<TaskDefinition | null>(null)
const editId = ref<number | null>(null)
const busy = ref(false)
const error = ref('')

const form = ref<FormModel>({
  name: '',
  kind: 'DAILY_LOCAL',
  intervalSeconds: 86400,
  hour: 9,
  minute: 0,
  zoneId: 'America/New_York',
  anchorUtc: new Date().toISOString(),
  viewZoneId: 'America/New_York',
  limit: 14
})

function patch(p: Partial<FormModel>) {
  form.value = { ...form.value, ...p }
}

async function loadTasks() {
  tasks.value = await api.listTasks()
}

async function doPreview() {
  error.value = ''
  busy.value = true
  selectedTask.value = null
  try {
    preview.value = await api.preview(
      scheduleFromForm(form.value),
      form.value.limit,
      form.value.kind === 'FIXED_INTERVAL' ? form.value.viewZoneId : form.value.zoneId
    )
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function doSave() {
  error.value = ''
  busy.value = true
  try {
    const schedule = scheduleFromForm(form.value)
    const saved = editId.value
      ? await api.updateTask(editId.value, form.value.name, schedule, form.value.limit)
      : await api.createTask(form.value.name, schedule, form.value.limit)
    editId.value = null
    preview.value = null
    await loadTasks()
    await viewInstances(saved)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function applyScenario(scenario: Scenario) {
  error.value = ''
  busy.value = true
  try {
    await api.pinClock(scenario.pinInstant)
    form.value = { ...form.value, ...scenario.form } as FormModel
    preview.value = null
    selectedTask.value = null
    editId.value = null
    await clockBar.value?.refresh()
    await doPreview()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function pause(task: TaskDefinition) {
  await api.pauseTask(task.id)
  await loadTasks()
}

async function resume(task: TaskDefinition) {
  await api.resumeTask(task.id)
  await loadTasks()
}

async function remove(task: TaskDefinition) {
  if (!window.confirm(`删除任务「${task.name}」及其全部已生成实例？`)) return
  await api.deleteTask(task.id)
  if (selectedTask.value?.id === task.id) selectedTask.value = null
  await loadTasks()
}

async function viewInstances(task: TaskDefinition) {
  selectedTask.value = task
  preview.value = null
  editId.value = null
  persisted.value = await api.taskInstances(task.id)
}

async function editTask(task: TaskDefinition) {
  selectedTask.value = null
  preview.value = null
  editId.value = task.id
  const s = task.schedule
  form.value = {
    name: task.name,
    kind: s.kind,
    intervalSeconds: s.intervalSeconds ?? 86400,
    hour: s.hour ?? 9,
    minute: s.minute ?? 0,
    zoneId: s.kind === 'DAILY_LOCAL' ? s.zoneId : 'UTC',
    anchorUtc: s.anchorUtc,
    viewZoneId: s.kind === 'FIXED_INTERVAL' ? 'America/New_York' : s.zoneId,
    limit: 14
  }
}

function cancelEdit() {
  editId.value = null
}

onMounted(async () => {
  zones.value = await api.zones()
  await loadTasks().catch((e) => (error.value = e.message))
})
</script>

<template>
  <h1>周期任务全栈工作台</h1>
  <p class="subtitle">
    “每天当地 09:00”与“每隔 24 小时”是两条不同的调度规则：前者锚定时区墙上时间（DST 当天会有
    23/25 小时间隔，空档时刻会被跳过），后者锚定 UTC、本地显示时刻会漂移。下方基于 Spring Boot +
    Quartz 计算并录制多个未来实例，PostgreSQL 持久化，Vue 3 展示当地时间 / UTC 对照。
  </p>

  <ClockBar ref="clockBar" />
  <ScenarioBar @select="applyScenario" />
  <TaskForm
    :model="form"
    :zones="zones"
    :busy="busy"
    :edit-id="editId"
    @update="patch"
    @preview="doPreview"
    @save="doSave"
    @cancel-edit="cancelEdit"
  />
  <div v-if="error" class="error" style="margin-bottom: 16px">{{ error }}</div>
  <TimelinePanel
    :preview="preview"
    :persisted="persisted"
    :selected-task="selectedTask"
  />
  <TaskList
    :tasks="tasks"
    :selected-id="selectedTask?.id ?? null"
    @pause="pause"
    @resume="resume"
    @delete="remove"
    @edit="editTask"
    @view="viewInstances"
  />
</template>
