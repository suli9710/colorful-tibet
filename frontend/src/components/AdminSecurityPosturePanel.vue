<template>
  <section class="overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
    <div class="flex flex-col gap-4 border-b border-stone-100 bg-gradient-to-r from-slate-50 via-white to-emerald-50 px-4 py-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between">
      <div class="flex min-w-0 items-center gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-lg" :class="statusTone.iconBg">
          <ShieldCheck class="h-5 w-5" :class="statusTone.iconText" />
        </div>
        <div class="min-w-0">
          <h3 class="text-lg font-bold text-stone-900 sm:text-xl">{{ t('admin.securityPosture.title') }}</h3>
          <p class="mt-1 text-sm text-stone-500">{{ t('admin.securityPosture.subtitle') }}</p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusTone.badge">
          {{ statusLabel }}
        </span>
        <button
          :disabled="loading"
          class="inline-flex items-center gap-1.5 rounded-lg bg-white px-3 py-2 text-xs font-medium text-stone-600 shadow-sm ring-1 ring-stone-200 transition-colors hover:bg-stone-50 disabled:opacity-60"
          @click="$emit('refresh')"
        >
          <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': loading }" />
          {{ loading ? t('common.loading') : t('common.refresh') }}
        </button>
      </div>
    </div>

    <div v-if="loading && !posture" class="p-8 text-center text-stone-500">
      <div class="mx-auto mb-3 h-10 w-10 animate-spin rounded-full border-b-2 border-emerald-500"></div>
      {{ t('admin.securityPosture.loading') }}
    </div>

    <div v-else-if="error && !posture" class="p-6 text-center text-sm text-red-600">
      {{ error }}
    </div>

    <div v-else-if="posture" class="space-y-5 p-4 sm:p-6">
      <div class="grid gap-4 lg:grid-cols-[220px_1fr]">
        <div class="rounded-lg border border-stone-100 bg-stone-50 p-4">
          <p class="text-sm font-semibold text-stone-600">{{ t('admin.securityPosture.score') }}</p>
          <div class="mt-3 flex items-end gap-2">
            <span class="text-4xl font-bold text-stone-900">{{ posture.score }}</span>
            <span class="pb-1 text-sm text-stone-400">/ 100</span>
          </div>
          <div class="mt-4 h-2 overflow-hidden rounded-full bg-white">
            <div class="h-full rounded-full transition-all" :class="statusTone.bar" :style="{ width: `${scoreWidth}%` }"></div>
          </div>
          <p class="mt-3 text-xs text-stone-500">
            {{ t('admin.securityPosture.updatedAt', { time: formatDateTime(posture.generatedAt) }) }}
          </p>
        </div>

        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <div v-for="group in capabilityGroups" :key="group.title" class="rounded-lg border border-stone-100 p-4">
            <div class="mb-3 flex items-center justify-between gap-2">
              <h4 class="text-sm font-semibold text-stone-800">{{ group.title }}</h4>
              <component :is="group.icon" class="h-4 w-4 text-stone-400" />
            </div>
            <div class="space-y-2">
              <div v-for="item in group.items" :key="item.label" class="flex items-center justify-between gap-3 text-sm">
                <span class="text-stone-500">{{ item.label }}</span>
                <span class="shrink-0 rounded-full px-2 py-0.5 text-xs font-medium" :class="pillClass(item.ok)">
                  {{ item.value }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-[1fr_360px]">
        <div class="rounded-lg border border-stone-100">
          <div class="border-b border-stone-100 px-4 py-3">
            <h4 class="text-sm font-semibold text-stone-800">{{ t('admin.securityPosture.findings') }}</h4>
          </div>
          <div class="divide-y divide-stone-100">
            <div v-for="finding in posture.findings" :key="finding.id" class="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div class="min-w-0">
                <p class="text-sm font-medium text-stone-800">{{ findingLabel(finding.id, finding.message) }}</p>
                <p class="mt-0.5 text-xs text-stone-400">{{ finding.id }}</p>
              </div>
              <span class="w-fit rounded-full px-2.5 py-1 text-xs font-semibold" :class="findingClass(finding.status)">
                {{ findingStatusLabel(finding.status) }}
              </span>
            </div>
          </div>
        </div>

        <div class="rounded-lg border border-stone-100 p-4">
          <h4 class="text-sm font-semibold text-stone-800">{{ t('admin.securityPosture.dependencies') }}</h4>
          <div class="mt-4 space-y-3">
            <div v-for="dependency in dependencies" :key="dependency.label" class="flex items-center justify-between rounded-lg bg-stone-50 px-3 py-2">
              <span class="text-sm text-stone-600">{{ dependency.label }}</span>
              <span class="rounded-full px-2 py-0.5 text-xs font-semibold" :class="dependencyClass(dependency.status)">
                {{ dependencyStatusLabel(dependency.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  FileWarning,
  LockKeyhole,
  RefreshCw,
  ShieldCheck,
  ShieldHalf,
  Server
} from 'lucide-vue-next'
import type { DependencyHealthStatus, SecurityFindingStatus, SecurityPostureResponse } from '../api'

const props = defineProps<{
  posture: SecurityPostureResponse | null
  loading: boolean
  error?: string
}>()

defineEmits<{
  refresh: []
}>()

const { t, te, locale } = useI18n()

const boolLabel = (value: boolean) => value ? t('common.enabled') : t('common.disabled')
const configuredLabel = (value: boolean) => value ? t('admin.securityPosture.configured') : t('admin.securityPosture.missing')

const statusTone = computed(() => {
  switch (props.posture?.status) {
    case 'BLOCKED':
      return {
        badge: 'bg-red-100 text-red-700',
        iconBg: 'bg-red-100',
        iconText: 'text-red-600',
        bar: 'bg-red-500'
      }
    case 'DEGRADED':
      return {
        badge: 'bg-amber-100 text-amber-700',
        iconBg: 'bg-amber-100',
        iconText: 'text-amber-600',
        bar: 'bg-amber-500'
      }
    default:
      return {
        badge: 'bg-emerald-100 text-emerald-700',
        iconBg: 'bg-emerald-100',
        iconText: 'text-emerald-600',
        bar: 'bg-emerald-500'
      }
  }
})

const statusLabel = computed(() => t(`admin.securityPosture.status.${props.posture?.status || 'READY'}`))
const scoreWidth = computed(() => Math.max(0, Math.min(100, props.posture?.score || 0)))

const capabilityGroups = computed(() => {
  const posture = props.posture
  if (!posture) return []
  return [
    {
      title: t('admin.securityPosture.groups.environment'),
      icon: Server,
      items: [
        { label: t('admin.securityPosture.items.activeProfiles'), value: posture.environment.activeProfiles.join(', ') || '-', ok: posture.environment.activeProfiles.length > 0 },
        { label: t('admin.securityPosture.items.strictSecrets'), value: boolLabel(posture.environment.strictSecretsRequired), ok: posture.environment.strictSecretsRequired }
      ]
    },
    {
      title: t('admin.securityPosture.groups.exposure'),
      icon: FileWarning,
      items: [
        { label: t('admin.securityPosture.items.publicDocs'), value: boolLabel(posture.exposure.publicDocsEnabled), ok: !posture.exposure.publicDocsEnabled },
        { label: t('admin.securityPosture.items.publicMetrics'), value: boolLabel(posture.exposure.publicMetricsEnabled), ok: !posture.exposure.publicMetricsEnabled },
        { label: t('admin.securityPosture.items.cors'), value: configuredLabel(posture.exposure.corsConfigured), ok: posture.exposure.corsConfigured }
      ]
    },
    {
      title: t('admin.securityPosture.groups.auth'),
      icon: LockKeyhole,
      items: [
        { label: t('admin.securityPosture.items.jwt'), value: configuredLabel(posture.authentication.jwtConfigured), ok: posture.authentication.jwtConfigured },
        { label: t('admin.securityPosture.items.csrf'), value: configuredLabel(posture.authentication.csrfConfigured), ok: posture.authentication.csrfConfigured },
        { label: t('admin.securityPosture.items.totp'), value: configuredLabel(posture.authentication.superAdminTotpConfigured), ok: posture.authentication.superAdminTotpConfigured },
        { label: t('admin.securityPosture.items.cookie'), value: posture.authentication.cookieSameSite, ok: posture.authentication.cookieSecure }
      ]
    },
    {
      title: t('admin.securityPosture.groups.protection'),
      icon: ShieldHalf,
      items: [
        { label: t('admin.securityPosture.items.rateLimit'), value: boolLabel(posture.protections.rateLimitEnabled), ok: posture.protections.rateLimitEnabled },
        { label: t('admin.securityPosture.items.bruteForce'), value: boolLabel(posture.protections.bruteForceEnabled), ok: posture.protections.bruteForceEnabled },
        { label: t('admin.securityPosture.items.antibot'), value: boolLabel(posture.protections.antibotEnabled), ok: posture.protections.antibotEnabled || posture.protections.recaptchaConfigured },
        { label: t('admin.securityPosture.items.pii'), value: configuredLabel(posture.dataProtection.piiKeysConfigured && posture.dataProtection.piiActiveKeyConfigured), ok: posture.dataProtection.piiKeysConfigured && posture.dataProtection.piiActiveKeyConfigured }
      ]
    }
  ]
})

const dependencies = computed(() => {
  const posture = props.posture
  if (!posture) return []
  return [
    { label: t('admin.securityPosture.dependency.database'), status: posture.dependencies.database },
    { label: t('admin.securityPosture.dependency.redis'), status: posture.dependencies.redis },
    { label: t('admin.securityPosture.dependency.scrapling'), status: posture.dependencies.scrapling },
    { label: t('admin.securityPosture.dependency.ai'), status: (posture.dependencies.aiProviderConfigured ? 'UP' : 'UNKNOWN') as DependencyHealthStatus },
    { label: t('admin.securityPosture.dependency.payment'), status: (posture.dependencies.paymentCallbackSecretConfigured ? 'UP' : 'UNKNOWN') as DependencyHealthStatus }
  ]
})

const pillClass = (ok: boolean) => ok ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'

const findingClass = (status: SecurityFindingStatus) => {
  if (status === 'PASS') return 'bg-emerald-100 text-emerald-700'
  if (status === 'INFO') return 'bg-blue-100 text-blue-700'
  if (status === 'WARN') return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
}

const dependencyClass = (status: DependencyHealthStatus) => {
  if (status === 'UP' || status === 'DISABLED') return 'bg-emerald-100 text-emerald-700'
  if (status === 'UNKNOWN') return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
}

const findingStatusLabel = (status: SecurityFindingStatus) => t(`admin.securityPosture.findingStatus.${status}`)
const dependencyStatusLabel = (status: DependencyHealthStatus) => t(`admin.securityPosture.dependencyStatus.${status}`)

const findingLabel = (id: string, fallback: string) => {
  const key = `admin.securityPosture.finding.${id}`
  return te(key) ? t(key) : fallback
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString(locale.value === 'bo' ? 'bo-CN' : 'zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
