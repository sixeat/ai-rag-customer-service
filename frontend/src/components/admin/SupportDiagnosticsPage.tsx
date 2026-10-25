import { useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ChartNoAxesCombined, Loader2, Play, Search, Square } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { useToast } from '@/components/ui/toast'
import {
  diagnoseSupport, listSupportEvaluationCases, runSupportEvaluationCase,
  type SupportDiagnostic, type SupportEvaluationResult, type SupportScenario,
} from '@/api/support-diagnostics'

const outcomeLabels: Record<string, string> = {
  ANSWERED: '已回答', EXPIRED_NOTICE: '过期提醒', NO_MATERIALS: '无可用资料',
  NO_CHUNKS: '资料未解析', NO_MATCH: '无相关依据',
  MODEL_UNAVAILABLE: '模型不可用', NEEDS_SCENARIO: '需要补充场景',
  HANDOFF_REQUESTED: '主动转人工',
}
const filterLabels: Record<string, string> = {
  ACTIVE: '生效中', DRAFT: '未发布', FUTURE: '待生效', EXPIRED: '已过期', NOT_READY: '未解析', RETIRED: '旧稿停用',
}
const tierLabels: Record<string, string> = { SMALL: '小模型', FLAGSHIP: '旗舰模型' }
const statusLabels: Record<string, string> = { PASS: '通过', FAIL: '未通过', SKIPPED: '缺少样例' }

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="rounded-lg border bg-background p-3">
    <p className="text-xs text-muted-foreground">{label}</p>
    <p className="mt-1 text-lg font-semibold">{value}</p>
  </div>
}

function DiagnosticDetails({ result }: { result: SupportDiagnostic }) {
  return <div className="space-y-4 rounded-lg border p-4">
    <div className="flex flex-wrap items-center gap-2">
      <h3 className="font-semibold">诊断结果</h3>
      <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">
        {outcomeLabels[result.outcome] ?? result.outcome}
      </span>
      {result.fallback && <span className="rounded-full bg-amber-50 px-2 py-1 text-xs text-amber-800">小模型失败后回退</span>}
    </div>
    <div className="grid gap-2 sm:grid-cols-3 lg:grid-cols-6">
      <Metric label="分流场景" value={result.scenario === 'GAME' ? '游戏' : result.scenario === 'ECOMMERCE' ? '电商' : '未确定'} />
      <Metric label="问题难度" value={result.difficulty === 'LOW' ? '低' : result.difficulty === 'HIGH' ? '高' : '—'} />
      <Metric label="实际模型" value={result.modelName || '未调用'} />
      <Metric label="总耗时" value={`${result.elapsedMillis} ms`} />
      <Metric label="Token" value={result.totalTokens ?? '—'} />
      <Metric label="命中片段" value={result.sources.length} />
    </div>
    <p className="text-xs text-muted-foreground">
      活跃资料 {result.activeMaterials} 份 · 候选片段 {result.candidateChunks} 条
      {result.requestedTier && ` · 计划 ${tierLabels[result.requestedTier]} · 实际 ${tierLabels[result.actualTier ?? ''] ?? '未调用'}`}
    </p>
    {Object.keys(result.contentFilters).length > 0 && <div className="flex flex-wrap gap-2 text-xs">
      <span className="text-muted-foreground">客服知识状态：</span>
      {Object.entries(result.contentFilters).map(([key, count]) =>
        <span key={key} className="rounded-md bg-muted px-2 py-1">{filterLabels[key] ?? key} {count}</span>)}
    </div>}
    <div><p className="mb-1 text-xs font-medium text-muted-foreground">回答</p>
      <p className="whitespace-pre-wrap break-words text-sm leading-6">{result.answer}</p>
    </div>
    <div><p className="mb-2 text-xs font-medium text-muted-foreground">实际引用来源</p>
      {result.sources.length ? <div className="space-y-2">
        {result.sources.map((source, index) => <div key={`${source.materialId}-${source.chunkId}-${index}`} className="rounded-md border p-3 text-sm">
          <p className="font-medium">{source.materialTitle}{source.pageNo ? ` · 第 ${source.pageNo} 页` : ''}
            {source.score != null && <span className="ml-2 text-xs text-muted-foreground">得分 {source.score.toFixed(2)}</span>}</p>
          <p className="mt-1 line-clamp-3 text-xs leading-5 text-muted-foreground">{source.excerpt}</p>
        </div>)}
      </div> : <p className="text-sm text-muted-foreground">本次没有引用来源。</p>}
    </div>
  </div>
}

