<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import type { ClockView } from '../api/types'

const emit = defineEmits<{ changed: [] }>()

const view = ref<ClockView | null>(null)
const setInput = ref('2026-03-06T12:00:00Z')
const error = ref('')

async function refresh() {
  try {
    view.value = await api.clock()
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function setClock() {
  try {
    view.value = await api.setClock(new Date(setInput.value).toISOString())
    error.value = ''
    emit('changed')
  } catch (e) {
    error.value = '无效时间：' + (e as Error).message
  }
}

async function advance(seconds: number) {
  view.value = await api.advance(seconds)
  emit('changed')
}

async function reset() {
  view.value = await api.resetClock()
  emit('changed')
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<template>
  <div class="clock-bar">
    <div class="clock-title">⏱ 演示时钟（可注入）</div>
    <div v-if="view" class="clock-now">
      <div><span class="label">UTC</span><code>{{ view.utc }}</code></div>
      <div><span class="label">纽约</span><code>{{ view.newYork }}</code></div>
      <div><span class="label">柏林</span><code>{{ view.berlin }}</code></div>
      <div><span class="label">上海</span><code>{{ view.shanghai }}</code></div>
    </div>
    <div class="clock-actions">
      <input v-model="setInput" placeholder="2026-03-06T12:00:00Z" />
      <button @click="setClock">设定</button>
      <button @click="advance(3600)">+1 小时</button>
      <button @click="advance(86400)">+1 天</button>
      <button @click="advance(604800)">+7 天</button>
      <button @click="reset">恢复真实时间</button>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>
