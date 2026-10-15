<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'

const keyword = defineModel<string>({ default: '' })
const emit = defineEmits<{ search: [] }>()
const { t } = useI18n()
</script>

<template>
  <div class="dictionary-search-box">
    <el-input
      v-model="keyword"
      clearable
      size="large"
      :placeholder="t('dictionary.search_placeholder')"
      @keyup.enter="emit('search')"
    />
    <el-button type="primary" size="large" @click="emit('search')">
      {{ t('dictionary.search') }}
    </el-button>
  </div>
</template>

<style scoped>
.dictionary-search-box {
  display: flex;
  width: 100%;
  gap: 12px;
}

.dictionary-search-box :deep(.el-input__wrapper) {
  border-radius: 8px;
  min-height: 46px;
}

.dictionary-search-box :deep(.el-button) {
  border-radius: 8px;
  min-width: 92px;
}
</style>
