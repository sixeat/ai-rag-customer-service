import { expect, test } from '@playwright/test'

const sessionKey = 'learning-assistant.frontend.session'
const ok = (data: unknown) => ({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'ok', data }) })

test.use({ channel: process.platform === 'win32' ? 'msedge' : undefined, video: 'off' })

test('single support entry answers and hands off on desktop and mobile', async ({ page }) => {
  let conversation: Record<string, unknown> | null = null
  const personalRequests: string[] = []
  await page.addInitScript(({ key }) => {
    localStorage.setItem(key, JSON.stringify({ id: '7', username: 'tester', nickname: 'Tester', role: 'USER', token: 'test-token' }))
  }, { key: sessionKey })
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (!path.startsWith('/api/')) return route.continue()
    if (/^\/api\/(?:materials|knowledge-bases|rag|favorites|llm)(?:\/|$)/.test(path)) personalRequests.push(path)
    if (path === '/api/auth/me') return route.fulfill(ok({ id: '7', username: 'tester', nickname: 'Tester', role: 'USER' }))
    if (path === '/api/support/conversations' && route.request().method() === 'GET') return route.fulfill(ok(conversation ? [conversation] : []))
    if (path === '/api/support/conversations' && route.request().method() === 'POST') {
      const content = String(route.request().postDataJSON().content)
      conversation = {
        id: '1', ownerId: '7', scenario: 'ECOMMERCE', status: 'AI', createdAt: '2026-10-23T12:00:00', updatedAt: '2026-10-23T12:00:00',
        messages: [
          { id: '1', sender: 'USER', content, sources: [], createdAt: '2026-10-23T12:00:00' },
          { id: '2', sender: 'AI', content: '订单通常在三个工作日内发货。', sources: [{ materialId: '10', chunkId: '20', materialTitle: '配送政策', pageNo: 1, excerpt: '三个工作日内发货', score: 0.9 }], createdAt: '2026-10-23T12:00:01' },
        ],
      }
      return route.fulfill(ok(conversation))
    }
    if (path === '/api/support/conversations/1/handoff') {
      conversation = { ...conversation, status: 'WAITING_HUMAN', messages: [...(conversation?.messages as unknown[]), { id: '3', sender: 'AI', content: '已转交人工客服，后续回复会显示在此会话。', sources: [], createdAt: '2026-10-23T12:00:02' }] }
      return route.fulfill(ok(conversation))
    }
    return route.fulfill(ok(path === '/api/materials' || path === '/api/favorites' || path === '/api/rag/history' ? [] : {}))
  })

  await page.goto('/workspace/support')
  await expect(page).toHaveURL(/\/workspace\/support/)
  await expect(page.getByRole('heading', { name: '客服', exact: true })).toBeVisible()
  await expect(page.getByRole('navigation', { name: '客服' }).getByRole('button', { name: '智能客服' })).toBeVisible()
  await expect(page.getByRole('button', { name: '资料管理' })).toHaveCount(0)
  await page.getByRole('textbox', { name: '客服问题' }).fill('订单什么时候发货')
  await page.getByRole('button', { name: '发送' }).click()
  await expect(page.getByText('订单通常在三个工作日内发货。')).toBeVisible()
  await expect(page.getByText('配送政策')).toBeVisible()
  await page.getByRole('button', { name: '转人工' }).click()
  await expect(page.getByText('已转交人工客服，后续回复会显示在此会话。')).toBeVisible()
  await page.screenshot({ path: 'test-results/support-desktop.png', fullPage: true })

  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.getByRole('textbox', { name: '客服问题' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/support-mobile.png', fullPage: true })

  await page.goto('/workspace/knowledge-bases')
  await expect(page).toHaveURL(/\/workspace\/support$/)
  expect(personalRequests).toEqual([])
})
