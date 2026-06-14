import * as ts from 'typescript'
import { describe, expect, it } from 'vitest'

const nativeDialogs = new Set(['alert', 'confirm', 'prompt'])
const nativeDialogHosts = new Set(['globalThis', 'self', 'window'])
const publicCommentOwnerFiles = [
  '../api/types.ts',
  '../views/Heritage.vue',
  '../views/ScenicSpotDetail.vue'
] as const
const publicCommunityViewFiles = [
  '../views/RouteDetail.vue',
  '../views/QuestionDetail.vue',
  '../views/RouteCommunity.vue'
] as const
const sharedUploadComponentFiles = [
  '../components/ImageUploadField.vue'
] as const
const safeClientErrorDisplayFiles = [
  '../components/AdminCommunityPanel.vue',
  '../views/AdminDashboard.vue',
  '../views/CreateRoute.vue',
  '../views/HotelBooking.vue',
  '../views/Login.vue',
  '../views/OrderCenter.vue',
  '../views/Register.vue',
  '../views/ScenicSpots.vue',
  '../views/UserProfile.vue'
] as const
const dynamicLeafletOwnerFiles = [
  '../views/Heritage.vue',
  '../views/ScenicSpotDetail.vue'
] as const
const userVisibleDataDisplayFiles = [
  '../components/HeatMap.vue',
  '../data/hotelImages.ts',
  '../data/hotelTranslations.ts',
  '../views/CreateRoute.vue',
  '../views/Favorites.vue'
] as const
const sourceModules = import.meta.glob('../**/*.{js,jsx,ts,tsx,vue}', {
  eager: true,
  query: '?raw',
  import: 'default'
}) as Record<string, string>
const commonMojibakeMarkers = [
  { label: 'mojibake arrow', value: String.fromCodePoint(0x922b) },
  { label: 'mojibake emoji prefix', value: String.fromCodePoint(0x9983) },
  { label: 'mojibake full-width colon', value: String.fromCodePoint(0x951b) },
  { label: 'mojibake heart', value: String.fromCodePoint(0x9242) },
  { label: 'mojibake CJK punctuation', value: String.fromCodePoint(0x9286) },
  { label: 'replacement character', value: String.fromCodePoint(0xfffd) },
  { label: 'latin mojibake prefix', value: String.fromCodePoint(0x00e2) },
  { label: 'latin mojibake prefix', value: String.fromCodePoint(0x00c3) }
] as const

interface NativeDialogFinding {
  dialog: string
  filePath: string
  line: number
  column: number
  snippet: string
}

