import { lazy, Suspense, type ReactNode } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import App from '@/App'
import { AuthLayout } from '@/components/auth/AuthLayout'
import { AppShell } from '@/components/layout/AppShell'
import { ProtectedRoute } from '@/components/layout/ProtectedRoute'
import { RouteFallback } from '@/components/layout/RouteFallback'

const TermsPage = lazy(() => import('@/components/legal/LegalPages').then((module) => ({ default: module.TermsPage })))
const PrivacyPage = lazy(() => import('@/components/legal/LegalPages').then((module) => ({ default: module.PrivacyPage })))
const AboutPage = lazy(() => import('@/components/legal/LegalPages').then((module) => ({ default: module.AboutPage })))
const LoginForm = lazy(() => import('@/components/auth/LoginForm').then((module) => ({ default: module.LoginForm })))
const RegisterForm = lazy(() => import('@/components/auth/RegisterForm').then((module) => ({ default: module.RegisterForm })))
const ForgotPasswordForm = lazy(() => import('@/components/auth/ForgotPasswordForm').then((module) => ({ default: module.ForgotPasswordForm })))
const SupportPage = lazy(() => import('@/components/workspace/SupportPage').then((module) => ({ default: module.SupportPage })))
const UsersPage = lazy(() => import('@/components/admin/UsersPage').then((module) => ({ default: module.UsersPage })))
const LogsPage = lazy(() => import('@/components/admin/LogsPage').then((module) => ({ default: module.LogsPage })))
const SupportQueuePage = lazy(() => import('@/components/admin/SupportQueuePage').then((module) => ({ default: module.SupportQueuePage })))
const SupportContentPage = lazy(() => import('@/components/admin/SupportContentPage').then((module) => ({ default: module.SupportContentPage })))

/** 路由级加载态，用于懒加载页面组件时保持布局稳定。 */
function RouteLoading() {
  return (
    <div className="flex min-h-[320px] items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#4b5563] border-t-transparent" />
    </div>
  )
}

/** 给页面路由统一包一层 Suspense，避免首屏一次性下载所有页面代码。 */
function lazyElement(element: ReactNode) {
  return <Suspense fallback={<RouteLoading />}>{element}</Suspense>
}

/**
 * 路由配置 — 定义整个应用的页面结构。
 *
 * 路由层级：
 * / (App)
 *   ├── index → 重定向到客服入口
 *   ├── 认证页面（不需要登录，AuthLayout 布局）
 *   │   ├── /login          → 登录页
 *   │   ├── /register       → 注册页
 *   │   └── /forgot-password → 忘记密码页
 *   ├── /workspace（AppShell 布局）
 *   │   └── support         → 电商与游戏统一客服
 *   ├── /admin（需要管理员权限，ProtectedRoute(requireAdmin) + AppShell）
 *   │   ├── support         → 人工客服队列
 *   │   ├── support-content → 客服知识
 *   │   ├── users           → 用户管理
 *   │   └── logs            → 系统日志
 *   └── * → 404 回退页
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,  // App 是根组件（ToastProvider + Outlet）
    children: [
      // 根路径直接进入客服工作区；未登录用户在问答入口登录。
      { index: true, element: <Navigate to="/workspace/support" replace /> },
      { path: 'terms', element: lazyElement(<TermsPage />) },
      { path: 'privacy', element: lazyElement(<PrivacyPage />) },
      { path: 'about', element: lazyElement(<AboutPage />) },

      // ===== 认证页面（不需要登录） =====
      {
        element: <AuthLayout />,  // AuthLayout 提供登录/注册的统一样式
        children: [
          { path: 'login', element: lazyElement(<LoginForm />) },
          { path: 'register', element: lazyElement(<RegisterForm />) },
          { path: 'forgot-password', element: lazyElement(<ForgotPasswordForm />) },
        ],
      },

      // ===== 工作区（需要登录） =====
      {
        path: 'workspace',
        element: <ProtectedRoute allowGuest />,  // 工作区允许游客预览；具体功能入口再提示登录
        children: [
          {
            element: <AppShell />,  // AppShell 提供侧边栏 + 顶栏 + 内容区布局
            children: [
              // 客服是本分支唯一的用户工作区入口。
              { index: true, element: <Navigate to="/workspace/support" replace /> },
              { path: 'support', element: lazyElement(<SupportPage />) }, // 统一客服入口
              { path: '*', element: <Navigate to="/workspace/support" replace /> },
            ],
          },
        ],
      },

      // ===== 管理后台（需要管理员权限） =====
      {
        path: 'admin',
        element: <ProtectedRoute requireAdmin />,  // requireAdmin 确保只有管理员能进入
        children: [
          {
            element: <AppShell />,
            children: [
              { index: true, element: <Navigate to="/admin/support" replace /> },
              { path: 'users', element: lazyElement(<UsersPage />) },              // 用户管理
              { path: 'support', element: lazyElement(<SupportQueuePage />) }, // 人工客服队列
              { path: 'support-content', element: lazyElement(<SupportContentPage />) },
              { path: 'logs', element: lazyElement(<LogsPage />) },               // 系统日志
              { path: '*', element: <Navigate to="/admin/support" replace /> },
            ],
          },
        ],
      },

      // ===== 404 回退 =====
      { path: '*', element: <RouteFallback /> },
    ],
  },
])
