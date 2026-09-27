<script setup lang="ts">
import type { InstanceStatus, TriggerInstance } from '../api/types'

const props = defineProps<{
  instances: TriggerInstance[]
  loading: boolean
}>()

const labels: Record<InstanceStatus, string> = {
  NORMAL: '正常',
  SKIPPED: '跳过',
  REPEATED_FIRST: '重复·第 1 次',
  REPEATED_SECOND: '重复·第 2 次'
}

function fmtUtc(iso: string | null): string {
  if (!iso) return '—（不触发）'
  return iso.replace('T', ' ').replace(/:\d\dZ$/, 'Z')
}
</script>

<template>
  <div class="card">
    <h2>未来触发时间线（本地 ↔ UTC 对照）</h2>
    <p v-if="props.loading" class="hint">计算中…</p>
    <p v-else-if="props.instances.length === 0" class="hint">
      选择左侧任务查看未来实例；SKIPPED / REPEATED 行展示夏令时造成的跳过或重复及处理原因。
    </p>
    <table v-else>
      <thead>
        <tr>
          <th>#</th>
          <th>本地时间（任务时区）</th>
          <th>UTC 触发时刻</th>
          <th>偏移</th>
          <th>状态</th>
          <th>处理原因</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="i in props.instances" :key="i.id" :class="'st-' + i.status">
          <td>{{ i.seq }}</td>
          <td><code>{{ i.localDateTime.replace('T', ' ') }}</code></td>
          <td><code>{{ fmtUtc(i.fireTimeUtc) }}</code></td>
          <td>{{ i.zoneOffset ?? '—' }}</td>
          <td><span class="badge" :class="'badge-' + i.status">{{ labels[i.status] }}</span></td>
          <td class="reason">{{ i.reason ?? '' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