describe('frontend source guardrails', () => {
  it('does not use native browser alert, confirm, or prompt dialogs', () => {
    const findings = Object.entries(sourceModules)
      .map(([filePath, source]) => ({
        filePath: normalizePath(filePath),
        source: String(source)
      }))
      .filter(({ filePath }) => !isTestFile(filePath))
      .flatMap(({ filePath, source }) => findNativeDialogCalls(filePath, source))

    expect(findings.map(formatFinding)).toEqual([])
  })

  it('uses owner instead of userId for public comment ownership', () => {
    for (const filePath of publicCommentOwnerFiles) {
      const source = getSourceModule(filePath)

      expect(source, filePath).toContain('owner')
      expect(source, filePath).not.toMatch(/\buserId\b/)
    }
  })

  it('keeps public community views free of common mojibake markers', () => {
    const findings = publicCommunityViewFiles.flatMap(filePath => {
      const source = getSourceModule(filePath)

      return commonMojibakeMarkers.flatMap(marker =>
        findTextOccurrences(source, marker.value).map(location =>
          `${filePath.replace(/^\.\.\//, '')}:${location.line}:${location.column} contains ${marker.label}: ${location.snippet}`
        )
      )
    })

    expect(findings).toEqual([])
  })

  it('keeps public community ownership checks on backend owner flags', () => {
    const routeDetailSource = getSourceModule('../views/RouteDetail.vue')
    const questionDetailSource = getSourceModule('../views/QuestionDetail.vue')

    expect(routeDetailSource).toContain('comment?.user?.owner')
    expect(routeDetailSource).not.toMatch(/comment\.user\.(?:id|username)/)
    expect(questionDetailSource).toContain('question.value?.author?.owner')
    expect(questionDetailSource).not.toContain('question.value?.author?.id === currentUserId.value')
    expect(questionDetailSource).not.toContain('currentUserId')
    expect(questionDetailSource).not.toContain('auth.user?.id')
    expect(questionDetailSource).not.toContain('question.author?.username')
    expect(questionDetailSource).not.toContain('answer.user?.username')
  })

  it('keeps session UI off raw auth user account identifiers', () => {
    const authSource = getSourceModule('../stores/auth.ts')
    const navBarSource = getSourceModule('../components/NavBar.vue')
    const userProfileSource = getSourceModule('../views/UserProfile.vue')
    const questionDetailSource = getSourceModule('../views/QuestionDetail.vue')
    const adminDashboardSource = getSourceModule('../views/AdminDashboard.vue')
    const adminUserActionSource = getSourceSection(
      adminDashboardSource,
      'const canDeleteUser',
      '// Carousel management'
    )
    const sessionConsumers = [
      ['../components/NavBar.vue', navBarSource],
      ['../views/UserProfile.vue', userProfileSource],
      ['../views/QuestionDetail.vue', questionDetailSource],
      ['../views/AdminDashboard.vue', adminUserActionSource]
    ] as const

    expect(authSource).not.toMatch(/\b(?:id|username)\?:/)
    expect(authSource).not.toContain('[key: string]')
    expect(authSource).toContain('type RawAuthUser')
    expect(getSourceSection(authSource, 'function applySession', 'function login')).toContain('sanitizeSessionProfile(userData)')
    for (const [filePath, source] of sessionConsumers) {
      expect(source, filePath).not.toMatch(/\bauth\.user\??\.\s*(?:id|username)\b/)
    }
    expect(userProfileSource).not.toMatch(/\b(?:userInfo|user)\??\.\s*username\b/)
    expect(questionDetailSource).not.toContain('currentUserId')
    expect(adminUserActionSource).not.toContain('currentUser')
  })

  it('keeps missing nickname display fallbacks generic and trimmed', () => {
    const navBarSource = getSourceModule('../components/NavBar.vue')
    const userProfileSource = getSourceModule('../views/UserProfile.vue')
    const questionDetailSource = getSourceModule('../views/QuestionDetail.vue')
    const sessionDisplaySources = [
      ['../components/NavBar.vue', navBarSource],
      ['../views/UserProfile.vue', userProfileSource],
      ['../views/QuestionDetail.vue', questionDetailSource]
    ] as const

    expect(navBarSource).toContain("auth.user?.nickname?.trim()")
    expect(navBarSource).toContain("t('profile.member')")
    expect(userProfileSource).toContain('normalizeProfileText(userInfo.value?.nickname)')
    expect(userProfileSource).toContain('profileRoleLabel.value')
    expect(questionDetailSource).toContain('normalizePublicUserName(user?.nickname)')
    expect(questionDetailSource).toContain("t('questionDetail.anonymousUser')")

    for (const [filePath, source] of sessionDisplaySources) {
      expect(source, filePath).not.toMatch(/\bnickname\b\s*(?:\|\||\?\?)\s*[^;\n]*(?:username|id)\b/)
      expect(source, filePath).not.toMatch(/\b(?:username|id)\b\s*(?:\|\||\?\?)\s*\bnickname\b/)
    }
  })

  it('keeps shared upload failures generic instead of rendering raw backend errors', () => {
    for (const filePath of sharedUploadComponentFiles) {
      const source = getSourceModule(filePath)

      expect(source, filePath).toContain('summarizeClientError')
      expect(source, filePath).toContain('uploadEndpoint: string')
      expect(source, filePath).not.toContain("uploadEndpoint: '/admin/upload-image'")
      expect(source, filePath).toContain(':alt="uploadLabel(\'previewAlt\')"')
      expect(source, filePath).toContain('loading="lazy"')
      expect(source, filePath).toContain('decoding="async"')
      expect(source, filePath).toContain("t('upload.uploadFailed')")
      expect(source, filePath).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
      expect(source, filePath).not.toMatch(/\b\w+\.message\b/)
    }
  })

  it('keeps high-impact user-visible failures on safe local fallback text', () => {
    for (const filePath of safeClientErrorDisplayFiles) {
      const source = getSourceModule(filePath)

      expect(source, filePath).toContain('safeClientErrorMessage')
      expect(source, filePath).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
      expect(source, filePath).not.toMatch(/\b(?:data|responseData|serverError)\??\.\s*(?:error|message)\b/)
      expect(source, filePath).not.toMatch(/\b\w+\.message\b/)
    }
  })

  it('keeps user-visible data display files off broad production any', () => {
    for (const filePath of userVisibleDataDisplayFiles) {
      const source = getSourceModule(filePath)

      expect(source, filePath).not.toMatch(/\bany\b/)
    }
  })

  it('keeps admin hotel orders off internal user summaries', () => {
    const adminDashboardSource = getSourceModule('../views/AdminDashboard.vue')
    const hotelOrderTemplate = getSourceSection(
      adminDashboardSource,
      '<!-- Hotel Orders Management -->',
      '<!-- Recent Bookings -->'
    )
    const hotelOrderScript = getSourceSection(
      adminDashboardSource,
      'interface HotelOrder',
      'const routeSourceLabel'
    )

    expect(hotelOrderTemplate).toContain('displayHotelOrderGuest(order)')
    expect(hotelOrderTemplate).toContain('maskHotelOrderPhone(order.phone)')
    expect(hotelOrderTemplate).not.toMatch(/order(?:\.|\[['"])user/)
    expect(hotelOrderTemplate).not.toContain("t('admin.user')")
    expect(hotelOrderTemplate).not.toContain("|| '-'")
    expect(hotelOrderScript).toContain('const maskHotelOrderPhone')
    expect(hotelOrderScript).not.toMatch(/\buser\??\.\s*username\b/)
  })

  it('keeps admin user actions on backend policy flags instead of hardcoded account names', () => {
    const adminDashboardSource = getSourceModule('../views/AdminDashboard.vue')
    const adminUserActionSource = getSourceSection(
      adminDashboardSource,
      'const canChangeUserRole',
      '// Carousel management'
    )

    expect(adminDashboardSource).not.toMatch(/['"]lzh['"]/)
    expect(adminDashboardSource).toContain('u.protectedAccount')
    expect(adminUserActionSource).toContain('roleMutable')
    expect(adminUserActionSource).toContain('deletable')
    expect(adminUserActionSource).not.toMatch(/\busername\b/)
  })

  it('keeps admin list loader failures visible through sanitized reporting', () => {
    const adminDashboardSource = getSourceModule('../views/AdminDashboard.vue')

    expect(adminDashboardSource).toContain('const reportAdminListLoadFailure')
    expect(adminDashboardSource).toContain('safeClientErrorMessage(error, fallback)')
    expect(adminDashboardSource).toContain('summarizeClientError(error)')
    for (const resource of ['admin carousels', 'admin routes', 'admin hotels', 'admin room types']) {
      expect(adminDashboardSource).toContain(`reportAdminListLoadFailure('${resource}', e,`)
    }
  })

  it('does not scope route planner drafts by internal user id or persistent job id', () => {
    const routePlannerSource = getSourceModule('../views/RoutePlanner.vue')
    const draftSource = getSourceModule('../composables/useRoutePlannerDraft.ts')

    expect(routePlannerSource).not.toContain('auth.user?.id')
    expect(routePlannerSource).not.toContain('route-planner:draft:${userId}')
    expect(draftSource).toContain('jobId: normalizeJobId(draft.jobId)')
    expect(draftSource).toContain('const normalizeJobId = (_jobId: unknown) =>')
  })

  it('keeps home recommendations on the current-session endpoint', () => {
    const homeSource = getSourceModule('../views/Home.vue')
    const recommendationSource = getSourceSection(
      homeSource,
      'const fetchRecommendations = async () =>',
      'watch(locale'
    )

    expect(recommendationSource).toContain('endpoints.spots.recommendationsMe')
    expect(recommendationSource).not.toContain('userId=')
    expect(recommendationSource).not.toContain('auth.user?.id')
    expect(recommendationSource).not.toMatch(/\buser\??\.\s*id\b/)
  })

  it('keeps GET cache session scope off stored user identifiers', () => {
    const cacheSource = getSourceModule('../api/cache.ts')
    const scopeSource = getSourceSection(
      cacheSource,
      'function getSessionCacheScope()',
      'function getRequestPath'
    )

    expect(scopeSource).toContain('AUTH_SESSION_VERSION_KEY')
    expect(scopeSource).toContain('authenticated')
    expect(cacheSource).not.toContain('normalizeSessionUserId')
    expect(cacheSource).not.toMatch(/\buser\.(?:id|username)\b/)
  })

  it('only uses v-html in components that route content through the sanitizer', () => {
    const findings = Object.entries(sourceModules)
      .map(([filePath, source]) => ({
        filePath: normalizePath(filePath),
        source: String(source)
      }))
      .filter(({ filePath }) => filePath.endsWith('.vue') && !isTestFile(filePath))
      .filter(({ source }) => /\bv-html\s*=/.test(source))
      .filter(({ source }) => !/\b(?:renderMarkdownToSafeHtml|sanitizeHtml)\b/.test(source))
      .map(({ filePath }) => `${filePath.replace(/^\.\.\//, '')} uses v-html without the shared sanitizer`)

    expect(findings).toEqual([])
  })

  it('keeps route-level pages lazy-loaded for production entry chunk health', () => {
    const routerSource = getSourceModule('../router/index.ts')
    const staticViewImports = findImportSpecifiers(routerSource)
      .filter(importSpecifier => importSpecifier.modulePath.startsWith('../views/'))
      .filter(importSpecifier => !importSpecifier.typeOnly)
      .map(importSpecifier => `router/index.ts imports ${importSpecifier.modulePath} statically`)

    expect(staticViewImports).toEqual([])
    for (const viewPath of Object.keys(sourceModules).filter(filePath => /^..\/views\/.+\.vue$/.test(filePath))) {
      const routeImport = `() => import('${viewPath.replace('../views/', '../views/')}')`
      expect(routerSource, `router should lazy-load ${viewPath}`).toContain(routeImport)
    }
  })

  it('keeps ECharts centralized behind lazy chart loaders', () => {
    const directPackageImports = Object.entries(sourceModules)
      .map(([filePath, source]) => ({
        filePath: normalizePath(filePath),
        source: String(source)
      }))
      .filter(({ filePath }) => !isTestFile(filePath))
      .flatMap(({ filePath, source }) =>
        findImportSpecifiers(source)
          .filter(importSpecifier => importSpecifier.modulePath === 'echarts' || importSpecifier.modulePath.startsWith('echarts/'))
          .filter(() => !filePath.startsWith('../lib/echarts'))
          .map(importSpecifier => `${filePath.replace(/^\.\.\//, '')} imports ${importSpecifier.modulePath} outside the ECharts adapter`)
      )
    const eagerAdapterImports = Object.entries(sourceModules)
      .map(([filePath, source]) => ({
        filePath: normalizePath(filePath),
        source: String(source)
      }))
      .filter(({ filePath }) => filePath.endsWith('.vue') && !isTestFile(filePath))
      .flatMap(({ filePath, source }) =>
        findImportSpecifiers(source)
          .filter(importSpecifier => importSpecifier.modulePath.startsWith('@/lib/echarts'))
          .filter(importSpecifier => !importSpecifier.typeOnly)
          .map(importSpecifier => `${filePath.replace(/^\.\.\//, '')} eagerly imports ${importSpecifier.modulePath}`)
      )
    const lazyChartOwners = [
      ['../components/AdminAnalyticsPanel.vue', '@/lib/echartsAdmin'],
      ['../components/HeatMap.vue', '@/lib/echartsHeatMap']
    ] as const

    expect(directPackageImports).toEqual([])
    expect(eagerAdapterImports).toEqual([])
    for (const [filePath, modulePath] of lazyChartOwners) {
      expect(getSourceModule(filePath), filePath).toContain(`import('${modulePath}')`)
    }
  })

  it('keeps Leaflet maps dynamically loaded only by map detail experiences', () => {
    const findings = Object.entries(sourceModules)
      .map(([filePath, source]) => ({
        filePath: normalizePath(filePath),
        source: String(source)
      }))
      .filter(({ filePath }) => !isTestFile(filePath))
      .flatMap(({ filePath, source }) => {
        const staticImports = findImportSpecifiers(source)
          .filter(importSpecifier => importSpecifier.modulePath === 'leaflet' || importSpecifier.modulePath.startsWith('leaflet/'))
          .filter(importSpecifier => !importSpecifier.typeOnly)
          .map(importSpecifier => `${filePath.replace(/^\.\.\//, '')} statically imports ${importSpecifier.modulePath}`)
        const dynamicImports = findDynamicImportSpecifiers(source)
          .filter(modulePath => modulePath === 'leaflet')
          .filter(() => !dynamicLeafletOwnerFiles.includes(filePath as typeof dynamicLeafletOwnerFiles[number]))
          .map(modulePath => `${filePath.replace(/^\.\.\//, '')} dynamically imports ${modulePath} outside the map detail allowlist`)

        return [...staticImports, ...dynamicImports]
      })

    expect(findings).toEqual([])
    for (const filePath of dynamicLeafletOwnerFiles) {
      const source = getSourceModule(filePath)

      expect(source, filePath).toContain("import('leaflet')")
      expect(source, filePath).toContain("import('leaflet/dist/leaflet.css')")
    }
  })

  it('keeps third-party scripts and booking links fail-closed on trusted origins', () => {
    const amapSource = getSourceModule('./amap.ts')
    const recaptchaSource = getSourceModule('./recaptcha.ts')
    const externalBookingSource = getSourceModule('./externalBooking.ts')

    expect(amapSource).toContain('isTrustedAmapScript(existingScript)')
    expect(amapSource).toContain('url.origin === AMAP_SCRIPT_ORIGIN')
    expect(recaptchaSource).toContain('isTrustedRecaptchaScript(existingScript, mode, siteKey)')
    expect(recaptchaSource).toContain('TRUSTED_RECAPTCHA_SCRIPT_ORIGINS.has(url.origin)')
    expect(externalBookingSource).toContain('allowedHostSuffixes')
    expect(externalBookingSource).toContain('hostnameMatchesSuffix')
    expect(externalBookingSource).toContain('VITE_EXTERNAL_BOOKING_ALLOWED_HOSTS')
  })
})

const isTestFile = (filePath: string) => {
  const normalizedPath = normalizePath(filePath)
  const fileName = normalizedPath.split('/').pop() || ''

  return (
    /(?:^|\/)(?:__specs__|__tests__)(?:\/|$)/.test(normalizedPath) ||
    /\.(?:spec|test)\.(?:d\.)?[cm]?[jt]sx?$/.test(fileName) ||
    /\.(?:spec|test)\.vue$/.test(fileName)
  )
}

const findNativeDialogCalls = (filePath: string, source: string): NativeDialogFinding[] => {
  if (filePath.endsWith('.vue')) {
    return [
      ...extractVueBlocks(source, 'script').flatMap(block =>
        findNativeDialogCallsInSource(filePath, block.content, source, block.offset, ts.ScriptKind.TS)
      ),
      ...extractVueTemplateExpressions(source).flatMap(expression =>
        findNativeDialogCallsInSource(filePath, expression.content, source, expression.offset, ts.ScriptKind.TSX)
      )
    ]
  }

  return findNativeDialogCallsInSource(filePath, source, source, 0, getScriptKind(filePath))
}

const extractVueBlocks = (source: string, blockName: 'script' | 'template') => {
  const blocks: Array<{ content: string; offset: number }> = []
  const blockPattern = new RegExp(`<${blockName}\\b[^>]*>([\\s\\S]*?)<\\/${blockName}>`, 'gi')
  let match: RegExpExecArray | null

  while ((match = blockPattern.exec(source)) !== null) {
    const content = match[1]
    blocks.push({
      content,
      offset: match.index + match[0].indexOf(content)
    })
  }

  return blocks
}

const extractVueTemplateExpressions = (source: string) =>
  extractVueBlocks(source, 'template').flatMap(block => {
    const expressions: Array<{ content: string; offset: number }> = []
    const mustachePattern = /{{([\s\S]*?)}}/g
    const directivePattern = /(?:^|\s)(?:[@:][^\s=>/]+|v-[\w-]+(?::[^\s=>/]+)?(?:\.[^\s=>/]+)*)\s*=\s*(["'])([\s\S]*?)\1/g
    const eventHandlerPattern = /\son[a-z]+\s*=\s*(["'])([\s\S]*?)\1/gi

    collectRegexCaptures(block.content, block.offset, mustachePattern, 1, expressions)
    collectRegexCaptures(block.content, block.offset, directivePattern, 2, expressions)
    collectRegexCaptures(block.content, block.offset, eventHandlerPattern, 2, expressions)

    return expressions
  })

const collectRegexCaptures = (
  source: string,
  sourceOffset: number,
  pattern: RegExp,
  captureIndex: number,
  captures: Array<{ content: string; offset: number }>
) => {
  let match: RegExpExecArray | null

  while ((match = pattern.exec(source)) !== null) {
    const content = match[captureIndex]
    captures.push({
      content,
      offset: sourceOffset + match.index + match[0].indexOf(content)
    })
  }
}

const findNativeDialogCallsInSource = (
  filePath: string,
  source: string,
  originalSource: string,
  offset: number,
  scriptKind: ts.ScriptKind
): NativeDialogFinding[] => {
  const sourceFile = ts.createSourceFile(filePath, source, ts.ScriptTarget.Latest, true, scriptKind)
  const findings: NativeDialogFinding[] = []

  const visit = (node: ts.Node) => {
    if (ts.isCallExpression(node)) {
      const dialog = getNativeDialogName(node.expression)

      if (dialog) {
        const position = offset + node.expression.getStart(sourceFile)
        const location = getLineAndColumn(originalSource, position)

        findings.push({
          dialog,
          filePath,
          line: location.line,
          column: location.column,
          snippet: getLineSnippet(originalSource, position)
        })
      }
    }

    ts.forEachChild(node, visit)
  }

  visit(sourceFile)
  return findings
}

const getNativeDialogName = (expression: ts.Expression): string | null => {
  const unwrappedExpression = unwrapExpression(expression)

  if (ts.isIdentifier(unwrappedExpression) && nativeDialogs.has(unwrappedExpression.text)) {
    return unwrappedExpression.text
  }

  if (ts.isPropertyAccessExpression(unwrappedExpression)) {
    const dialog = unwrappedExpression.name.text
    const owner = unwrapExpression(unwrappedExpression.expression)

    if (nativeDialogs.has(dialog) && ts.isIdentifier(owner) && nativeDialogHosts.has(owner.text)) {
      return dialog
    }
  }

  if (ts.isElementAccessExpression(unwrappedExpression)) {
    const owner = unwrapExpression(unwrappedExpression.expression)
    const argument = unwrapExpression(unwrappedExpression.argumentExpression)
    const dialog = getStringLiteralText(argument)

    if (dialog && nativeDialogs.has(dialog) && ts.isIdentifier(owner) && nativeDialogHosts.has(owner.text)) {
      return dialog
    }
  }

  return null
}

const unwrapExpression = (expression: ts.Expression): ts.Expression => {
  let currentExpression = expression

  while (ts.isParenthesizedExpression(currentExpression) || ts.isNonNullExpression(currentExpression)) {
    currentExpression = currentExpression.expression
  }

  return currentExpression
}

const getStringLiteralText = (expression: ts.Expression) => {
  if (ts.isStringLiteral(expression) || ts.isNoSubstitutionTemplateLiteral(expression)) {
    return expression.text
  }

  return null
}

const getScriptKind = (filePath: string) => {
  if (filePath.endsWith('.jsx')) return ts.ScriptKind.JSX
  if (filePath.endsWith('.tsx')) return ts.ScriptKind.TSX
  if (filePath.endsWith('.js')) return ts.ScriptKind.JS
  return ts.ScriptKind.TS
}

const getLineAndColumn = (source: string, position: number) => {
  const linePrefix = source.slice(0, position)
  const lineBreaks = linePrefix.match(/\n/g)
  const lastLineBreak = linePrefix.lastIndexOf('\n')

  return {
    line: (lineBreaks?.length ?? 0) + 1,
    column: position - lastLineBreak
  }
}

const getLineSnippet = (source: string, position: number) => {
  const lineStart = source.lastIndexOf('\n', position) + 1
  const nextLineBreak = source.indexOf('\n', position)
  const lineEnd = nextLineBreak === -1 ? source.length : nextLineBreak

  return source.slice(lineStart, lineEnd).trim()
}

const findTextOccurrences = (source: string, text: string) => {
  const findings: Array<{ line: number; column: number; snippet: string }> = []
  let index = source.indexOf(text)

  while (index !== -1) {
    const location = getLineAndColumn(source, index)

    findings.push({
      line: location.line,
      column: location.column,
      snippet: getLineSnippet(source, index)
    })
    index = source.indexOf(text, index + text.length)
  }

  return findings
}

const formatFinding = (finding: NativeDialogFinding) =>
  `${finding.filePath.replace(/^\.\.\//, '')}:${finding.line}:${finding.column} uses native ${finding.dialog}(): ${finding.snippet}`

const normalizePath = (filePath: string) => filePath.replace(/\\/g, '/')

const findImportSpecifiers = (source: string) => {
  const imports: Array<{ modulePath: string; typeOnly: boolean }> = []
  const importPattern = /^\s*import\s+(type\s+)?(?:[\s\S]*?\s+from\s+)?['"]([^'"]+)['"]/gm
  let match: RegExpExecArray | null

  while ((match = importPattern.exec(source)) !== null) {
    imports.push({
      modulePath: match[2],
      typeOnly: Boolean(match[1])
    })
  }

  return imports
}

const findDynamicImportSpecifiers = (source: string) => {
  const imports: string[] = []
  const importPattern = /\bimport\(\s*['"]([^'"]+)['"]\s*\)/g
  let match: RegExpExecArray | null

  while ((match = importPattern.exec(source)) !== null) {
    imports.push(match[1])
  }

  return imports
}

const getSourceSection = (source: string, startMarker: string, endMarker: string) => {
  const startIndex = source.indexOf(startMarker)
  const endIndex = source.indexOf(endMarker, startIndex + startMarker.length)

  if (startIndex === -1 || endIndex === -1) {
    throw new Error(`Missing source section between "${startMarker}" and "${endMarker}"`)
  }

  return source.slice(startIndex, endIndex)
}

const getSourceModule = (filePath: string) => {
  const source = sourceModules[filePath]

  if (typeof source !== 'string') {
    throw new Error(`Missing source module for ${filePath}`)
  }

  return source
}
