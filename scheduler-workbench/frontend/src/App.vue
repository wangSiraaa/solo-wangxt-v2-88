<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from './api/client'
import type { TaskDefinition, TriggerInstance } from './api/types'
import DemoClockBar from './components/DemoClockBar.vue'
import TaskForm from './components/TaskForm.vue'
import TaskList from './components/TaskList.vue'
import Timeline from './components/Timeline.vue'
import ScenarioPanel from './components/ScenarioPanel.vue'

const tasks = ref<TaskDefinition[]>([])
const selectedId = ref<string | null>(null)
const instances = ref<TriggerInstance[]>([])
const loading = ref(false)
const genCount = ref(14)
const clockBar = ref<InstanceType<typeof DemoClockBar> | null>(null)

async function loadTasks(keepSelection = true) {
  tasks.value = await api.listTasks()
  if (!keepSelection || !tasks.value.some(t => t.id === selectedId.value)) {
    selectedId.value = tasks.value[0]?.id ?? null
  }
  if (selectedId.value) await loadInstances()
}

async function loadInstances() {
  if (!selectedId.value) {
    instances.value = []
    return
  }
  loading.value = true
  try {
    instances.value = await api.instances(selectedId.value)
    if (instances.value.length === 0) await regenerate()
  } finally {
    loading.value = false
  }
}

async function regenerate() {
  if (!selectedId.value) return
  loading.value = true
  try {
    instances.value = await api.generate(selectedId.value, genCount.value)
  } finally {
    loading.value = false
  }
}

async function preview() {
  if (!selectedId.value) return
  loading.value = true
  try {
    instances.value = await api.preview(selectedId.value, genCount.value)
  } finally {
    loading.value = false
  }
}

async function pause(id: string) {
  await api.pause(id)
  await loadTasks()
}

async function resume(id: string) {
  await api.resume(id)
  await loadTasks()
}

async function remove(id: string) {
  await api.deleteTask(id)
  await loadTasks(false)
}

async function onScenarioDone() {
  await clockBar.value?.refresh()
  await loadTasks(false)
}

onMounted(() => loadTasks(false))
</script>

<template>
  <div class="page">
    <h1>周期任务全栈工作台</h1>
    <p class="subtitle">
      “每天当地九点”与“每隔二十四小时”不是同一条调度规则 —— 这里可以并排验证它们在闰日与夏令时下的差异。
    </p>

    <DemoClockBar ref="clockBar" @changed="regenerate" />

    <div class="columns">
      <div class="col">
        <TaskForm @created="loadTasks(false)" />
        <ScenarioPanel @done="onScenarioDone" />
        <TaskList
          :tasks="tasks"
          :selected-id="selectedId"
          @select="id => { selectedId = id; loadInstances() }"
          @pause="pause"
          @resume="resume"
          @remove="remove"
        />
      </div>
      <div class="col wide">
        <div class="toolbar">
          <label>生成范围
            <input type="number" v-model.number="genCount" min="1" max="400" style="width: 4.5em" />
            <span class="hint">（每日任务按天计，间隔任务按条计）</span>
          </label>
          <button class="primary" @click="regenerate">生成并保存实例</button>
          <button @click="preview">仅预览（不落库）</button>
        </div>
        <Timeline :instances="instances" :loading="loading" />
      </div>
    </div>
  </div>
</template>
