<script setup lang="ts">
import type { TaskDefinition } from '../types'

defineProps<{
  tasks: TaskDefinition[]
  selectedId: number | null
}>()

const emit = defineEmits<{
  (e: 'pause', task: TaskDefinition): void
  (e: 'resume', task: TaskDefinition): void
  (e: 'delete', task: TaskDefinition): void
  (e: 'edit', task: TaskDefinition): void
  (e: 'view', task: TaskDefinition): void
}>()

function describe(task: TaskDefinition): string {
  const s = task.schedule
  if (s.kind === 'DAILY_LOCAL') {
    return `${s.zoneId} 每天 ${String(s.hour).padStart(2, '0')}:${String(s.minute).padStart(2, '0')}`
  }
  const hours = (s.intervalSeconds ?? 0) / 3600
  return `每 ${Number.isInteger(hours) ? hours : (s.intervalSeconds ?? 0) + ' 秒'} 小时（UTC 固定间隔）`
}
</script>

<template>
  <section class="card">
    <h2>已保存任务（PostgreSQL）</h2>
    <p v-if="tasks.length === 0" class="muted">尚无任务，填写表单后点击“保存任务并生成实例”。</p>
    <table v-else>
      <thead>
        <tr>
          <th>#</th>
          <th>名称</th>
          <th>规则</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id" :style="selectedId === task.id ? 'background:#f0f7ff' : ''">
          <td class="mono">{{ task.id }}</td>
          <td>{{ task.name }}</td>
          <td class="muted">{{ describe(task) }}</td>
          <td>
            <span :class="['pill', task.state === 'ACTIVE' ? 'active' : 'paused']">
              {{ task.state === 'ACTIVE' ? '运行中' : '已暂停' }}
            </span>
          </td>
          <td style="white-space: nowrap">
            <button class="secondary" @click="emit('view', task)">查看实例</button>
            <button class="secondary" @click="emit('edit', task)">编辑</button>
            <button
              v-if="task.state === 'ACTIVE'"
              class="secondary"
              @click="emit('pause', task)"
            >暂停</button>
            <button
              v-else
              class="secondary"
              @click="emit('resume', task)"
            >恢复</button>
            <button class="danger" @click="emit('delete', task)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
