import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { FileUp, Loader2, Pencil, Plus, Trash2 } from 'lucide-react'
import { useToast } from '@/components/ui/toast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { deleteSupportContent, listSupportContent, saveSupportContent,
  type SupportContent, type SupportContentCategory, type SupportContentInput } from '@/api/support-content'

const categoryLabel: Record<SupportContentCategory, string> = {
  ECOMMERCE_PROMOTION: '电商优惠', GAME_EVENT: '游戏活动', GAME_UPDATE: '版本更新',
}
const emptyInput: SupportContentInput = {
  category: 'ECOMMERCE_PROMOTION', title: '', body: '', published: false,
  startsAt: '', endsAt: '', file: null,
}

function localDateTime(value: string | null): string {
  return value ? value.slice(0, 16) : ''
}

function availability(item: SupportContent): string {
  if (!item.published) return '已下架'
  if (item.textStatus !== 'READY' && item.textStatus !== 'PARTIAL') return item.textStatus === 'FAILED' ? '解析失败' : '处理中'
  const now = new Date()
  if (item.startsAt && new Date(item.startsAt) > now) return '待生效'
  if (item.endsAt && new Date(item.endsAt) <= now) return '已过期'
  return '生效中'
}

export function SupportContentPage() {
  const { showToast } = useToast()
  const { data: items = [], refetch, isLoading } = useQuery({
    queryKey: ['admin-support-content'], queryFn: listSupportContent, refetchInterval: 10000,
  })
  const [editing, setEditing] = useState<SupportContent | null>(null)
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState<SupportContentInput>(emptyInput)
  const [busy, setBusy] = useState(false)

  const begin = (item?: SupportContent) => {
    setEditing(item ?? null)
    setInput(item ? {
      category: item.category, title: item.title, body: item.body ?? '', published: item.published,
      startsAt: localDateTime(item.startsAt), endsAt: localDateTime(item.endsAt), file: null,
    } : { ...emptyInput })
    setOpen(true)
  }

  const save = async () => {
    if (!input.title.trim() || (!editing && !input.file && !input.body.trim())) return
    setBusy(true)
    try {
      const changedBody = editing && input.body.trim() && input.body.trim() !== (editing.body ?? '').trim()
      const file = changedBody && !input.file
        ? new File([input.body.trim()], `${input.title.trim()}.txt`, { type: 'text/plain' }) : input.file
      await saveSupportContent({ ...input, file }, editing ? String(editing.id) : undefined)
      setOpen(false)
      await refetch()
      showToast('客服知识已保存', 2500)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '保存失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (item: SupportContent) => {
    if (!window.confirm(`删除“${item.title}”？`)) return
    setBusy(true)
    try {
      await deleteSupportContent(String(item.id))
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '删除失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  const toggle = async (item: SupportContent) => {
    setBusy(true)
    try {
      await saveSupportContent({
        category: item.category, title: item.title, body: item.body ?? '',
        startsAt: localDateTime(item.startsAt), endsAt: localDateTime(item.endsAt),
        published: !item.published, file: null,
      }, String(item.id))
      await refetch()
    } catch (error) {
      showToast(error instanceof Error ? error.message : '更新失败', 3000)
    } finally {
      setBusy(false)
    }
  }

  return <div className="mx-auto max-w-6xl space-y-5">
    <header className="flex flex-wrap items-center justify-between gap-3 border-b pb-5">
      <div><h1 className="text-xl font-semibold">客服知识</h1><p className="mt-1 text-sm text-muted-foreground">优惠活动、游戏活动和版本更新</p></div>
      <Button onClick={() => begin()}><Plus className="mr-2 h-4 w-4" />新增内容</Button>
    </header>
    {isLoading && <Loader2 className="h-5 w-5 animate-spin" />}
    <div className="divide-y border-y">
      {items.map((item) => <div key={item.id} className="flex flex-wrap items-center gap-3 py-3 text-sm">
        <div className="min-w-0 flex-1"><p className="truncate font-medium">{item.title}</p><p className="mt-1 text-xs text-muted-foreground">{categoryLabel[item.category]} · {availability(item)}</p></div>
        <label className="flex items-center gap-2 text-xs"><input type="checkbox" className="h-4 w-4 accent-emerald-700" checked={item.published} disabled={busy} onChange={() => void toggle(item)} />发布</label>
        <Button variant="ghost" size="icon" title="编辑" aria-label={`编辑 ${item.title}`} onClick={() => begin(item)}><Pencil className="h-4 w-4" /></Button>
        <Button variant="ghost" size="icon" title="删除" aria-label={`删除 ${item.title}`} disabled={busy} onClick={() => void remove(item)}><Trash2 className="h-4 w-4" /></Button>
      </div>)}
      {!isLoading && items.length === 0 && <p className="py-12 text-center text-sm text-muted-foreground">暂无客服知识。</p>}
    </div>
    <Dialog open={open} onOpenChange={setOpen}><DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
      <DialogHeader><DialogTitle>{editing ? '编辑客服知识' : '新增客服知识'}</DialogTitle></DialogHeader>
      <div className="space-y-4">
        <select aria-label="内容类型" value={input.category} onChange={(event) => setInput({ ...input, category: event.target.value as SupportContentCategory })} className="h-10 w-full rounded-md border bg-background px-3 text-sm">
          {Object.entries(categoryLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
        <Input aria-label="标题" placeholder="标题" maxLength={128} value={input.title} onChange={(event) => setInput({ ...input, title: event.target.value })} />
        <Textarea aria-label="正文" placeholder="活动规则或版本更新内容" rows={7} maxLength={100000} value={input.body} disabled={Boolean(input.file)} onChange={(event) => setInput({ ...input, body: event.target.value })} />
        <label className="flex cursor-pointer items-center gap-2 text-sm"><FileUp className="h-4 w-4" />
          <span className="min-w-0 truncate">{input.file?.name ?? (editing ? '替换资料文件（可选）' : '上传资料文件（或填写正文）')}</span>
          <input type="file" className="sr-only" accept=".txt,.md,.pdf,.docx,.pptx,.html" onChange={(event) => setInput({ ...input, file: event.target.files?.[0] ?? null, body: '' })} />
        </label>
        <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs text-muted-foreground">开始时间<Input type="datetime-local" className="mt-1" value={input.startsAt} onChange={(event) => setInput({ ...input, startsAt: event.target.value })} /></label>
          <label className="text-xs text-muted-foreground">结束时间<Input type="datetime-local" className="mt-1" value={input.endsAt} onChange={(event) => setInput({ ...input, endsAt: event.target.value })} /></label></div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 accent-emerald-700" checked={input.published} onChange={(event) => setInput({ ...input, published: event.target.checked })} />发布到客服问答</label>
      </div>
      <DialogFooter><Button variant="outline" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => void save()} disabled={busy || !input.title.trim() || (!editing && !input.file && !input.body.trim())}>{busy && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}保存</Button></DialogFooter>
    </DialogContent></Dialog>
  </div>
}
