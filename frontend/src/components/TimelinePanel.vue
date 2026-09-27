<script setup lang="ts">
import { computed } from 'vue'
import type { TaskDefinition, TimelineItem, TimelinePreview, TriggerInstanceRow } from '../types'
import TimelineTable from './TimelineTable.vue'

const props = defineProps<{
  preview: TimelinePreview | null
  persisted: TriggerInstanceRow[]
  selectedTask: TaskDefinition | null
}>()

// Persisted rows store the suppression but not the previous-row gap; derive it for display
// across the scheduled (non-suppressed) rows.
const persistedItems = computed<TimelineItem[]>(() => {
  let previousUtc: string | null = null
  return props.persisted.map((r) => {
    const gap =
      r.actualUtc && previousUtc
        ? (new Date(r.actualUtc).getTime() - new Date(previousUtc).getTime()) / 1000
        : null
    if (r.actualUtc) previousUtc = r.actualUtc
    return {
      ordinal: r.ordinal,
      state: r.state,
      note: r.dstNote,
      nominalLocal: r.nominalLocal,
      actualUtc: r.actualUtc,
      actualLocal: r.actualLocal,
      utcOffset: r.utcOffset,
      explanation: r.explanation,
      gapSecondsFromPrevious: gap
    }
  })
})
</script>

<template>
  <section class="card">
    <h2>未来触发时间线（多实例录制与核对）</h2>
    <template v-if="selectedTask">
      <div class="summary">
        任务 <strong>#{{ selectedTask.id }} {{ selectedTask.name }}</strong>
        已持久化实例（PostgreSQL <span class="mono">trigger_instances</span>），
        含 DST 空档的抑制行。
      </div>
      <TimelineTable :items="persistedItems" persisted />
    </template>
    <template v-else-if="preview">
      <div class="summary">{{ preview.ruleSummary }}</div>
      <p class="muted" style="margin-top: -4px">
        生成时刻 <span class="mono">{{ preview.generatedAt }}</span>
        ，从 <span class="mono">{{ preview.fromUtc }}</span> 之后开始计算；下表同时给出当地时间与 UTC 对照。
      </p>
      <TimelineTable :items="preview.items" />
    </template>
    <p v-else class="muted">
      选择上方样例或配置规则后点击“预览未来实例”。时间线会展示接下来若干次触发，而不仅仅是下次运行时间。
    </p>
  </section>
</template>
