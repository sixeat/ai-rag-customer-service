import type { SupportConversation } from '@/api/support'

const scenarioLabel = { ECOMMERCE: '电商客服', GAME: '游戏客服' }
const statusLabel = { AI: '智能回复', WAITING_HUMAN: '等待人工', HUMAN_ACTIVE: '人工处理中', CLOSED: '已关闭' }

export function SupportConversationView({ conversation }: { conversation: SupportConversation }) {
  return <div className="space-y-4" aria-live="polite">
    <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
      {conversation.scenario && <span>{scenarioLabel[conversation.scenario]}</span>}
      <span>{statusLabel[conversation.status]}</span>
    </div>
    {conversation.messages.map((message) => <div key={message.id}
      className={`max-w-[92%] space-y-2 rounded-md border p-3 text-sm sm:max-w-[85%] ${message.sender === 'USER' ? 'ml-auto border-emerald-200 bg-emerald-50 dark:border-emerald-900 dark:bg-emerald-950' : 'bg-background'}`}>
      <p className="text-xs font-medium text-muted-foreground">{message.sender === 'USER' ? '我' : message.sender === 'AGENT' ? '人工客服' : '智能客服'}</p>
      <p className="whitespace-pre-wrap break-words leading-6">{message.content}</p>
      {message.sources?.length > 0 && <div className="space-y-2 border-t pt-2"><p className="text-xs font-medium text-muted-foreground">资料来源</p>
        {message.sources.map((source) => <div key={source.chunkId} className="text-xs">
          <p className="font-medium">{source.materialTitle}{source.pageNo ? ` · 第 ${source.pageNo} 页` : ''}</p>
          <p className="line-clamp-3 text-muted-foreground">{source.excerpt}</p>
        </div>)}
      </div>}
    </div>)}
  </div>
}