export function SupportDiagnosticsPage() {
  const { showToast } = useToast()
  const { data: cases = [], isLoading } = useQuery({
    queryKey: ['support-evaluation-cases'], queryFn: listSupportEvaluationCases,
  })
  const [question, setQuestion] = useState('')
  const [scenario, setScenario] = useState<SupportScenario | ''>('')
  const [diagnosing, setDiagnosing] = useState(false)
  const [diagnostic, setDiagnostic] = useState<SupportDiagnostic | null>(null)
  const [results, setResults] = useState<Record<string, SupportEvaluationResult>>({})
  const [running, setRunning] = useState(false)
  const [activeCase, setActiveCase] = useState<string | null>(null)
  const stopRequested = useRef(false)
  const summary = useMemo(() => {
    const all = Object.values(results)
    const tested = all.filter((item) => item.status !== 'SKIPPED')
    const passed = tested.filter((item) => item.status === 'PASS').length
    const expired = tested.filter((item) => item.testCase.group === '过期陷阱')
    const expiredPassed = expired.filter((item) => item.status === 'PASS').length
    return { completed: all.length, tested: tested.length, passed, failed: tested.length - passed,
      skipped: all.length - tested.length, expired: expired.length, expiredPassed }
  }, [results])

  const diagnose = async () => {
    if (!question.trim()) return
    setDiagnosing(true)
    try {
      setDiagnostic(await diagnoseSupport(question.trim(), scenario || null))
    } catch (error) {
      showToast(error instanceof Error ? error.message : '诊断失败', 3000)
    } finally {
      setDiagnosing(false)
    }
  }

  const runOne = async (id: string) => {
    setActiveCase(id)
    try {
      const result = await runSupportEvaluationCase(id)
      setResults((previous) => ({ ...previous, [id]: result }))
    } finally {
      setActiveCase(null)
    }
  }

  const runAll = async () => {
    if (!cases.length || running) return
    stopRequested.current = false
    setResults({})
    setRunning(true)
    try {
      for (const item of cases) {
        if (stopRequested.current) break
        try {
          await runOne(item.id)
        } catch (error) {
          showToast(`${item.id} 运行失败：${error instanceof Error ? error.message : '请求失败'}`, 4000)
          break
        }
      }
    } finally {
      setRunning(false)
    }
  }

  return <div className="mx-auto max-w-6xl space-y-7 pb-10">
    <header className="border-b pb-5">
      <h1 className="flex items-center gap-2 text-xl font-semibold"><ChartNoAxesCombined className="h-5 w-5" />问答诊断与评估</h1>
      <p className="mt-1 text-sm text-muted-foreground">仅管理员可见；诊断会调用真实客服检索与模型，不会创建用户会话。</p>
    </header>

    <section className="space-y-4">
      <div><h2 className="text-lg font-semibold">单题诊断</h2>
        <p className="text-sm text-muted-foreground">查看场景分流、知识状态、命中来源和模型选择。</p></div>
      <Textarea aria-label="诊断问题" value={question} onChange={(event) => setQuestion(event.target.value)}
        maxLength={6000} rows={3} placeholder="例如：游戏《叫我万岁爷》秋日签到活动什么时候结束？" />
      <div className="flex flex-wrap items-center gap-3">
        <select aria-label="诊断场景" className="h-9 rounded-md border bg-background px-3 text-sm"
          value={scenario} onChange={(event) => setScenario(event.target.value as SupportScenario | '')}>
          <option value="">自动分流</option><option value="ECOMMERCE">电商</option><option value="GAME">游戏</option>
        </select>
        <Button disabled={diagnosing || !question.trim()} onClick={() => void diagnose()}>
          {diagnosing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Search className="mr-2 h-4 w-4" />}运行诊断
        </Button>
      </div>
      {diagnostic && <DiagnosticDetails result={diagnostic} />}
    </section>

    <section className="space-y-4 border-t pt-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div><h2 className="text-lg font-semibold">客服回归评估 · {cases.length} 题</h2>
          <p className="mt-1 text-sm text-muted-foreground">基于 samples/support-knowledge 演示资料；缺资料会跳过。完整运行最多调用 17 次问答模型，按题展示进度。</p></div>
        <div className="flex gap-2">
          {running && <Button variant="outline" onClick={() => { stopRequested.current = true }}><Square className="mr-2 h-4 w-4" />本题结束后停止</Button>}
          <Button disabled={running || isLoading || cases.length === 0} onClick={() => void runAll()}>
            {running ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Play className="mr-2 h-4 w-4" />}
            {running ? `评估中 ${summary.completed}/${cases.length}` : '运行全部'}
          </Button>
        </div>
      </div>
      <div className="grid gap-2 sm:grid-cols-4">
        <Metric label="已完成" value={`${summary.completed}/${cases.length}`} />
        <Metric label="通过 / 已测" value={`${summary.passed}/${summary.tested}`} />
        <Metric label="未通过 / 缺资料" value={`${summary.failed} / ${summary.skipped}`} />
        <Metric label="过期提醒正确" value={`${summary.expiredPassed}/${summary.expired}`} />
      </div>
      <div className="divide-y rounded-lg border">
        {cases.map((item) => {
          const result = results[item.id]
          return <div key={item.id} className="grid gap-2 p-3 text-sm md:grid-cols-[5rem_6rem_minmax(0,1fr)_6rem_5rem] md:items-start">
            <span className="font-mono text-xs text-muted-foreground">{item.id}</span>
            <span className="text-xs text-muted-foreground">{item.group}</span>
            <div><p>{item.question}</p>
              {result?.findings.map((finding) => <p key={finding} className="mt-1 text-xs text-amber-700 dark:text-amber-300">{finding}</p>)}
              {result?.diagnostic && <p className="mt-1 text-xs text-muted-foreground">
                {outcomeLabels[result.diagnostic.outcome] ?? result.diagnostic.outcome}
                {result.diagnostic.modelName && ` · ${result.diagnostic.modelName}`}
                {` · ${result.diagnostic.elapsedMillis} ms`}
                {` · 引用 ${result.diagnostic.sources.length} 条`}
              </p>}
            </div>
            <span className={`text-xs font-medium ${result?.status === 'PASS' ? 'text-emerald-700 dark:text-emerald-300' : result?.status === 'FAIL' ? 'text-red-700 dark:text-red-300' : 'text-muted-foreground'}`}>
              {result ? statusLabels[result.status] : '未运行'}
            </span>
            <Button variant="outline" size="sm" disabled={running || activeCase !== null}
              onClick={() => void runOne(item.id).catch((error) => showToast(error instanceof Error ? error.message : '评估失败', 3000))}>
              {activeCase === item.id ? <Loader2 className="h-4 w-4 animate-spin" /> : '运行'}
            </Button>
          </div>
        })}
        {!isLoading && cases.length === 0 && <p className="p-5 text-sm text-muted-foreground">评估题目暂不可用。</p>}
      </div>
    </section>
  </div>
}
