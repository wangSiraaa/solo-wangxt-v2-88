<script setup lang="ts">
import type { DstNote, InstanceState, TimelineItem } from '../types'

defineProps<{
  items: TimelineItem[]
  /** When showing persisted rows, state may be FIRED. */
  persisted?: boolean
}>()

const noteLabels: Record<DstNote, string> = {
  NORMAL: '正常',
  GAP_SKIPPED: '空档跳过',
  OVERLAP_FIRED_ONCE: '重叠只触发一次',
  OFFSET_SHIFT: 'UTC 偏移切换',
  INTERVAL_OFFSET_DRIFT: '本地时刻漂移'
}

const noteClass: Record<DstNote, string> = {
  NORMAL: 'normal',
  GAP_SKIPPED: 'gap',
  OVERLAP_FIRED_ONCE: 'overlap',
  OFFSET_SHIFT: 'shift',
  INTERVAL_OFFSET_DRIFT: 'drift'
}

const stateLabels: Record<InstanceState, string> = {
  SCHEDULED: '已排期',
  SUPPRESSED: '已抑制',
  FIRED: '已触发'
}

function gapLabel(seconds: number | null): string {
  if (seconds === null) return '—'
  const hours = seconds / 3600
  if (Number.isInteger(hours)) return `${hours} 小时`
  return `${seconds} 秒`
}

function localUtcDual(item: TimelineItem): string {
  if (!item.actualLocal) return '—'
  return item.actualLocal.replace('T', ' ')
}
</script>

<template>
  <div style="overflow-x: auto">
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>状态</th>
          <th>DST 判定</th>
          <th>名义本地时间</th>
          <th>当地时间</th>
          <th>UTC 偏移</th>
          <th>UTC 实际触发</th>
          <th>距上一实例</th>
          <th>处理原因</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="item in items"
          :key="item.ordinal"
          :class="{
            suppressed: item.state === 'SUPPRESSED',
            fired: persisted && item.state === 'FIRED'
          }"
        >
          <td class="mono">{{ item.ordinal }}</td>
          <td>
            <span :class="['pill', item.state === 'FIRED' ? 'fired' : item.state === 'SUPPRESSED' ? 'gap' : 'normal']">
              {{ stateLabels[item.state] }}
            </span>
          </td>
          <td><span :class="['pill', noteClass[item.note]]">{{ noteLabels[item.note] }}</span></td>
          <td class="mono">{{ item.nominalLocal.replace('T', ' ') }}</td>
          <td class="mono">{{ localUtcDual(item) }}</td>
          <td class="mono">{{ item.utcOffset ?? '—' }}</td>
          <td class="mono">{{ item.actualUtc ? item.actualUtc.replace('T', ' ').replace('Z', ' Z') : '不触发' }}</td>
          <td class="mono">{{ gapLabel(item.gapSecondsFromPrevious) }}</td>
          <td class="explanation">{{ item.explanation }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
