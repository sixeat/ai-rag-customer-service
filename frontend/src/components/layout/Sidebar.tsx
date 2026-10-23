import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { BookOpen, ChevronLeft, Headset, LogOut, Moon, ScrollText, Settings, Sun, Users } from 'lucide-react'
import { ADMIN_SECTIONS, WORKSPACE_SECTIONS } from '@/constants'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/context/AuthContext'
import { redirectToLogin } from '@/lib/auth-gate'
import { cn } from '@/lib/utils'
import { ProfileDialog } from './ProfileDialog'
import { UserAvatar } from './UserAvatar'

const iconMap: Record<string, React.ElementType> = {
  headset: Headset,
  'book-open': BookOpen,
  users: Users,
  'scroll-text': ScrollText,
}

export function Sidebar() {
  const location = useLocation()
  const navigate = useNavigate()
  const { session, isAuthenticated, isAdmin, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('learning-assistant.sidebar.collapsed') === 'true')
  const [theme, setTheme] = useState<'light' | 'dark'>(() => localStorage.getItem('learning-assistant.theme') === 'dark' ? 'dark' : 'light')
  const [profileOpen, setProfileOpen] = useState(false)
  const isAdminRoute = location.pathname.startsWith('/admin')
  const sections = isAdminRoute ? ADMIN_SECTIONS : WORKSPACE_SECTIONS

  useEffect(() => {
    localStorage.setItem('learning-assistant.sidebar.collapsed', String(collapsed))
  }, [collapsed])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    localStorage.setItem('learning-assistant.theme', theme)
  }, [theme])

  return (
    <aside className={cn(
      'flex h-[100dvh] min-h-0 shrink-0 flex-col bg-[#f7f8fb] px-3 py-4 transition-[width] duration-200 dark:bg-[#111318]',
      collapsed ? 'w-[76px]' : 'w-[240px]',
    )}>
      <div className="mb-6 flex items-center justify-between gap-2 px-1">
        <button type="button" onClick={() => navigate('/workspace/support')} title="智能客服"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#202124] text-white dark:bg-white dark:text-[#202124]">
          <Headset className="h-5 w-5" />
        </button>
        {!collapsed && <span className="min-w-0 flex-1 truncate text-sm font-semibold">智能客服</span>}
        <button type="button" onClick={() => setCollapsed((value) => !value)}
          title={collapsed ? '展开侧边栏' : '收起侧边栏'} aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
          className="rounded-md p-1.5 text-muted-foreground hover:bg-slate-200/70 dark:hover:bg-white/10">
          <ChevronLeft className={cn('h-4 w-4', collapsed && 'rotate-180')} />
        </button>
      </div>

      <nav className="min-h-0 flex-1 space-y-1 overflow-y-auto" aria-label={isAdminRoute ? '管理后台' : '客服'}>
        {sections.map((section) => {
          const Icon = iconMap[section.icon] || Headset
          const active = location.pathname === section.path
          return <button key={section.path} type="button" onClick={() => navigate(section.path)}
            title={collapsed ? section.label : undefined}
            className={cn('flex h-11 w-full items-center rounded-lg text-sm font-medium transition-colors',
              collapsed ? 'justify-center' : 'gap-3 px-3',
              active ? 'bg-white text-[#202124] shadow-sm dark:bg-white/10 dark:text-white'
                : 'text-[#626a76] hover:bg-white/70 dark:text-slate-300 dark:hover:bg-white/[0.07]')}>
            <Icon className="h-4 w-4 shrink-0" />
            {!collapsed && <span className="truncate">{section.label}</span>}
          </button>
        })}
        {isAdmin && <button type="button"
          onClick={() => navigate(isAdminRoute ? '/workspace/support' : '/admin/support')}
          title={isAdminRoute ? '返回客服' : '管理后台'}
          className={cn('flex h-11 w-full items-center rounded-lg text-sm text-muted-foreground hover:bg-white/70 dark:hover:bg-white/[0.07]',
            collapsed ? 'justify-center' : 'gap-3 px-3')}>
          {isAdminRoute ? <Headset className="h-4 w-4" /> : <Settings className="h-4 w-4" />}
          {!collapsed && <span>{isAdminRoute ? '返回客服' : '管理后台'}</span>}
        </button>}
      </nav>

      <div className="space-y-3 border-t border-slate-200/70 pt-3 dark:border-slate-800">
        {!collapsed && <div className="grid grid-cols-2 gap-1 rounded-md bg-[#eef1f5] p-1 dark:bg-white/[0.08]">
          {(['light', 'dark'] as const).map((value) => <button key={value} type="button"
            onClick={() => setTheme(value)} aria-label={value === 'light' ? '浅色模式' : '深色模式'}
            className={cn('flex h-8 items-center justify-center gap-1 rounded text-xs',
              theme === value ? 'bg-white text-foreground shadow-sm dark:bg-slate-900' : 'text-muted-foreground')}>
            {value === 'light' ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
            {value === 'light' ? '浅色' : '深色'}
          </button>)}
        </div>}
        <div className="flex items-center gap-1">
          <button type="button" title={isAuthenticated ? '账号设置' : '登录'}
            onClick={() => isAuthenticated ? setProfileOpen(true) : redirectToLogin(0)}
            className={cn('flex min-w-0 items-center gap-2 rounded-lg p-1 text-left hover:bg-white/70 dark:hover:bg-white/[0.07]',
              collapsed ? 'justify-center' : 'flex-1')}>
            <UserAvatar session={session} />
            {!collapsed && <span className="min-w-0 truncate text-sm">{session?.nickname || session?.username || '未登录'}</span>}
          </button>
          {!collapsed && isAuthenticated && <Button variant="ghost" size="icon" title="退出登录" onClick={logout}>
            <LogOut className="h-4 w-4" />
          </Button>}
        </div>
      </div>
      <ProfileDialog open={profileOpen} onOpenChange={setProfileOpen} />
    </aside>
  )
}
