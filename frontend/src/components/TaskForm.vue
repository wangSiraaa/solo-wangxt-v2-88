<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import type { FormModel } from '../types'

const props = defineProps<{
  model: FormModel
  zones: string[]
  busy: boolean
  editId: number | null
}>()

const emit = defineEmits<{
  (e: 'update', patch: Partial<FormModel>): void
  (e: 'preview'): void
  (e: 'save'): void
  (e: 'cancel-edit'): void
}>()

const anchorLocal = ref('')

function syncLocalFromUtc() {
  if (props.model.anchorUtc) {
    const d = new Date(props.model.anchorUtc)
    const pad = (n: number) => String(n).padStart(2, '0')
    anchorLocal.value =
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
      `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
}

watch(() => props.model.anchorUtc, syncLocalFromUtc)

function onLocalInput() {
  if (anchorLocal.value) {
    emit('update', { anchorUtc: new Date(anchorLocal.value).toISOString() })
  }
}

onMounted(syncLocalFromUtc)
</script>

<template>
  <section class="card">
    <h2>{{ editId ? `编辑任务 #${editId}` : '新建周期任务' }}</h2>
    <div class="row" style="margin-bottom: 12px">
      <div class="field" style="flex: 2 1 220px">
        <label>任务名称</label>
        <input
          :value="model.name"
          placeholder="例如：每日对账"
          @input="emit('update', { name: ($event.target as HTMLInputElement).value })"
        />
      </div>
      <div class="field" style="flex: 1 1 160px">
        <label>规则类型</label>
        <select
          :value="model.kind"
          @change="emit('update', { kind: ($event.target as HTMLSelectElement).value as FormModel['kind'] })"
        >
          <option value="DAILY_LOCAL">每日本地时间（带时区）</option>
          <option value="FIXED_INTERVAL">固定间隔（锚定 UTC）</option>
        </select>
      </div>
      <div class="field" style="flex: 0 0 110px">
        <label>实例数量</label>
        <input
          type="number"
          min="2"
          max="60"
          :value="model.limit"
          @input="emit('update', { limit: Number(($event.target as HTMLInputElement).value) })"
        />
      </div>
    </div>

    <div class="row" style="margin-bottom: 12px">
      <template v-if="model.kind === 'DAILY_LOCAL'">
        <div class="field">
          <label>时（当地墙上时间）</label>
          <input
            type="number"
            min="0"
            max="23"
            :value="model.hour"
            @input="emit('update', { hour: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="field">
          <label>分</label>
          <input
            type="number"
            min="0"
            max="59"
            :value="model.minute"
            @input="emit('update', { minute: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="field" style="flex: 1 1 220px">
          <label>时区</label>
          <select
            :value="model.zoneId"
            @change="emit('update', { zoneId: ($event.target as HTMLSelectElement).value })"
          >
            <option v-for="z in zones" :key="z" :value="z">{{ z }}</option>
          </select>
        </div>
      </template>
      <template v-else>
        <div class="field" style="flex: 1 1 220px">
          <label>固定间隔（秒；86400 = 24 小时）</label>
          <input
            type="number"
            min="60"
            step="60"
            :value="model.intervalSeconds"
            @input="emit('update', { intervalSeconds: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="field" style="flex: 1 1 220px">
          <label>本地显示时区（用于 UTC 对照，仅影响标注）</label>
          <select
            :value="model.viewZoneId"
            @change="emit('update', { viewZoneId: ($event.target as HTMLSelectElement).value })"
          >
            <option v-for="z in zones" :key="z" :value="z">{{ z }}</option>
          </select>
        </div>
      </template>
    </div>

    <div class="row" style="align-items: flex-end">
      <div class="field" style="flex: 1 1 240px">
        <label>起始锚点（浏览器本地时间，提交为 UTC 瞬时）</label>
        <input v-model="anchorLocal" type="datetime-local" step="60" @change="onLocalInput" />
        <span class="muted mono">UTC: {{ model.anchorUtc }}</span>
      </div>
      <div style="display: flex; gap: 8px">
        <button :disabled="busy" @click.prevent="emit('preview')">预览未来实例</button>
        <button class="secondary" :disabled="busy" @click.prevent="emit('save')">
          {{ editId ? '保存修改并重建' : '保存任务并生成实例' }}
        </button>
        <button v-if="editId" class="secondary" @click.prevent="emit('cancel-edit')">取消编辑</button>
      </div>
    </div>
  </section>
</template>
