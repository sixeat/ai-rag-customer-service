import api from '@/lib/axios'

export interface KnowledgeBase {
  id: string
  name: string
  description: string | null
  scenario: 'GENERAL' | 'ECOMMERCE' | 'GAME'
  published: boolean
  materialIds: string[]
  createdAt: string
  updatedAt: string
}

export interface KnowledgeBaseSource {
  materialId: string
  chunkId: string
  materialTitle: string
  pageNo: number | null
  excerpt: string
  score: number
}

export interface KnowledgeBaseAnswer {
  answer: string
  modelName: string | null
  sources: KnowledgeBaseSource[]
}

export async function listKnowledgeBases(): Promise<KnowledgeBase[]> {
  const response = await api.get<KnowledgeBase[]>('/knowledge-bases')
  return response.data
}

export async function createKnowledgeBase(name: string, description: string, scenario: KnowledgeBase['scenario']): Promise<KnowledgeBase> {
  const response = await api.post<KnowledgeBase>('/knowledge-bases', { name, description, scenario })
  return response.data
}

export async function updateKnowledgeBase(id: string, name: string, description: string, scenario: KnowledgeBase['scenario']): Promise<KnowledgeBase> {
  const response = await api.put<KnowledgeBase>(`/knowledge-bases/${id}`, { name, description, scenario })
  return response.data
}

export async function deleteKnowledgeBase(id: string): Promise<void> {
  await api.delete(`/knowledge-bases/${id}`)
}

export async function setKnowledgeBaseMaterial(id: string, materialId: string, selected: boolean): Promise<void> {
  if (selected) await api.post(`/knowledge-bases/${id}/materials/${materialId}`)
  else await api.delete(`/knowledge-bases/${id}/materials/${materialId}`)
}

export async function askKnowledgeBase(id: string, question: string): Promise<KnowledgeBaseAnswer> {
  const response = await api.post<KnowledgeBaseAnswer>(`/knowledge-bases/${id}/ask`, { question })
  return response.data
}

export async function publishKnowledgeBase(id: string, published: boolean): Promise<KnowledgeBase> {
  const response = await api.put<KnowledgeBase>(`/admin/knowledge-bases/${id}/publish`, null, { params: { published } })
  return response.data
}
