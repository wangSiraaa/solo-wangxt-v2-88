<script setup lang="ts">
import type { TaskDefinition } from '../api/types'

const props = defineProps<{
  tasks: TaskDefinition[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  select: [id: string]
  pause: [id: string]
  resume: [id: string]
  remove: [id: string]
}>()

function describe(t: TaskDefinition): string {
  if (t.scheduleType === 'INTERVAL') {
    const s = t.intervalSeconds ?? 0
    if (s % 86400 === 0) return `每 ${s / 86400} 天（固定间隔）`
    if (s % 3600 === 0) return `每 ${s / 3600} 小时（固定间隔）`
    return `每 ${s} 秒（固定间隔）`
  }
  return `每天 ${t.dailyTime?.slice(0, 5)}（${t.timezone}）`
}
</script>

<template>
  <div class="card">
    <h2>任务列表</h2>
    <p v-if="props.tasks.length === 0" class="hint">暂无任务，请在上方创建或运行样例场景。</p>
    <div
      v-for="t in props.tasks"
      :key="t.id"
      class="task-row"
      :class="{ selected: t.id === props.selectedId, paused: t.paused }"
      @click="emit('select', t.id)"
    >
      <div class="task-main">
        <strong>{{ t.name }}</strong>
        <span class="hint">{{ describe(t) }}</span>
      </div>
      <div class="task-actions" @click.stop>
        <span v-if="t.paused" class="badge badge-paused">已暂停</span>
        <button v-if="!t.paused" @click="emit('pause', t.id)">暂停</button>
        <button v-else @click="emit('resume', t.id)">恢复</button>
        <button class="danger" @click="emit('remove', t.id)">删除</button>
      </div>
    </div>
  </div>
</template>
