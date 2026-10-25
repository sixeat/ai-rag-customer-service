import api from '@/lib/axios'
import type { KnowledgeBaseSource } from './knowledge-bases'

export type SupportScenario = 'ECOMMERCE' | 'GAME'
export type SupportEvaluationStatus = 'PASS' | 'FAIL' | 'SKIPPED'

export interface SupportDiagnostic {
  question: string
  scenario: SupportScenario | null
  outcome: string
  answer: string
  sources: KnowledgeBaseSource[]
  activeMaterials: number
  candidateChunks: number
  contentFilters: Record<string, number>
  difficulty: 'LOW' | 'HIGH' | null
  requestedTier: 'SMALL' | 'FLAGSHIP' | null
  actualTier: 'SMALL' | 'FLAGSHIP' | null
  modelName: string | null
  fallback: boolean
  totalTokens: number | null
  elapsedMillis: number
}

export interface SupportEvaluationCase {
  id: string
  group: string
  question: string
  expectedScenario: SupportScenario | null
  expectedOutcome: string
  expectedDifficulty: 'LOW' | 'HIGH' | null
  requiredTitle: string | null
  expectedSourceTitle: string | null
  mustContain: string | null
}

export interface SupportEvaluationResult {
  testCase: SupportEvaluationCase
  status: SupportEvaluationStatus
  findings: string[]
  diagnostic: SupportDiagnostic | null
}

export async function diagnoseSupport(question: string, scenario: SupportScenario | null): Promise<SupportDiagnostic> {
  const response = await api.post<SupportDiagnostic>('/admin/support/diagnose', { question, scenario })
  return response.data
}

export async function listSupportEvaluationCases(): Promise<SupportEvaluationCase[]> {
  const response = await api.get<SupportEvaluationCase[]>('/admin/support/evaluation/cases')
  return response.data
}

export async function runSupportEvaluationCase(id: string): Promise<SupportEvaluationResult> {
  const response = await api.post<SupportEvaluationResult>(`/admin/support/evaluation/cases/${id}/run`)
  return response.data
}
