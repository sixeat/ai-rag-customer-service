import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Headset, Loader2, Send, X } from 'lucide-react'
import { useToast } from '@/components/ui/toast'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { agentReply, closeSupportConversation, listAgentQueue } from '@/api/support'
import { SupportConversationView } from '@/components/workspace/SupportConversationView'

export function SupportQueuePage() {
  const { showToast } = useToast()
  const { data: queue = [], refetch, isLoading } = useQuery({
    queryKey: ['support-agent-queue'], queryFn: listAgentQueue, refetchInterval: 5000,
  })
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [reply, setReply] = useState('')
  const [busy, setBusy] = useState(false)
  const selected = queue.find((item) => String(item.id) === selectedId) ?? queue[0]

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!selected || !reply.trim()) return
    setBusy(true)
    try {
      await agentReply(String(selected.id), reply.trim())
      setReply('')
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '回复失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  const close = async () => {
    if (!selected) return
    setBusy(true)
    try {
      await closeSupportConversation(String(selected.id))
      setSelectedId(null)
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '关闭失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  return <div className="mx-auto max-w-6xl space-y-5">
    <header className="flex items-center justify-between border-b pb-5"><h1 className="flex items-center gap-2 text-xl font-semibold"><Headset className="h-5 w-5" />人工客服</h1><span className="text-sm text-muted-foreground">{queue.length} 个待处理会话</span></header>
    <div className="grid gap-5 lg:grid-cols-[250px_minmax(0,1fr)]">
      <nav aria-label="待处理会话" className="space-y-1 border-b pb-4 lg:border-b-0 lg:border-r lg:pr-4">
        {isLoading && <Loader2 className="h-5 w-5 animate-spin" />}
        {queue.map((conversation) => <button key={conversation.id} type="button" onClick={() => setSelectedId(String(conversation.id))}
          className={`w-full rounded-md px-3 py-2 text-left text-sm ${String(selected?.id) === String(conversation.id) ? 'bg-emerald-50 dark:bg-emerald-950' : 'hover:bg-muted'}`}>
          <span className="block truncate">{conversation.messages[0]?.content || '客服会话'}</span>
          <span className="text-xs text-muted-foreground">用户 #{conversation.ownerId} · {conversation.status === 'WAITING_HUMAN' ? '等待回复' : '处理中'}</span>
        </button>)}
      </nav>
      {selected ? <div className="min-w-0 space-y-5">
        <div className="max-h-[50vh] overflow-y-auto border-b pb-5"><SupportConversationView conversation={selected} /></div>
        <form onSubmit={(event) => void submit(event)} className="space-y-2">
          <Textarea value={reply} onChange={(event) => setReply(event.target.value)} maxLength={6000} rows={3} aria-label="人工回复" placeholder="回复用户" />
          <div className="flex justify-between gap-2"><Button type="button" variant="outline" size="sm" onClick={() => void close()} disabled={busy}><X className="mr-1 h-4 w-4" />关闭会话</Button>
            <Button type="submit" size="sm" disabled={busy || !reply.trim()}>{busy ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Send className="mr-1 h-4 w-4" />}回复</Button></div>
        </form>
      </div> : <p className="py-16 text-center text-sm text-muted-foreground">当前没有待处理会话。</p>}
    </div>
  </div>
}
