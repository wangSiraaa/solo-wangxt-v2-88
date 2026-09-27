<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'
import type { ClockView } from '../types'

const clock = ref<ClockView | null>(null)
const pinValue = ref('2026-03-07T00:00')
const error = ref('')
const busy = ref(false)

async function refresh() {
  clock.value = await api.clock()
}

async function pin() {
  error.value = ''
  busy.value = true
  try {
    const instant = new Date(pinValue.value).toISOString()
    clock.value = await api.pinClock(instant)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function advance(hours: number) {
  error.value = ''
  busy.value = true
  try {
    clock.value = await api.advanceClock(hours * 3600)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function live() {
  busy.value = true
  try {
    clock.value = await api.liveClock()
  } finally {
    busy.value = false
  }
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<template>
  <section class="card clock-bar">
    <div>
      <div class="muted">可注入时钟（演示模式）</div>
      <div class="clock-now">
        <span :class="['pill', clock?.demo ? 'paused' : 'active']">
          {{ clock?.demo ? '演示固定时钟' : '实时系统时钟' }}
        </span>
        <span class="mono" style="margin-left: 8px">{{ clock?.instant ?? '…' }}</span>
      </div>
    </div>
    <div class="field">
      <label>拨到（浏览器本地时间，提交为 UTC 瞬时）</label>
      <input v-model="pinValue" type="datetime-local" step="60" />
    </div>
    <div style="display: flex; gap: 8px; align-items: flex-end">
      <button :disabled="busy" @click="pin">固定时钟</button>
      <button class="secondary" :disabled="busy" @click="advance(1)">+1 小时</button>
      <button class="secondary" :disabled="busy" @click="advance(24)">+1 天</button>
      <button class="secondary" :disabled="busy" @click="live">恢复实时</button>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
  </section>
</template>
