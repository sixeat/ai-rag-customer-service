import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, Loader2, MessageSquare, Pencil, Plus, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/components/ui/toast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { useMaterials } from '@/api/materials'
import {
  askKnowledgeBase, createKnowledgeBase, deleteKnowledgeBase, listKnowledgeBases,
  publishKnowledgeBase, setKnowledgeBaseMaterial, updateKnowledgeBase,
  type KnowledgeBaseAnswer,
  type KnowledgeBase,
} from '@/api/knowledge-bases'

function errorText(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败'
}

export function KnowledgeBasesPage() {
  const { isAuthenticated, isAdmin } = useAuth()
  const { showToast } = useToast()
  const { data: bases = [], refetch, isLoading } = useQuery({
    queryKey: ['knowledge-bases'], queryFn: listKnowledgeBases, enabled: isAuthenticated,
  })
  const { data: materials = [] } = useMaterials()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selected = bases.find((base) => String(base.id) === selectedId) ?? bases[0]
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [scenario, setScenario] = useState<KnowledgeBase['scenario']>('GENERAL')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState<KnowledgeBaseAnswer | null>(null)
  const [busy, setBusy] = useState(false)
  const [asking, setAsking] = useState(false)

  const openCreate = () => {
    setEditingId(null)
    setName('')
    setDescription('')
    setScenario('GENERAL')
    setDialogOpen(true)
  }

  const openEdit = () => {
    if (!selected) return
    setEditingId(String(selected.id))
    setName(selected.name)
    setDescription(selected.description ?? '')
    setScenario(selected.scenario)
    setDialogOpen(true)
  }

  const save = async () => {
    if (!name.trim()) return
    setBusy(true)
    try {
      const saved = editingId
        ? await updateKnowledgeBase(editingId, name.trim(), description.trim(), scenario)
        : await createKnowledgeBase(name.trim(), description.trim(), scenario)
      await refetch()
      setSelectedId(String(saved.id))
      setAnswer(null)
      setDialogOpen(false)
    } catch (error) {
      showToast(errorText(error), 3000)
    } finally {
      setBusy(false)
    }
  }

  const remove = async () => {
    if (!selected || !window.confirm(`删除知识库“${selected.name}”？资料文件仍会保留。`)) return
    setBusy(true)
    try {
      await deleteKnowledgeBase(String(selected.id))
      setSelectedId(null)
      setAnswer(null)
      await refetch()
    } catch (error) {
      showToast(errorText(error), 3000)
    } finally {
      setBusy(false)
    }
  }

  const togglePublished = async () => {
    if (!selected) return
    setBusy(true)
    try {
      await publishKnowledgeBase(String(selected.id), !selected.published)
      await refetch()
    } catch (error) {
      showToast(errorText(error), 3000)
    } finally {
      setBusy(false)
    }
  }

  const toggleMaterial = async (materialId: string, checked: boolean) => {
    if (!selected) return
    setBusy(true)
    try {
      await setKnowledgeBaseMaterial(String(selected.id), materialId, checked)
      await refetch()
      setAnswer(null)
    } catch (error) {
      showToast(errorText(error), 3000)
    } finally {
      setBusy(false)
    }
  }

  const ask = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!selected || !question.trim()) return
    setAsking(true)
    setAnswer(null)
    try {
      setAnswer(await askKnowledgeBase(String(selected.id), question.trim()))
    } catch (error) {
      showToast(errorText(error), 3000)
    } finally {
      setAsking(false)
    }
  }

  if (!isAuthenticated) {
    return <div className="mx-auto max-w-4xl py-16 text-center"><p className="mb-4 text-sm text-muted-foreground">登录后可创建知识库。</p><Button asChild><Link to="/login?redirect=/workspace/knowledge-bases">登录</Link></Button></div>
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b pb-5">
        <div><h1 className="text-xl font-semibold">知识库问答</h1><p className="mt-1 text-sm text-muted-foreground">{bases.length} 个知识库</p></div>
        <Button onClick={openCreate}><Plus className="mr-2 h-4 w-4" />新建知识库</Button>
      </header>

      <div className="grid gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
        <nav aria-label="知识库列表" className="min-w-0 border-b pb-4 lg:border-b-0 lg:border-r lg:pr-5">
          {isLoading && <Loader2 className="h-5 w-5 animate-spin" />}
          <div className="flex gap-2 overflow-x-auto lg:flex-col">
            {bases.map((base) => (
              <button key={base.id} type="button" onClick={() => { setSelectedId(String(base.id)); setAnswer(null); setQuestion('') }}
                className={`min-w-44 max-w-full shrink-0 rounded-md px-3 py-2 text-left text-sm lg:w-full ${String(selected?.id) === String(base.id) ? 'bg-emerald-50 font-medium text-emerald-900 dark:bg-emerald-950 dark:text-emerald-100' : 'hover:bg-muted'}`}>
                <span className="block truncate">{base.name}</span>
                <span className="mt-0.5 block text-xs text-muted-foreground">{base.scenario === 'GENERAL' ? '通用' : base.scenario === 'ECOMMERCE' ? '电商客服' : '游戏客服'} · {base.materialIds.length} 份资料{base.published ? ' · 已发布' : ''}</span>
              </button>
            ))}
          </div>
        </nav>

        {selected ? <div className="min-w-0 space-y-7">
          <section>
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0"><h2 className="break-words text-lg font-semibold">{selected.name}</h2><p className="mt-1 text-xs text-muted-foreground">{selected.scenario === 'GENERAL' ? '通用知识库' : selected.scenario === 'ECOMMERCE' ? '电商客服' : '游戏客服'}{selected.published ? ' · 已发布' : ''}</p>{selected.description && <p className="mt-1 break-words text-sm text-muted-foreground">{selected.description}</p>}</div>
              <div className="flex shrink-0 gap-1">
                <Button variant="ghost" size="icon" title="编辑知识库" aria-label="编辑知识库" onClick={openEdit}><Pencil className="h-4 w-4" /></Button>
                <Button variant="ghost" size="icon" title="删除知识库" aria-label="删除知识库" onClick={remove} disabled={busy}><Trash2 className="h-4 w-4" /></Button>
              </div>
            </div>
          </section>

          {isAdmin && selected.scenario !== 'GENERAL' && <div className="flex items-center gap-3"><label className="flex cursor-pointer items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 accent-emerald-700" checked={selected.published} disabled={busy} onChange={() => void togglePublished()} />发布到客服</label></div>}

          <section className="space-y-3 border-t pt-5">
            <div className="flex items-center justify-between"><h3 className="text-sm font-semibold">关联资料</h3><Link to="/workspace/materials" className="text-xs text-emerald-700 hover:underline dark:text-emerald-400">管理资料</Link></div>
            {materials.length === 0 ? <p className="text-sm text-muted-foreground">暂无资料。先上传资料再关联到知识库。</p> :
              <div className="grid gap-2 sm:grid-cols-2">
                {materials.map((material) => <label key={material.id} className="flex min-w-0 cursor-pointer items-center gap-3 rounded-md border px-3 py-2 text-sm">
                  <input type="checkbox" className="h-4 w-4 accent-emerald-700" checked={selected.materialIds.some((id) => String(id) === String(material.id))} disabled={busy}
                    onChange={(event) => void toggleMaterial(String(material.id), event.target.checked)} />
                  <BookOpen className="h-4 w-4 shrink-0 text-muted-foreground" /><span className="min-w-0 flex-1 truncate">{material.title}</span>
                  <span className="shrink-0 text-xs text-muted-foreground">{material.textStatus === 'READY' || material.textStatus === 'PARTIAL' ? '可检索' : '处理中'}</span>
                </label>)}
              </div>}
          </section>

          <section className="space-y-4 border-t pt-5">
            <h3 className="flex items-center gap-2 text-sm font-semibold"><MessageSquare className="h-4 w-4" />提问</h3>
            <form onSubmit={(event) => void ask(event)} className="flex flex-col gap-2 sm:flex-row">
              <Input value={question} onChange={(event) => setQuestion(event.target.value)} maxLength={6000} placeholder="输入关于知识库资料的问题" aria-label="问题" className="min-w-0 flex-1" />
              <Button type="submit" disabled={asking || !question.trim() || selected.materialIds.length === 0}>{asking && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}提问</Button>
            </form>
            {answer && <div className="space-y-4 border-l-2 border-emerald-600 pl-4" aria-live="polite">
              <p className="whitespace-pre-wrap break-words text-sm leading-7">{answer.answer}</p>
              {answer.sources.length > 0 && <div className="space-y-2 border-t pt-3"><h4 className="text-xs font-semibold text-muted-foreground">资料来源</h4>
                {answer.sources.map((source) => <Link key={source.chunkId} to={`/workspace/reader?materialId=${source.materialId}&chunkId=${source.chunkId}`}
                  className="block rounded border p-2 text-xs hover:border-emerald-500">
                  <span className="font-medium">{source.materialTitle}{source.pageNo ? ` · 第 ${source.pageNo} 页` : ''}</span>
                  <span className="mt-1 block line-clamp-2 text-muted-foreground">{source.excerpt}</span>
                </Link>)}
              </div>}
            </div>}
          </section>
        </div> : <div className="py-16 text-center text-sm text-muted-foreground">新建知识库，关联资料后即可提问。</div>}
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}><DialogContent className="sm:max-w-md"><DialogHeader><DialogTitle>{editingId ? '编辑知识库' : '新建知识库'}</DialogTitle></DialogHeader>
        <div className="space-y-3"><Input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} placeholder="知识库名称" aria-label="知识库名称" />
          <select value={scenario} onChange={(event) => setScenario(event.target.value as KnowledgeBase['scenario'])} disabled={editingId != null && selected?.published}
            aria-label="知识库场景" className="h-10 w-full rounded-md border bg-background px-3 text-sm">
            <option value="GENERAL">通用知识库</option><option value="ECOMMERCE">电商客服</option><option value="GAME">游戏客服</option>
          </select>
          <Textarea value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} placeholder="描述（可选）" aria-label="知识库描述" /></div>
        <DialogFooter><Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button><Button onClick={() => void save()} disabled={busy || !name.trim()}>{busy && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}保存</Button></DialogFooter>
      </DialogContent></Dialog>
    </div>
  )
}
