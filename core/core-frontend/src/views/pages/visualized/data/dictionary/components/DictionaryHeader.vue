<script setup lang="ts">
import { shallowRef, onMounted } from 'vue'
import { dictionaryIsAdmin } from '@/api/dictionary'
import { useI18n } from '@/hooks/web/useI18n'
import DictionaryImportDialog from './DictionaryImportDialog.vue'

const emit = defineEmits<{ imported: [] }>()
const { t } = useI18n()
const admin = shallowRef(false)
const importVisible = shallowRef(false)

onMounted(async () => {
  admin.value = await dictionaryIsAdmin()
})
</script>

<template>
  <div class="dictionary-header">
    <span class="dictionary-brand">{{ t('dictionary.title') }}</span>
    <el-button v-if="admin" @click="importVisible = true">{{ t('dictionary.upload') }}</el-button>
    <DictionaryImportDialog v-model="importVisible" @imported="emit('imported')" />
  </div>
</template>

<style scoped>
.dictionary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  border-bottom: 1px solid #eceff3;
}

.dictionary-brand {
  font-size: 18px;
  font-weight: 600;
  color: #182230;
}
</style>
