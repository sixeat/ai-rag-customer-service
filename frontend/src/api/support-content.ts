import api from '@/lib/axios'

export type SupportContentCategory = 'ECOMMERCE_PROMOTION' | 'GAME_EVENT' | 'GAME_UPDATE'

export interface SupportContent {
  id: string
  materialId: string
  category: SupportContentCategory
  title: string
  body: string | null
  published: boolean
  startsAt: string | null
  endsAt: string | null
  textStatus: string
  updatedAt: string
}

export interface SupportContentInput {
  category: SupportContentCategory
  title: string
  body: string
  published: boolean
  startsAt: string
  endsAt: string
  file: File | null
}

export async function listSupportContent(): Promise<SupportContent[]> {
  const response = await api.get<SupportContent[]>('/admin/support-content')
  return response.data
}

export async function saveSupportContent(input: SupportContentInput, id?: string): Promise<SupportContent> {
  const form = new FormData()
  form.append('category', input.category)
  form.append('title', input.title.trim())
  form.append('published', String(input.published))
  if (input.startsAt) form.append('startsAt', input.startsAt)
  if (input.endsAt) form.append('endsAt', input.endsAt)
  if (input.file) {
    form.append('file', input.file)
    if (input.body.trim()) form.append('body', input.body.trim())
  } else if (!id && input.body.trim()) {
    form.append('body', input.body.trim())
    form.append('file', new File([input.body.trim()], `${input.title.trim()}.txt`, { type: 'text/plain' }))
  }
  const response = id
    ? await api.put<SupportContent>(`/admin/support-content/${id}`, form)
    : await api.post<SupportContent>('/admin/support-content', form)
  return response.data
}

export async function deleteSupportContent(id: string): Promise<void> {
  await api.delete(`/admin/support-content/${id}`)
}
