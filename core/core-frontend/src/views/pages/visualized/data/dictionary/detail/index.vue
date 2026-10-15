<script setup lang="ts">
import { shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router_2'
import { ElMessage } from 'element-plus-secondary'
import { getDictionaryDdl, getDictionaryDetail, type DictionaryDetail } from '@/api/dictionary'
import { useI18n } from '@/hooks/web/useI18n'
import DictionaryHeader from '../components/DictionaryHeader.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const detail = shallowRef<DictionaryDetail | null>(null)
const loading = shallowRef(false)
const ddlVisible = shallowRef(false)
const fieldsVisible = shallowRef(false)
const ddlLoading = shallowRef(false)
const ddl = shallowRef('')

watch(
  () => route.query.id,
  async (id, _old, onCleanup) => {
    if (!id) return
    let active = true
    onCleanup(() => {
      active = false
    })
    loading.value = true
    detail.value = null
    try {
      const result = await getDictionaryDetail(String(id))
      if (active) detail.value = result
    } finally {
      if (active) loading.value = false
    }
  },
  { immediate: true }
)

async function showDdl() {
  if (!detail.value) return
  ddlVisible.value = true
  ddlLoading.value = true
  ddl.value = ''
  try {
    ddl.value = await getDictionaryDdl(detail.value.table.id)
  } catch {
    ddl.value = ''
  } finally {
    ddlLoading.value = false
  }
}

async function copy(value: string) {
  if (navigator.clipboard) {
    await navigator.clipboard.writeText(value)
  } else {
    const input = document.createElement('textarea')
    input.value = value
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    input.remove()
  }
  ElMessage.success(t('dictionary.copied'))
}

function back() {
  router.push({ path: '/data/dictionary-results', query: { q: String(route.query.q || '') } })
}
</script>

<template>
  <div class="dictionary-page">
    <DictionaryHeader />
    <main v-loading="loading" class="detail-content">
      <el-button link class="back-button" @click="back"
        >← {{ t('dictionary.back_results') }}</el-button
      >
      <template v-if="detail">
        <div class="title-row">
          <div>
            <div class="source-label">
              {{ detail.table.sourceName || detail.table.datasourceName }}
            </div>
            <h1>{{ detail.table.schemaName }}.{{ detail.table.tableName }}</h1>
            <p class="display-name">
              {{ detail.table.displayName || detail.table.purpose || '—' }}
            </p>
          </div>
          <div class="detail-actions">
            <el-button @click="fieldsVisible = true">{{ t('dictionary.view_fields') }}</el-button>
            <el-button @click="showDdl">{{ t('dictionary.view_ddl') }}</el-button>
          </div>
        </div>
        <div class="meta-grid">
          <div>
            <span>{{ t('dictionary.source') }}</span
            ><strong>{{ detail.table.datasourceName }}</strong>
          </div>
          <div>
            <span>{{ t('dictionary.schema') }}</span
            ><strong>{{ detail.table.schemaName || '—' }}</strong>
          </div>
          <div>
            <span>{{ t('dictionary.purpose') }}</span
            ><strong>{{ detail.table.purpose || '—' }}</strong>
          </div>
          <div>
            <span>{{ t('dictionary.fields') }}</span
            ><strong>{{ detail.fields.length }}</strong>
          </div>
        </div>
      </template>
    </main>
    <el-drawer v-model="fieldsVisible" :title="t('dictionary.fields')" size="min(860px, 90vw)">
      <template v-if="detail">
        <el-table :data="detail.fields" stripe class="field-table">
          <el-table-column prop="fieldName" :label="t('dictionary.field_name')" min-width="190" />
          <el-table-column prop="dataType" :label="t('dictionary.field_type')" min-width="150" />
          <el-table-column
            prop="description"
            :label="t('dictionary.field_description')"
            min-width="220"
            show-overflow-tooltip
          />
          <el-table-column :label="t('dictionary.primary_key')" width="90">
            <template #default="scope">{{ scope.row.primaryKey ? 'Y' : '—' }}</template>
          </el-table-column>
          <el-table-column :label="t('dictionary.required')" width="90">
            <template #default="scope">{{ scope.row.requiredField ? 'Y' : 'N' }}</template>
          </el-table-column>
          <el-table-column
            prop="remark"
            :label="t('dictionary.remark')"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column width="75">
            <template #default="scope">
              <el-button link type="primary" @click="copy(scope.row.fieldName)">{{
                t('dictionary.copy')
              }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
    <el-dialog v-model="ddlVisible" :title="t('dictionary.view_ddl')" width="760px">
      <div v-loading="ddlLoading">
        <p v-if="!ddlLoading && !ddl" class="ddl-note">{{ t('dictionary.ddl_unavailable') }}</p>
        <template v-else-if="ddl">
          <p class="ddl-note">{{ t('dictionary.ddl_reference') }}</p>
          <pre class="ddl-code">{{ ddl }}</pre>
        </template>
      </div>
      <template #footer>
        <el-button v-if="ddl" @click="copy(ddl)">{{ t('dictionary.copy') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dictionary-page {
  min-height: 100%;
  background: #fff;
}
.detail-content {
  max-width: 1120px;
  min-height: 400px;
  margin: 32px auto;
  padding: 0 24px 48px;
}
.back-button {
  margin-bottom: 22px;
}
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}
.detail-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}
.source-label {
  color: #3266d6;
  font-weight: 600;
  margin-bottom: 9px;
}
.title-row h1 {
  font-size: 30px;
  margin: 0;
  color: #182230;
  overflow-wrap: anywhere;
}
.display-name {
  color: #667085;
  margin: 10px 0 24px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 4px 0 36px;
}
.meta-grid div {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  border: 1px solid #e7ebf0;
  border-radius: 10px;
}
.meta-grid span {
  color: #667085;
  font-size: 13px;
}
.meta-grid strong {
  color: #182230;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.detail-content h2 {
  font-size: 20px;
  margin-bottom: 14px;
}
.field-table {
  width: 100%;
}
.ddl-note {
  color: #667085;
}
.ddl-code {
  max-height: 480px;
  overflow: auto;
  padding: 20px;
  border-radius: 8px;
  background: #101828;
  color: #f2f4f7;
  white-space: pre-wrap;
}
@media (max-width: 800px) {
  .meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
