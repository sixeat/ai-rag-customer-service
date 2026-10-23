import { expect, test } from '@playwright/test'

const sessionKey = 'learning-assistant.frontend.session'
const ok = (data: unknown) => ({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, message: 'ok', data }) })

test.use({ channel: process.platform === 'win32' ? 'msedge' : undefined, video: 'off' })

test('admin publishes support content from text on desktop and mobile', async ({ page }) => {
  let items: Record<string, unknown>[] = []
  await page.addInitScript(({ key }) => {
    localStorage.setItem(key, JSON.stringify({ id: '9', username: 'admin', nickname: 'Admin', role: 'ADMIN', token: 'test-token' }))
  }, { key: sessionKey })
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (!path.startsWith('/api/')) return route.continue()
    if (path === '/api/auth/me') return route.fulfill(ok({ id: '9', username: 'admin', nickname: 'Admin', role: 'ADMIN' }))
    if (path === '/api/admin/support-content' && route.request().method() === 'GET') return route.fulfill(ok(items))
    if (path === '/api/admin/support-content' && route.request().method() === 'POST') {
      const body = route.request().postData() ?? ''
      expect(body).toContain('ECOMMERCE_PROMOTION')
      expect(body).toContain('满减活动')
      expect(body).toContain('满减规则详见公告。')
      items = [{ id: '1', materialId: '10', category: 'ECOMMERCE_PROMOTION', title: '满减活动', body: '满减规则详见公告。', published: true, startsAt: null, endsAt: null, textStatus: 'READY', updatedAt: '2026-10-24T00:00:00' }]
      return route.fulfill(ok(items[0]))
    }
    return route.fulfill(ok([]))
  })

  await page.goto('/admin/support-content')
  await expect(page.getByRole('navigation', { name: '管理后台' }).getByRole('button', { name: '客服知识' })).toBeVisible()
  await expect(page.getByRole('button', { name: '资料队列' })).toHaveCount(0)
  await page.getByRole('button', { name: '新增内容' }).click()
  await page.getByRole('textbox', { name: '标题' }).fill('满减活动')
  await page.getByRole('textbox', { name: '正文' }).fill('满减规则详见公告。')
  await page.getByRole('checkbox', { name: '发布到客服问答' }).check()
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('满减活动')).toBeVisible()
  await expect(page.getByText('电商优惠 · 生效中')).toBeVisible()
  await page.screenshot({ path: 'test-results/support-content-desktop.png', fullPage: true })

  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.getByText('满减活动')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/support-content-mobile.png', fullPage: true })

  await page.goto('/admin/dashboard')
  await expect(page).toHaveURL(/\/admin\/support$/)
})
