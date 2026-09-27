<script setup lang="ts">
import { scenarios } from '../scenarios'
import type { Scenario } from '../scenarios'

const emit = defineEmits<{
  (e: 'select', scenario: Scenario): void
}>()
</script>

<template>
  <section class="card">
    <h2>录制样例（普通日期 / 闰日 / 夏令时切换）</h2>
    <p class="muted" style="margin: -4px 0 10px">
      选择样例会把演示时钟拨到该历史或未来时刻，并填充任务表单；随后可预览、保存多实例时间线。
    </p>
    <div class="scenario-bar">
      <button v-for="s in scenarios" :key="s.key" @click="emit('select', s)">
        {{ s.label }}
      </button>
    </div>
  </section>
</template>
