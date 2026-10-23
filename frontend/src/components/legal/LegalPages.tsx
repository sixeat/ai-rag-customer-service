import { ArrowLeft, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'

const LAST_UPDATED = '2026-10-24'
const CONTACT_EMAIL = '2393158856@qq.com'

type LegalSection = {
  title: string
  paragraphs: string[]
}

type LegalPageProps = {
  eyebrow: string
  title: string
  description: string
  sections: LegalSection[]
}

function LegalPageLayout({ eyebrow, title, description, sections }: LegalPageProps) {
  return (
    <main className="min-h-screen bg-[#f7f9fb] px-5 py-6 text-[#222833] sm:px-8 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <nav className="mb-8 flex items-center justify-between gap-4">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm transition hover:border-slate-300 hover:text-slate-950"
          >
            <ArrowLeft className="h-4 w-4" />
            返回首页
          </Link>
          <span className="text-sm font-bold text-slate-500">智能客服</span>
        </nav>

        <header className="border-b border-slate-200 pb-8">
          <p className="text-sm font-black tracking-[0.18em] text-slate-500">{eyebrow}</p>
          <h1 className="mt-4 text-4xl font-black leading-tight tracking-normal text-[#222833] sm:text-5xl">{title}</h1>
          <p className="mt-5 max-w-3xl text-base font-medium leading-8 text-slate-600">{description}</p>
          <p className="mt-4 text-sm font-bold text-slate-500">最后更新：{LAST_UPDATED}</p>
        </header>

        <article className="space-y-8 py-8">
          {sections.map((section) => (
            <section key={section.title} className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="text-xl font-black text-[#222833]">{section.title}</h2>
              <div className="mt-4 space-y-3 text-sm leading-7 text-slate-600">
                {section.paragraphs.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
              </div>
            </section>
          ))}
        </article>
      </div>
    </main>
  )
}

export function TermsPage() {
  return (
    <LegalPageLayout
      eyebrow="TERMS"
      title="使用协议"
      description="本协议用于说明你在访问和使用智能客服时应遵守的基本规则。使用本服务即表示你理解并同意以下内容。"
      sections={[
        {
          title: '一、服务说明',
          paragraphs: [
            '智能客服提供电商与游戏问题问答、资料来源展示、会话记录和人工客服转接。管理员可维护客服知识。',
            'AI 回答基于用户问题、已发布的客服知识和模型能力形成；具体订单、账号或退款操作由人工客服核实处理。',
          ],
        },
        {
          title: '二、账号与安全',
          paragraphs: [
            '你应妥善保管账号、密码和登录状态，不得将账号提供给他人进行违法、侵权或破坏平台稳定性的行为。',
            '如发现账号异常使用或未授权登录，请及时通过本协议中的联系方式与我们联系。',
          ],
        },
        {
          title: '三、会话与内容',
          paragraphs: [
            '请勿在会话中提交与客服处理无关的敏感信息，或侵犯他人的隐私、商业秘密和合法权益。',
            '你不得提交违法违规、诈骗引流、侵犯个人信息或其他不适宜内容。',
            '管理员发布的客服资料可能经过文本提取、切片、索引和向量化，用于检索问答。',
          ],
        },
        {
          title: '四、AI 输出限制',
          paragraphs: [
            'AI 输出可能存在遗漏、误解、过时或不准确的情况。请结合展示的资料来源核验；涉及个人订单或游戏账号时可转人工。',
            '当知识不足、模型服务异常或网络波动时，回答可能失败或延迟。',
          ],
        },
        {
          title: '五、禁止行为',
          paragraphs: [
            '不得通过爬虫、批量注册、接口滥用、绕过限流、逆向工程、攻击服务器或其他方式影响平台安全与稳定。',
            '不得利用平台生成、传播违法违规内容，或诱导模型输出危害网络安全、侵犯隐私、作弊代写、欺诈等内容。',
          ],
        },
        {
          title: '六、服务变更与免责声明',
          paragraphs: [
            '我们可能根据产品迭代、合规要求或系统维护调整部分功能、接口、容量限制和展示方式。',
            '因不可抗力、第三方服务故障、网络异常或模型服务异常导致的服务中断或回答偏差，我们将在合理范围内协助处理。',
          ],
        },
        {
          title: '七、联系我们',
          paragraphs: [`如你对本协议或平台使用有疑问，可通过邮箱 ${CONTACT_EMAIL} 联系我们。`],
        },
      ]}
    />
  )
}

export function PrivacyPage() {
  return (
    <LegalPageLayout
      eyebrow="PRIVACY"
      title="隐私政策"
      description="本政策说明智能客服如何收集、使用、存储和保护与你使用服务相关的信息。"
      sections={[
        {
          title: '一、我们收集的信息',
          paragraphs: [
            '账号信息：包括注册、登录、身份校验所需的账号标识、昵称、角色、登录时间等信息。',
            '客服内容：包括你提交的问题、会话消息、AI 回答、引用来源和人工客服回复。',
            '设备与日志：包括请求时间、接口路径、错误日志、浏览器信息、IP 地址、性能状态等用于安全审计和问题排查的信息。',
          ],
        },
        {
          title: '二、信息使用目的',
          paragraphs: [
            '用于完成场景分流、检索增强问答、会话恢复、人工接管、后台管理和安全风控。',
            '用于定位回答错误、页面加载异常、账户异常访问等问题，并持续改进产品体验。',
          ],
        },
        {
          title: '三、客服知识与第三方服务',
          paragraphs: [
            '为了实现 AI 问答和向量检索，系统可能会将必要的文本片段、问题和上下文发送给配置的模型服务或向量检索服务处理。',
            '我们会尽量只传输完成当前功能所需的最小必要内容，并通过配置、权限和访问控制降低非必要暴露风险。',
          ],
        },
        {
          title: '四、存储与安全',
          paragraphs: [
            '我们会采用访问控制、日志审计和必要的传输安全措施保护会话与账号信息。',
            '请理解任何互联网服务都无法保证绝对安全。如发生安全事件，我们会在合理范围内采取补救措施并按适用要求通知相关用户。',
          ],
        },
        {
          title: '五、你的权利',
          paragraphs: [
            '你可以在平台内查看自己的客服会话；如需查询、更正或删除相关信息，可通过本政策中的联系方式提出请求。',
            '如需进一步查询、更正或删除账号相关信息，可通过本政策中的联系方式向我们提出请求。',
          ],
        },
        {
          title: '六、未成年人使用',
          paragraphs: [
            '未成年人应在监护人指导下使用本服务，不应提交本人或他人的敏感个人信息。',
          ],
        },
        {
          title: '七、联系我们',
          paragraphs: [`如你对隐私保护、资料处理或账号信息有疑问，可通过邮箱 ${CONTACT_EMAIL} 联系我们。`],
        },
      ]}
    />
  )
}

export function AboutPage() {
  return (
    <main className="min-h-screen bg-[#f7f9fb] px-5 py-6 text-[#222833] sm:px-8 lg:px-10">
      <div className="mx-auto max-w-4xl">
        <nav className="mb-8 flex items-center justify-between gap-4">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-700 shadow-sm transition hover:border-slate-300 hover:text-slate-950"
          >
            <ArrowLeft className="h-4 w-4" />
            返回首页
          </Link>
          <span className="text-sm font-bold text-slate-500">智能客服</span>
        </nav>

        <section className="rounded-lg border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
          <p className="text-sm font-black tracking-[0.18em] text-slate-500">ABOUT</p>
          <h1 className="mt-4 text-4xl font-black leading-tight tracking-normal text-[#222833] sm:text-5xl">关于我们</h1>
          <p className="mt-5 text-base font-medium leading-8 text-slate-600">
            智能客服为电商和游戏问题提供统一问答入口。系统根据问题匹配管理员维护的客服知识，必要时将会话转交人工客服。
          </p>
          <p className="mt-4 text-base font-medium leading-8 text-slate-600">
            回答会展示资料来源，便于核对优惠活动、游戏活动和版本更新等信息。管理员负责发布和维护这些内容。
          </p>

          <div className="mt-8 rounded-lg bg-[#f7f9fb] p-5">
            <h2 className="text-xl font-black text-[#222833]">联系方式</h2>
            <a
              className="mt-4 inline-flex items-center gap-2 text-sm font-black text-slate-700 transition hover:text-slate-950 hover:underline"
              href={`mailto:${CONTACT_EMAIL}`}
            >
              <Mail className="h-4 w-4" />
              {CONTACT_EMAIL}
            </a>
          </div>

          <p className="mt-6 text-sm font-bold text-slate-500">最后更新：{LAST_UPDATED}</p>
        </section>
      </div>
    </main>
  )
}
