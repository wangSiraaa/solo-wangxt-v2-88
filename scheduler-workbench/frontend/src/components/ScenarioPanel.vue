<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../api/client'

const emit = defineEmits<{ done: [] }>()
const busy = ref('')
const error = ref('')

interface Scenario {
  key: string
  title: string
  desc: string
  clock: string
  task: Record<string, unknown>
  days: number
}

const scenarios: Scenario[] = [
  {
    key: 'normal',
    title: '普通一周',
    desc: '上海 09:00，无夏令时，连续 7 天全部正常',
    clock: '2026-09-20T16:00:00Z',
    task: { name: '样例·普通一周', scheduleType: 'DAILY', dailyTime: '09:00:00', timezone: 'Asia/Shanghai' },
    days: 7
  },
  {
    key: 'leap',
    title: '闰日 2024-02-29',
    desc: 'UTC 08:00，核对 2/27 → 3/2，包含闰日',
    clock: '2024-02-27T00:00:00Z',
    task: { name: '样例·闰日', scheduleType: 'DAILY', dailyTime: '08:00:00', timezone: 'UTC' },
    days: 5
  },
  {
    key: 'spring',
    title: '夏令时·春（跳过）',
    desc: '纽约 02:30，2026-03-08 该时刻不存在 → SKIPPED',
    clock: '2026-03-06T12:00:00Z',
    task: { name: '样例·春季拨快', scheduleType: 'DAILY', dailyTime: '02:30:00', timezone: 'America/New_York' },
    days: 5
  },
  {
    key: 'fall',
    title: '夏令时·秋（重复）',
    desc: '纽约 01:30，2026-11-01 该时刻出现两次 → 两条实例',
    clock: '2026-10-30T12:00:00Z',
    task: { name: '样例·秋季回拨', scheduleType: 'DAILY', dailyTime: '01:30:00', timezone: 'America/New_York' },
    days: 4
  },
  {
    key: 'interval',
    title: '间隔对照：每 24 小时',
    desc: '锚定 UTC，跨过 3/8 拨快后本地时间从 09:00 漂到 10:00',
    clock: '2026-03-06T12:00:00Z',
    task: { name: '样例·每24小时', scheduleType: 'INTERVAL', intervalSeconds: 86400, timezone: 'America/New_York' },
    days: 5
  }
]

async function run(s: Scenario) {
  busy.value = s.key
  error.value = ''
  try {
    await api.setClock(s.clock)
    const task = await api.createTask(s.task)
    await api.generate(task.id, s.days)
    emit('done')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <div class="card">
    <h2>录制样例场景</h2>
    <p class="hint">一键设定演示时钟、创建任务并生成多个未来实例，用于核对夏令时与闰日行为。</p>
    <div v-for="s in scenarios" :key="s.key" class="scenario">
      <button :disabled="busy !== ''" @click="run(s)">
        {{ busy === s.key ? '生成中…' : s.title }}
      </button>
      <span class="hint">{{ s.desc }}</span>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>
