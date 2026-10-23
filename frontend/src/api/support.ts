import api from '@/lib/axios'
import type { KnowledgeBaseSource } from './knowledge-bases'

export type SupportScenario = 'ECOMMERCE' | 'GAME' | null
export type SupportStatus = 'AI' | 'WAITING_HUMAN' | 'HUMAN_ACTIVE' | 'CLOSED'

export interface SupportMessage {
  id: string
  sender: 'USER' | 'AI' | 'AGENT'
  content: string
  sources: KnowledgeBaseSource[]
  createdAt: string
}

export interface SupportConversation {
  id: string
  ownerId: string
  scenario: SupportScenario
  status: SupportStatus
  messages: SupportMessage[]
  createdAt: string
  updatedAt: string
}

export async function listSupportConversations(): Promise<SupportConversation[]> {
  const response = await api.get<SupportConversation[]>('/support/conversations')
  return response.data
}

export async function sendSupportMessage(conversationId: string | null, content: string): Promise<SupportConversation> {
  const path = conversationId ? `/support/conversations/${conversationId}/messages` : '/support/conversations'
  const response = await api.post<SupportConversation>(path, { content })
  return response.data
}

export async function requestSupportHuman(id: string): Promise<SupportConversation> {
  const response = await api.post<SupportConversation>(`/support/conversations/${id}/handoff`)
  return response.data
}

export async function listAgentQueue(): Promise<SupportConversation[]> {
  const response = await api.get<SupportConversation[]>('/admin/support/conversations')
  return response.data
}

export async function agentReply(id: string, content: string): Promise<SupportConversation> {
  const response = await api.post<SupportConversation>(`/admin/support/conversations/${id}/reply`, { content })
  return response.data
}

export async function closeSupportConversation(id: string): Promise<SupportConversation> {
  const response = await api.post<SupportConversation>(`/admin/support/conversations/${id}/close`)
  return response.data
}
