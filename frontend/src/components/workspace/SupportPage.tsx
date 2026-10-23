import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Headset, Loader2, Plus, Send } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/components/ui/toast'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { listSupportConversations, requestSupportHuman, sendSupportMessage } from '@/api/support'
import { SupportConversationView } from './SupportConversationView'

export function SupportPage() {
  const { isAuthenticated } = useAuth()
  const { showToast } = useToast()
  const { data: conversations = [], refetch } = useQuery({
    queryKey: ['support-conversations'], queryFn: listSupportConversations,
    enabled: isAuthenticated, refetchInterval: 5000,
  })
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [newConversation, setNewConversation] = useState(false)
  const [content, setContent] = useState('')
  const [busy, setBusy] = useState(false)
  const selected = newConversation ? null : conversations.find((item) => String(item.id) === selectedId) ?? conversations[0]

  const send = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!content.trim()) return
    setBusy(true)
    try {
      const result = await sendSupportMessage(selected ? String(selected.id) : null, content.trim())
      setContent('')
      setNewConversation(false)
      setSelectedId(String(result.id))
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '发送失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  const handoff = async () => {
    setBusy(true)
    try {
      const result = selected
        ? await requestSupportHuman(String(selected.id))
        : await sendSupportMessage(null, '转人工')
      setNewConversation(false)
      setSelectedId(String(result.id))
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '转人工失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  if (!isAuthenticated) {
    return <div className="mx-auto max-w-4xl py-16 text-center"><p className="mb-4 text-sm text-muted-foreground">登录后可联系客服。</p><Button asChild><Link to="/login?redirect=/workspace/support">登录</Link></Button></div>
  }

  return <div className="mx-auto flex h-full max-w-6xl flex-col">
    <header className="flex items-center justify-between border-b px-4 py-4"><h1 className="flex items-center gap-2 text-lg font-semibold"><Headset className="h-5 w-5" />客服</h1>
      <Button size="sm" variant="outline" onClick={() => { setNewConversation(true); setSelectedId(null); setContent('') }}><Plus className="mr-1 h-4 w-4" />新会话</Button></header>
    <div className="grid min-h-0 flex-1 md:grid-cols-[220px_minmax(0,1fr)]">
      <nav aria-label="客服会话" className="flex gap-2 overflow-x-auto border-b p-3 md:flex-col md:overflow-y-auto md:border-b-0 md:border-r">
        {conversations.map((conversation) => <button key={conversation.id} type="button" onClick={() => { setNewConversation(false); setSelectedId(String(conversation.id)) }}
          className={`min-w-40 shrink-0 rounded-md px-3 py-2 text-left text-sm md:w-full ${String(selected?.id) === String(conversation.id) ? 'bg-emerald-50 dark:bg-emerald-950' : 'hover:bg-muted'}`}>
          <span className="block truncate">{conversation.messages[0]?.content || '客服会话'}</span>
          <span className="block text-xs text-muted-foreground">{conversation.status === 'WAITING_HUMAN' ? '等待人工' : conversation.status === 'HUMAN_ACTIVE' ? '人工处理中' : conversation.status === 'CLOSED' ? '已关闭' : '智能回复'}</span>
        </button>)}
      </nav>
      <div className="flex min-h-0 flex-col">
        <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6">
          {selected ? <SupportConversationView conversation={selected} /> : <p className="py-16 text-center text-sm text-muted-foreground">请在下方输入问题。</p>}
        </div>
        <div className="border-t p-3 sm:p-4">
          <form onSubmit={(event) => void send(event)} className="space-y-2">
            <Textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={6000} rows={2} aria-label="客服问题" placeholder="输入订单、售后、游戏账号或充值问题" disabled={selected?.status === 'CLOSED'} />
            <div className="flex items-center justify-between gap-2">
              <Button type="button" variant="outline" size="sm" onClick={() => void handoff()} disabled={busy || selected?.status === 'CLOSED' || selected?.status === 'WAITING_HUMAN' || selected?.status === 'HUMAN_ACTIVE'}><Headset className="mr-1 h-4 w-4" />转人工</Button>
              <Button type="submit" size="sm" disabled={busy || !content.trim() || selected?.status === 'CLOSED'}>{busy ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Send className="mr-1 h-4 w-4" />}发送</Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
}
