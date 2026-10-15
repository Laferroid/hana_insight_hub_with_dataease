<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router_2'
import { searchDictionary, type DictionaryPage } from '@/api/dictionary'
import { useI18n } from '@/hooks/web/useI18n'
import DictionaryHeader from '../components/DictionaryHeader.vue'
import DictionarySearchBox from '../components/DictionarySearchBox.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const keyword = shallowRef('')
const submittedKeyword = computed(() => String(route.query.q || '').trim())
const page = shallowRef(1)
const pageSize = shallowRef(20)
const refreshVersion = shallowRef(0)
const loading = shallowRef(false)
const results = shallowRef<DictionaryPage>({ items: [], total: 0, page: 0, size: 20 })

watch(
  submittedKeyword,
  value => {
    keyword.value = value
    page.value = 1
  },
  { immediate: true }
)

watch(
  [submittedKeyword, page, pageSize, refreshVersion],
  async ([query, currentPage, size], _old, onCleanup) => {
    let active = true
    onCleanup(() => {
      active = false
    })
    loading.value = true
    results.value = { items: [], total: 0, page: currentPage - 1, size }
    try {
      const data = await searchDictionary(query, currentPage - 1, size)
      if (active) results.value = data
    } finally {
      if (active) loading.value = false
    }
  },
  { immediate: true }
)

function search() {
  const query = keyword.value.trim()
  if (query !== submittedKeyword.value) {
    router.push({ path: '/data/dictionary-results', query: { q: query } })
  } else if (page.value !== 1) {
    page.value = 1
  } else {
    refreshVersion.value++
  }
}

function openTable(id: string) {
  router.push({ path: '/data/dictionary-detail', query: { id, q: submittedKeyword.value } })
}

function refresh() {
  refreshVersion.value++
}

function changePageSize(size: number) {
  pageSize.value = size
  page.value = 1
}
</script>

<template>
  <div class="dictionary-page">
    <DictionaryHeader @imported="refresh" />
    <main class="results-content">
      <DictionarySearchBox v-model="keyword" @search="search" />
      <div class="results-heading">
        <h1>{{ t('dictionary.results') }}</h1>
        <span>{{ t('dictionary.result_count', { count: results.total }) }}</span>
      </div>
      <div v-loading="loading" class="result-list">
        <el-empty
          v-if="!loading && !results.items.length"
          :description="t('dictionary.no_result')"
        />
        <button
          v-for="table in results.items"
          :key="table.id"
          type="button"
          class="result-row"
          @click="openTable(table.id)"
        >
          <span class="result-source">{{ table.sourceName || table.datasourceName }}</span>
          <span class="result-title">{{ table.schemaName }}.{{ table.tableName }}</span>
          <span class="result-description">
            {{ table.displayName || table.purpose || '—' }}
            <small v-if="table.matchedFields?.length">{{ table.matchedFields.join(', ') }}</small>
          </span>
          <span class="result-meta">{{ t('dictionary.fields') }} {{ table.fieldCount }}</span>
          <span class="result-link">{{ t('dictionary.view_detail') }} →</span>
        </button>
      </div>
      <el-pagination
        v-if="results.total > 0"
        v-model:current-page="page"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[20, 50, 100]"
        :page-size="pageSize"
        :total="results.total"
        @size-change="changePageSize"
      />
    </main>
  </div>
</template>

<style scoped>
.dictionary-page {
  min-height: 100%;
  background: #fff;
}
.results-content {
  max-width: 920px;
  margin: 0 auto;
  padding: 44px 24px 56px;
}
.results-heading {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin: 32px 0 18px;
}
.results-heading h1 {
  font-size: 22px;
  margin: 0;
  color: #182230;
}
.results-heading span {
  color: #667085;
}
.result-list {
  min-height: 160px;
}
.result-row {
  display: grid;
  grid-template-columns: 130px minmax(0, 1.3fr) minmax(0, 1fr) 100px 110px;
  align-items: start;
  width: 100%;
  gap: 16px;
  padding: 20px 14px;
  border: 0;
  border-bottom: 1px solid #e9edf3;
  background: #fff;
  text-align: left;
  cursor: pointer;
}
.result-row:hover {
  background: #f7f9fd;
}
.result-source,
.result-title,
.result-description {
  min-width: 0;
  overflow-wrap: anywhere;
  white-space: normal;
}
.result-source {
  color: #3266d6;
}
.result-title {
  font-weight: 600;
  color: #182230;
}
.result-description {
  color: #475467;
}
.result-description small {
  display: block;
  margin-top: 5px;
  color: #667085;
  overflow-wrap: anywhere;
}
.result-meta {
  color: #667085;
}
.result-link {
  color: #3266d6;
}
.results-content :deep(.el-pagination) {
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 24px;
}
@media (max-width: 850px) {
  .result-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
