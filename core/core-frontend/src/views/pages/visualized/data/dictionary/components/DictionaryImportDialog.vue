<script setup lang="ts">
import { shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { dictionarySources, uploadDictionary, type DictionarySource } from '@/api/dictionary'
import { useI18n } from '@/hooks/web/useI18n'

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ imported: [] }>()
const { t } = useI18n()
const sources = shallowRef<DictionarySource[]>([])
const datasourceId = shallowRef('')
const file = shallowRef<File | null>(null)
const loading = shallowRef(false)
const input = shallowRef<HTMLInputElement | null>(null)

watch(visible, async open => {
  if (!open) return
  datasourceId.value = ''
  file.value = null
  sources.value = await dictionarySources()
})

function selectFile(event: Event) {
  const target = event.target as HTMLInputElement
  file.value = target.files?.[0] ?? null
}

async function submit() {
  if (!datasourceId.value || !file.value) return
  loading.value = true
  try {
    const result = await uploadDictionary(datasourceId.value, file.value)
    ElMessage.success(
      t('dictionary.import_success', { tables: result.tableCount, fields: result.fieldCount })
    )
    visible.value = false
    emit('imported')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="t('dictionary.upload_title')" width="540px">
    <div class="import-form">
      <p class="import-hint">{{ t('dictionary.import_hint') }}</p>
      <el-select
        v-model="datasourceId"
        filterable
        class="full-width"
        :placeholder="t('dictionary.select_source')"
      >
        <el-option
          v-for="source in sources"
          :key="source.id"
          :label="source.name"
          :value="source.id"
        />
      </el-select>
      <input ref="input" class="file-input" type="file" accept=".xlsx" @change="selectFile" />
      <el-button class="file-button" @click="input?.click()">
        {{ file?.name || t('dictionary.select_file') }}
      </el-button>
    </div>
    <template #footer>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!datasourceId || !file"
        @click="submit"
      >
        {{ t('dictionary.import') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-hint {
  color: #667085;
  margin: 0;
  line-height: 1.6;
}

.full-width,
.file-button {
  width: 100%;
}

.file-input {
  display: none;
}
</style>
