import request from '@/config/axios'

export interface DictionaryTable {
  id: string
  datasourceId: string
  datasourceName: string
  schemaName: string
  tableName: string
  displayName: string
  purpose: string
  sourceName: string
  subjectName: string
  remark: string
  fieldCount: number
  updatedAt: number
  matchedFields: string[]
}

export interface DictionaryField {
  fieldName: string
  dataType: string
  description: string
  primaryKey: boolean
  requiredField: boolean
  remark: string
  ordinal: number
}

export interface DictionaryPage {
  items: DictionaryTable[]
  total: number
  page: number
  size: number
}

export interface DictionaryDetail {
  table: DictionaryTable
  fields: DictionaryField[]
}

export interface DictionarySource {
  id: string
  name: string
  type: string
}

export interface DictionaryImport {
  importId: string
  tableCount: number
  fieldCount: number
}

export const searchDictionary = async (keyword: string, page = 0, size = 20) => {
  const response = await request.get({
    url: `/dictionary/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`
  })
  return response.data as DictionaryPage
}

export const suggestDictionary = async (keyword: string) => {
  const response = await request.get({
    url: `/dictionary/suggest?keyword=${encodeURIComponent(keyword)}`
  })
  return response.data as DictionaryPage
}

export const getDictionaryDetail = async (id: string) => {
  const response = await request.get({ url: `/dictionary/tables/${encodeURIComponent(id)}` })
  return response.data as DictionaryDetail
}

export const getDictionaryDdl = async (id: string) => {
  const response = await request.get({ url: `/dictionary/tables/${encodeURIComponent(id)}/ddl` })
  return response.data as string
}

export const dictionaryIsAdmin = async () => {
  const response = await request.get({ url: '/dictionary/admin' })
  return response.data as boolean
}

export const dictionarySources = async () => {
  const response = await request.get({ url: '/dictionary/sources' })
  return response.data as DictionarySource[]
}

export const uploadDictionary = async (datasourceId: string, file: File) => {
  const data = new FormData()
  data.append('datasourceId', datasourceId)
  data.append('file', file)
  const response = await request.post({
    url: '/dictionary/import',
    data,
    headersType: 'multipart/form-data',
    loading: true
  })
  return response.data as DictionaryImport
}
