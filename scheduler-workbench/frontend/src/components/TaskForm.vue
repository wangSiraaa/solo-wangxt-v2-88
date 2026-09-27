<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '../api/client'
import type { ScheduleType } from '../api/types'

const emit = defineEmits<{ created: [] }>()

const zones = [
  'UTC',
  'Asia/Shanghai',
  'America/New_York',
  'Europe/Berlin',
  'Australia/Sydney',
  'Pacific/Auckland'
]

const form = reactive({
  name: '',
  scheduleType: 'DAILY' as ScheduleType,
  intervalValue: 24,
  intervalUnit: 3600,
  dailyTime: '09:00',
  timezone: 'Asia/Shanghai'
})
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await api.createTask({
      name: form.name || '未命名任务',
      scheduleType: form.scheduleType,
      intervalSeconds:
        form.scheduleType === 'INTERVAL' ? form.intervalValue * form.intervalUnit : null,
      dailyTime: form.scheduleType === 'DAILY' ? form.dailyTime + ':00' : null,
      timezone: form.timezone
    })
    form.name = ''
    emit('created')
  } catch (e) {
    error.value = (e as Error).message
  }
}
</script>

<template>
  <div class="card">
    <h2>新建周期任务</h2>
    <label>名称 <input v-model="form.name" placeholder="如：每日报表" /></label>

    <div class="radio-row">
      <label>
        <input type="radio" value="DAILY" v-model="form.scheduleType" />
        每日定点（带时区）
      </label>
      <label>
        <input type="radio" value="INTERVAL" v-model="form.scheduleType" />
        固定间隔
      </label>
    </div>

    <div v-if="form.scheduleType === 'DAILY'" class="row">
      <label>本地时间 <input type="time" v-model="form.dailyTime" /></label>
      <label>时区
        <select v-model="form.timezone">
          <option v-for="z in zones" :key="z" :value="z">{{ z }}</option>
        </select>
      </label>
    </div>

    <div v-else class="row">
      <label>每
        <input type="number" min="1" v-model.number="form.intervalValue" style="width: 5em" />
        <select v-model.number="form.intervalUnit">
          <option :value="60">分钟</option>
          <option :value="3600">小时</option>
          <option :value="86400">天</option>
        </select>
        触发一次
      </label>
      <label>展示时区
        <select v-model="form.timezone">
          <option v-for="z in zones" :key="z" :value="z">{{ z }}</option>
        </select>
      </label>
    </div>

    <p class="hint" v-if="form.scheduleType === 'INTERVAL'">
      固定间隔锚定 UTC，夏令时只会改变它对应的本地挂钟时间。
    </p>
    <p class="hint" v-else>
      每日定点按所选时区的挂钟时间触发：夏令时拨快时该时刻可能被跳过，回拨时可能触发两次。
    </p>

    <button class="primary" @click="submit">创建任务</button>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>
