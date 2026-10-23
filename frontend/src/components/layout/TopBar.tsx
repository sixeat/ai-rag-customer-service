import { useLocation } from 'react-router-dom'
import { Menu } from 'lucide-react'
import { ADMIN_SECTIONS, WORKSPACE_SECTIONS } from '@/constants'
import { Button } from '@/components/ui/button'

interface TopBarProps {
  onOpenMobileMenu?: () => void
}

export function TopBar({ onOpenMobileMenu }: TopBarProps) {
  const location = useLocation()
  const current = [...WORKSPACE_SECTIONS, ...ADMIN_SECTIONS].find((section) => section.path === location.pathname)

  return <header className="flex h-14 shrink-0 items-center gap-3 border-b border-[#edf1f5] bg-[#fcfcfd] px-3 dark:border-slate-800 dark:bg-[#171a21] md:px-6">
    <Button variant="ghost" size="icon" className="h-9 w-9 shrink-0 md:hidden"
      title="打开菜单" onClick={onOpenMobileMenu}>
      <Menu className="h-4 w-4" />
    </Button>
    <h1 className="truncate text-sm font-semibold">{current?.label || '智能客服'}</h1>
  </header>
}
