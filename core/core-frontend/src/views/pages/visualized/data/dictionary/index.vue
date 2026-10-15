<script setup lang="ts">
import { shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router_2'
import { suggestDictionary, type DictionaryTable } from '@/api/dictionary'
import { useI18n } from '@/hooks/web/useI18n'
import DictionaryHeader from './components/DictionaryHeader.vue'
import DictionarySearchBox from './components/DictionarySearchBox.vue'

const { t } = useI18n()
const router = useRouter()
const keyword = shallowRef('')
const suggestions = shallowRef<DictionaryTable[]>([])
const searching = shallowRef(false)

watch(keyword, (value, _previous, onCleanup) => {
  if (!value.trim()) {
    suggestions.value = []
    return
  }
  const timer = window.setTimeout(async () => {
    searching.value = true
    try {
      const result = await suggestDictionary(value.trim())
      if (keyword.value.trim() === value.trim()) suggestions.value = result.items
    } finally {
      searching.value = false
    }
  }, 280)
  onCleanup(() => window.clearTimeout(timer))
})

function search() {
  router.push({ path: '/data/dictionary-results', query: { q: keyword.value.trim() } })
}

function openTable(id: string) {
  router.push({ path: '/data/dictionary-detail', query: { id, q: keyword.value.trim() } })
}
</script>

<template>
  <div class="dictionary-page">
    <DictionaryHeader @imported="keyword = ''" />
    <main class="search-home">
      <div class="search-heading">
        <div class="heading-mark">D</div>
        <h1>{{ t('dictionary.title') }}</h1>
        <p>{{ t('dictionary.subtitle') }}</p>
      </div>
      <div class="search-area">
        <DictionarySearchBox v-model="keyword" @search="search" />
        <div v-if="keyword.trim() && (suggestions.length || searching)" class="suggestions">
          <button
            v-for="table in suggestions"
            :key="table.id"
            class="suggestion"
            type="button"
            @click="openTable(table.id)"
          >
            <span class="suggestion-source">{{ table.sourceName || table.datasourceName }}</span>
            <span class="suggestion-name">{{ table.schemaName }}.{{ table.tableName }}</span>
            <span class="suggestion-description">{{ table.displayName || table.purpose }}</span>
            <span v-if="table.matchedFields?.length" class="suggestion-description">{{
              table.matchedFields.join(', ')
            }}</span>
          </button>
          <button class="all-results" type="button" @click="search">
            {{ t('dictionary.results') }} →
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dictionary-page {
  min-height: 100%;
  background: #fff;
}

.search-home {
  width: min(760px, calc(100% - 48px));
  margin: clamp(80px, 15vh, 180px) auto 40px;
}

.search-heading {
  text-align: center;
  margin-bottom: 36px;
}

.heading-mark {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin: 0 auto 18px;
  border-radius: 16px;
  background: #e9f0ff;
  color: #3266d6;
  font-size: 31px;
  font-weight: 700;
}

.search-heading h1 {
  margin: 0;
  font-size: 34px;
  color: #182230;
}

.search-heading p {
  color: #667085;
  font-size: 15px;
}

.search-area {
  position: relative;
}

.suggestions {
  position: absolute;
  z-index: 10;
  top: 60px;
  left: 0;
  right: 104px;
  max-height: 390px;
  overflow: auto;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 30px #1018281a;
}

.suggestion,
.all-results {
  width: 100%;
  padding: 12px 16px;
  border: 0;
  border-bottom: 1px solid #f2f4f7;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.suggestion {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 4px 12px;
}

.suggestion:hover,
.all-results:hover {
  background: #f5f8ff;
}

.suggestion-source {
  grid-row: 1 / span 3;
  color: #3266d6;
  overflow-wrap: anywhere;
}

.suggestion-name {
  color: #182230;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.suggestion-description {
  color: #667085;
  overflow-wrap: anywhere;
}

.all-results {
  color: #3266d6;
  font-weight: 600;
}

@media (max-width: 650px) {
  .suggestion {
    grid-template-columns: 1fr;
  }

  .suggestion-source {
    grid-row: auto;
  }
}
</style>
