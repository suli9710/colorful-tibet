<template>
  <div class="tibet-page-shell min-h-screen">
    <main class="mx-auto max-w-7xl px-4 pb-12 pt-24 sm:px-6 sm:pt-28 lg:px-8">
      <header class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="inline-flex items-center gap-2 rounded-full border border-tibet-gold/25 bg-white/80 px-3 py-1 text-xs font-semibold text-tibet-red">
            <ReceiptText class="h-3.5 w-3.5" />
            平台不收款
          </div>
          <h1 class="mt-4 text-2xl font-bold text-tibet-dark sm:text-4xl">咨询记录中心</h1>
          <p class="mt-2 max-w-2xl text-sm leading-6 text-tibet-brown/65">
            这里汇总景点、酒店和行程节点的咨询意向。实际购买、出票、入住和售后确认请以第三方有资质平台为准。
          </p>
        </div>

        <div class="flex flex-wrap gap-2">
          <router-link
            to="/route-planner"
            class="inline-flex items-center justify-center gap-2 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-semibold text-tibet-brown hover:bg-amber-50"
          >
            <Sparkles class="h-4 w-4" />
            继续规划
          </router-link>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow shadow-md shadow-tibet-red/20 disabled:opacity-50"
            :disabled="loading"
            @click="loadOrders(selectedOrderId)"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新
          </button>
        </div>
      </header>

      <section class="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="stat in stats"
          :key="stat.label"
          class="rounded-2xl border border-white/60 bg-white/80 px-4 py-3 shadow-sm"
        >
          <div class="flex items-center justify-between gap-3">
            <p class="text-xs font-semibold text-tibet-brown/55">{{ stat.label }}</p>
            <component :is="stat.icon" class="h-4 w-4 text-tibet-gold" />
          </div>
          <p class="mt-2 text-2xl font-bold text-tibet-dark">{{ stat.value }}</p>
          <p class="mt-1 text-xs text-tibet-brown/50">{{ stat.hint }}</p>
        </div>
      </section>

      <section class="mb-6 rounded-2xl border border-white/60 bg-white/75 p-3 shadow-sm">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="-mx-1 flex gap-1 overflow-x-auto px-1 pb-1 sm:mx-0 sm:px-0 sm:pb-0">
            <button
              v-for="tab in statusTabs"
              :key="tab.key"
              type="button"
              class="inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-semibold transition"
              :class="selectedTab === tab.key ? 'bg-tibet-dark text-white shadow-sm' : 'text-tibet-brown/65 hover:bg-white/80'"
              @click="selectedTab = tab.key"
            >
              {{ tab.label }}
              <span class="rounded-full px-1.5 py-0.5 text-[10px]" :class="selectedTab === tab.key ? 'bg-white/20' : 'bg-tibet-gold/10 text-tibet-brown/60'">
                {{ tab.count }}
              </span>
            </button>
          </div>

          <label class="relative block w-full lg:w-80">
            <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-tibet-brown/35" />
            <input
              v-model.trim="query"
              type="search"
              class="w-full rounded-xl border border-tibet-gold/20 bg-white/85 py-2.5 pl-9 pr-3 text-sm text-tibet-dark outline-none transition focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
              placeholder="搜索咨询编号、项目或凭证"
            >
          </label>
        </div>
      </section>

      <div v-if="errorMessage && orders.length" role="alert" class="mb-5 flex flex-col gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 sm:flex-row sm:items-center sm:justify-between">
        <span>{{ errorMessage }}</span>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 disabled:opacity-50"
          :disabled="loading"
          @click="loadOrders(selectedOrderId)"
        >
          <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': loading }" />
          重试
        </button>
      </div>
      <div v-if="statusMessage" role="status" aria-live="polite" class="mb-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
        {{ statusMessage }}
      </div>

      <div v-if="loading && !orders.length" class="rounded-3xl border border-white/60 bg-white/75 p-12 text-center text-tibet-brown/60">
        <div class="mx-auto mb-4 h-11 w-11 animate-spin rounded-full border-2 border-tibet-gold/30 border-b-tibet-gold"></div>
        <p class="text-sm font-semibold">正在加载咨询记录</p>
      </div>

      <div v-else-if="errorMessage && !orders.length" role="alert" class="rounded-3xl border border-rose-200 bg-rose-50 px-6 py-12 text-center shadow-sm">
        <PackageCheck class="mx-auto h-12 w-12 text-rose-500" />
        <h2 class="mt-4 text-xl font-bold text-rose-900">咨询记录加载失败</h2>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-rose-700">
          {{ errorMessage }}
        </p>
        <button
          type="button"
          class="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow disabled:opacity-50"
          :disabled="loading"
          @click="loadOrders(selectedOrderId)"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          重新加载
        </button>
      </div>

      <div v-else-if="!orders.length" class="rounded-3xl border border-white/60 bg-white/80 px-6 py-12 text-center shadow-sm">
        <PackageCheck class="mx-auto h-12 w-12 text-tibet-gold" />
        <h2 class="mt-4 text-xl font-bold text-tibet-dark">还没有咨询记录</h2>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-tibet-brown/60">
          从景点、酒店或 AI 行程页提交咨询后，这里会显示客服和第三方确认进度。
        </p>
        <div class="mt-6 flex flex-wrap justify-center gap-3">
          <router-link to="/route-planner" class="rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow">生成行程</router-link>
          <router-link to="/hotels" class="rounded-xl border border-tibet-gold/25 bg-white px-4 py-2.5 text-sm font-semibold text-tibet-brown">浏览酒店</router-link>
        </div>
      </div>

      <div
        v-else
        class="grid gap-6"
        :class="selectedOrder ? 'lg:grid-cols-[minmax(0,1.03fr)_minmax(360px,0.97fr)]' : 'lg:grid-cols-1'"
      >
        <section class="space-y-3">
          <article
            v-for="order in filteredOrders"
            :key="order.id"
            role="button"
            tabindex="0"
            :aria-label="orderCardActionLabel(order)"
            :aria-expanded="selectedOrderId === order.id"
            aria-controls="order-detail-panel"
            class="cursor-pointer rounded-2xl border bg-white/80 p-4 shadow-sm transition focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2"
            :class="selectedOrderId === order.id ? 'border-tibet-red/35 ring-2 ring-tibet-red/10' : 'border-white/65 hover:border-tibet-gold/30 hover:bg-white/90'"
            @click="selectOrder(order.id)"
            @keydown.enter.prevent="selectOrder(order.id)"
            @keydown.space.prevent="selectOrder(order.id)"
          >
            <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold" :class="statusMeta(order.status).className">
                    {{ statusMeta(order.status).label }}
                  </span>
                  <span class="text-xs text-tibet-brown/45">{{ order.orderNo }}</span>
                </div>
                <h2 class="mt-2 truncate text-base font-bold text-tibet-dark">{{ order.productSummary || '旅行咨询' }}</h2>
                <p class="mt-1 line-clamp-2 text-sm text-tibet-brown/58">{{ itemSummary(order) }}</p>
              </div>

              <div class="shrink-0 text-left sm:text-right">
                <p class="text-xs text-tibet-brown/45">参考价</p>
                <p class="text-lg font-bold tabular-nums text-tibet-dark">{{ formatCurrency(order.payableAmount, order.currency) }}</p>
                <p class="mt-1 text-xs font-medium" :class="confirmationMeta(order.paymentStatus).textClass">
                  {{ confirmationMeta(order.paymentStatus).label }}
                </p>
              </div>
            </div>

            <div class="mt-4 grid gap-2 text-xs text-tibet-brown/55 sm:grid-cols-3">
              <span class="inline-flex items-center gap-1.5">
                <CalendarDays class="h-3.5 w-3.5 text-tibet-gold" />
                {{ formatDate(order.createdAt) }}
              </span>
              <span class="inline-flex items-center gap-1.5">
                <TicketCheck class="h-3.5 w-3.5 text-tibet-gold" />
                {{ order.vouchers?.length || 0 }} 张凭证
              </span>
              <span class="inline-flex items-center gap-1.5">
                <FileText class="h-3.5 w-3.5 text-tibet-gold" />
                {{ thirdPartyConfirmations(order).length }} 条第三方记录
              </span>
            </div>
          </article>

          <div v-if="!filteredOrders.length" class="rounded-2xl border border-white/60 bg-white/75 p-8 text-center text-sm text-tibet-brown/60">
            <p>当前筛选下没有咨询记录。</p>
            <button
              v-if="selectedTab !== 'ALL' || query"
              type="button"
              class="mt-4 rounded-xl border border-tibet-gold/25 bg-white px-4 py-2 text-sm font-semibold text-tibet-brown transition hover:bg-amber-50"
              @click="resetFilters"
            >
              清除筛选
            </button>
          </div>
        </section>

        <aside v-if="selectedOrder" id="order-detail-panel" class="min-w-0 lg:sticky lg:top-28">
          <section class="overflow-hidden rounded-3xl border border-white/65 bg-white/85 shadow-lg shadow-slate-900/5">
            <div class="border-b border-tibet-gold/10 bg-white/55 px-5 py-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div class="min-w-0">
                  <p class="text-xs font-semibold text-tibet-brown/45">{{ selectedOrder.orderNo }}</p>
                  <h2 class="mt-1 text-xl font-bold text-tibet-dark">{{ selectedOrder.productSummary || '旅行咨询' }}</h2>
                </div>
                <button
                  type="button"
                  class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-tibet-gold/20 bg-white/80 text-tibet-brown transition hover:bg-amber-50"
                  aria-label="关闭咨询详情"
                  @click="closeOrderDetail"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>

              <div class="mt-4 grid gap-3 sm:grid-cols-3">
                <div>
                  <p class="text-xs text-tibet-brown/45">参考价</p>
                  <p class="mt-1 text-lg font-bold tabular-nums text-tibet-dark">{{ formatCurrency(selectedOrder.payableAmount, selectedOrder.currency) }}</p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">确认状态</p>
                  <p class="mt-1 text-sm font-semibold" :class="confirmationMeta(selectedOrder.paymentStatus).textClass">
                    {{ confirmationMeta(selectedOrder.paymentStatus).label }}
                  </p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">创建时间</p>
                  <p class="mt-1 text-sm font-semibold text-tibet-dark">{{ formatDate(selectedOrder.createdAt) }}</p>
                </div>
              </div>
            </div>

            <div class="divide-y divide-tibet-gold/10">
              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">咨询项目</h3>
                <div class="space-y-3">
                  <div v-for="item in selectedOrder.items" :key="item.id" class="flex flex-col gap-2 sm:flex-row sm:gap-3">
                    <span class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-tibet-gold/10 text-tibet-gold">
                      <component :is="itemIcon(item.productType)" class="h-4 w-4" />
                    </span>
                    <div class="min-w-0 flex-1">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="font-semibold text-tibet-dark">{{ item.productName }}</p>
                        <span class="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] text-gray-600">{{ itemTypeLabel(item.productType) }}</span>
                      </div>
                      <p class="mt-1 text-xs text-tibet-brown/55">
                        {{ item.skuName || '标准咨询' }} · {{ formatServiceDate(item) }} · 数量 {{ item.quantity || 1 }}
                      </p>
                    </div>
                    <p class="shrink-0 text-sm font-bold tabular-nums text-tibet-dark sm:text-right">{{ formatCurrency(item.subtotal, selectedOrder.currency) }}</p>
                  </div>
                </div>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">凭证与确认</h3>
                <div v-if="selectedOrder.vouchers?.length" class="space-y-2">
                  <div v-for="voucher in selectedOrder.vouchers" :key="voucher.id" class="flex items-center justify-between gap-3 rounded-xl bg-emerald-50/75 px-3 py-2">
                    <div class="min-w-0">
                      <p class="truncate text-sm font-bold text-emerald-800">{{ voucher.voucherCode }}</p>
                      <p class="text-xs text-emerald-700/65">{{ voucher.validFrom || '待确认' }} 至 {{ voucher.validUntil || '待确认' }}</p>
                    </div>
                    <span class="rounded-full bg-white/75 px-2 py-1 text-[11px] font-semibold text-emerald-700">{{ voucher.status }}</span>
                  </div>
                </div>
                <p v-else class="text-sm text-tibet-brown/55">暂无凭证。平台不出票，凭证以第三方平台为准。</p>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">第三方记录</h3>
                <div class="space-y-2">
                  <div v-for="confirmation in thirdPartyConfirmations(selectedOrder)" :key="confirmation.id" class="flex items-center justify-between gap-3 text-sm">
                    <span class="text-tibet-brown/60">{{ confirmation.provider }} · {{ confirmation.status }}</span>
                    <span class="font-semibold text-tibet-dark">{{ formatCurrency(confirmation.amount, selectedOrder.currency) }}</span>
                  </div>
                  <p v-if="!thirdPartyConfirmations(selectedOrder).length" class="text-sm text-tibet-brown/55">
                    暂无第三方确认。请以跳转平台或客服反馈为准。
                  </p>
                </div>
              </section>

              <section class="px-5 py-4">
                <p class="mb-4 rounded-2xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
                  本平台只保存咨询意向和导流记录，不处理站内收款、资金托管、出票或票据服务。
                </p>
                <div class="grid gap-2 sm:grid-cols-2">
                  <button
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40"
                    :class="cancelActionOpen ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-tibet-gold/20 bg-white/75 text-tibet-brown hover:bg-amber-50'"
                    :disabled="!canCancel(selectedOrder)"
                    @click="beginCancelAction"
                  >
                    <Ban class="h-4 w-4" />
                    取消咨询
                  </button>
                  <button
                    v-if="canDelete(selectedOrder)"
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="deletingOrderId === selectedOrder.id"
                    @click="deleteClosedOrder(selectedOrder)"
                  >
                    <Trash2 class="h-4 w-4" />
                    {{ deletingOrderId === selectedOrder.id ? '删除中' : '删除记录' }}
                  </button>
                </div>

                <form v-if="cancelActionOpen" class="mt-4 rounded-2xl border border-tibet-gold/15 bg-white/75 p-4" @submit.prevent="submitCancelAction">
                  <label class="block">
                    <span class="mb-1 block text-xs font-semibold text-tibet-brown/60">取消原因</span>
                    <textarea
                      v-model.trim="actionReason"
                      rows="3"
                      class="w-full resize-none rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                      placeholder="可选，便于客服处理"
                    ></textarea>
                  </label>
                  <p v-if="actionError" role="alert" class="mt-3 text-xs font-medium text-rose-600">{{ actionError }}</p>
                  <div class="mt-4 flex justify-end gap-2">
                    <button type="button" class="rounded-xl px-3 py-2 text-sm font-semibold text-tibet-brown/60 hover:bg-gray-100" @click="resetActionForm">收起</button>
                    <button type="submit" class="rounded-xl bg-tibet-dark px-4 py-2 text-sm font-semibold text-white disabled:opacity-50" :disabled="actionLoading">
                      {{ actionLoading ? '处理中' : '确认取消' }}
                    </button>
                  </div>
                </form>
              </section>
            </div>
          </section>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Ban,
  CalendarDays,
  FileText,
  Hotel,
  Landmark,
  PackageCheck,
  ReceiptText,
  RefreshCw,
  Search,
  Sparkles,
  TicketCheck,
  Trash2,
  X
} from 'lucide-vue-next'
import api, { endpoints } from '../api'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'

interface OrderItem {
  id: number
  productType: string
  productName: string
  skuName?: string | null
  serviceStartDate?: string | null
  serviceEndDate?: string | null
  quantity?: number | null
  subtotal?: number | string | null
}

interface Voucher {
  id: number
  voucherCode: string
  status: string
  validFrom?: string | null
  validUntil?: string | null
}

interface ThirdPartyConfirmation {
  id: number
  provider: string
  amount?: number | string | null
  status: string
}

interface Order {
  id: number
  orderNo: string
  status: string
  paymentStatus: string
  currency?: string | null
  productSummary?: string | null
  payableAmount?: number | string | null
  createdAt?: string | null
  items: OrderItem[]
  paymentTransactions?: ThirdPartyConfirmation[] | null
  vouchers: Voucher[]
}

const router = useRouter()
const { t } = useI18n()
const auth = useAuthStore()
const { showConfirm } = useConfirm()
const { showToast } = useToast()

const orders = ref<Order[]>([])
const selectedOrderId = ref<number | null>(null)
const selectedTab = ref('ALL')
const query = ref('')
const loading = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const cancelActionOpen = ref(false)
const deletingOrderId = ref<number | null>(null)
const actionLoading = ref(false)
const actionError = ref('')
const actionReason = ref('')

const statusLabels: Record<string, { label: string; className: string }> = {
  PENDING_PAYMENT: { label: '待确认', className: 'border-amber-200 bg-amber-50 text-amber-700' },
  PAID: { label: '第三方确认', className: 'border-sky-200 bg-sky-50 text-sky-700' },
  CONFIRMED: { label: '已确认', className: 'border-emerald-200 bg-emerald-50 text-emerald-700' },
  CANCELLED: { label: '已取消', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  REFUND_PENDING: { label: '售后沟通中', className: 'border-rose-200 bg-rose-50 text-rose-700' },
  REFUNDED: { label: '已关闭', className: 'border-purple-200 bg-purple-50 text-purple-700' },
  EXPIRED: { label: '已过期', className: 'border-gray-200 bg-gray-50 text-gray-500' }
}

const confirmationLabels: Record<string, { label: string; textClass: string }> = {
  UNPAID: { label: '待第三方确认', textClass: 'text-amber-700' },
  PAID: { label: '第三方已确认', textClass: 'text-emerald-700' },
  PARTIALLY_REFUNDED: { label: '售后沟通中', textClass: 'text-rose-700' },
  REFUNDED: { label: '已关闭', textClass: 'text-purple-700' },
  FAILED: { label: '无站内收款', textClass: 'text-rose-700' }
}

const statusMeta = (status?: string) =>
  statusLabels[status || ''] || { label: status || '未知', className: 'border-gray-200 bg-gray-50 text-gray-600' }

const confirmationMeta = (status?: string) =>
  confirmationLabels[status || ''] || { label: status || '未知状态', textClass: 'text-tibet-brown/65' }

const thirdPartyConfirmations = (order: Order) => order.paymentTransactions || []

const selectedOrder = computed(() => filteredOrders.value.find(order => order.id === selectedOrderId.value) || null)

const statusBucket = (order: Order) => {
  if (['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status)) return 'CLOSED'
  return order.status === 'PAID' ? 'CONFIRMED' : order.status
}

const countByBucket = (bucket: string) =>
  bucket === 'ALL'
    ? orders.value.length
    : orders.value.filter(order => statusBucket(order) === bucket).length

const statusTabs = computed(() => [
  { key: 'ALL', label: '全部', count: countByBucket('ALL') },
  { key: 'PENDING_PAYMENT', label: '待确认', count: countByBucket('PENDING_PAYMENT') },
  { key: 'CONFIRMED', label: '已确认', count: countByBucket('CONFIRMED') },
  { key: 'REFUND_PENDING', label: '售后中', count: countByBucket('REFUND_PENDING') },
  { key: 'CLOSED', label: '已关闭', count: countByBucket('CLOSED') }
])

const filteredOrders = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return orders.value.filter(order => {
    const matchesStatus = selectedTab.value === 'ALL' || statusBucket(order) === selectedTab.value
    if (!matchesStatus) return false
    if (!keyword) return true

    const searchable = [
      order.orderNo,
      order.productSummary,
      ...order.items.map(item => `${item.productName} ${item.skuName || ''}`),
      ...order.vouchers.map(voucher => voucher.voucherCode)
    ].join(' ').toLowerCase()
    return searchable.includes(keyword)
  })
})

const stats = computed(() => {
  const voucherCount = orders.value.reduce((sum, order) => sum + (order.vouchers?.length || 0), 0)
  const confirmationCount = orders.value.reduce((sum, order) => sum + thirdPartyConfirmations(order).length, 0)
  return [
    { label: '全部咨询', value: String(orders.value.length), hint: '景点、酒店和行程节点', icon: ReceiptText },
    { label: '待确认', value: String(countByBucket('PENDING_PAYMENT')), hint: '客服或第三方待确认', icon: PackageCheck },
    { label: '已确认', value: String(countByBucket('CONFIRMED')), hint: '第三方或客服已反馈', icon: TicketCheck },
    { label: '履约记录', value: `${voucherCount}/${confirmationCount}`, hint: '凭证 / 第三方记录', icon: FileText }
  ]
})

const loadOrders = async (preferredId?: number | null) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await api.get(endpoints.orders.my, { params: { _ts: Date.now() } })
    orders.value = Array.isArray(data) ? data : []
    const nextId = preferredId ?? selectedOrderId.value
    selectedOrderId.value = orders.value.some(order => order.id === nextId) ? nextId : null
    if (!selectedOrderId.value) resetActionForm()
  } catch (error: any) {
    console.error('Failed to load consultation records:', summarizeClientError(error))
    errorMessage.value = safeClientErrorMessage(error, '咨询记录加载失败')
  } finally {
    loading.value = false
  }
}

const selectOrder = (id: number) => {
  selectedOrderId.value = id
  resetActionForm()
}

const closeOrderDetail = () => {
  selectedOrderId.value = null
  resetActionForm()
}

const resetFilters = () => {
  selectedTab.value = 'ALL'
  query.value = ''
}

const beginCancelAction = () => {
  cancelActionOpen.value = !cancelActionOpen.value
  actionError.value = ''
  actionReason.value = ''
}

const resetActionForm = () => {
  cancelActionOpen.value = false
  actionError.value = ''
  actionReason.value = ''
}

const submitCancelAction = async () => {
  if (!selectedOrder.value) return
  actionLoading.value = true
  actionError.value = ''
  statusMessage.value = ''
  try {
    const order = selectedOrder.value
    await api.post(endpoints.orders.cancel(order.id), { reason: actionReason.value || undefined })
    statusMessage.value = '取消申请已提交。'
    resetActionForm()
    await loadOrders(order.id)
  } catch (error: any) {
    console.error('Consultation cancel failed:', summarizeClientError(error))
    actionError.value = safeClientErrorMessage(error, '操作失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}

const canCancel = (order: Order) => ['PENDING_PAYMENT', 'PAID', 'CONFIRMED'].includes(order.status)
const canDelete = (order: Order) => ['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status)

const deleteClosedOrder = async (order: Order) => {
  if (!canDelete(order) || deletingOrderId.value) return
  const confirmed = await showConfirm({
    message: `确定删除咨询记录 ${order.orderNo}？删除后列表中将不再显示。`,
    confirmLabel: t('common.delete'),
    cancelLabel: t('common.cancel'),
    tone: 'danger'
  })
  if (!confirmed) return

  deletingOrderId.value = order.id
  errorMessage.value = ''
  statusMessage.value = ''
  try {
    await api.delete(endpoints.orders.delete(order.id))
    orders.value = orders.value.filter(item => item.id !== order.id)
    if (selectedOrderId.value === order.id) closeOrderDetail()
    statusMessage.value = '已删除关闭的咨询记录。'
    showToast(statusMessage.value, 'success')
  } catch (error: any) {
    console.error('Failed to delete consultation record:', summarizeClientError(error))
    errorMessage.value = safeClientErrorMessage(error, '咨询记录删除失败，请稍后重试')
    showToast(errorMessage.value, 'error')
  } finally {
    deletingOrderId.value = null
  }
}

const formatCurrency = (value?: number | string | null, currency?: string | null) => {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: currency || 'CNY',
    maximumFractionDigits: amount % 1 === 0 ? 0 : 2
  }).format(Number.isFinite(amount) ? amount : 0)
}

const formatDate = (value?: string | null) => {
  if (!value) return '待确认'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const formatServiceDate = (item: OrderItem) => {
  if (!item.serviceStartDate) return '日期待确认'
  if (!item.serviceEndDate || item.serviceEndDate === item.serviceStartDate) return item.serviceStartDate
  return `${item.serviceStartDate} 至 ${item.serviceEndDate}`
}

const itemTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    SCENIC_SPOT: '景点门票',
    HOTEL_ROOM: '酒店房型',
    ROUTE_PACKAGE: '旅行线路'
  }
  return labels[type] || type
}

const itemIcon = (type: string) => {
  if (type === 'HOTEL_ROOM') return Hotel
  if (type === 'SCENIC_SPOT') return Landmark
  return PackageCheck
}

const itemSummary = (order: Order) => {
  if (!order.items?.length) return '暂无项目明细'
  return order.items
    .slice(0, 3)
    .map(item => `${item.productName}${item.skuName ? ` - ${item.skuName}` : ''}`)
    .join(' / ')
}

const orderCardActionLabel = (order: Order) =>
  `查看咨询详情：${order.productSummary || order.orderNo}`

onMounted(async () => {
  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }
  await loadOrders()
})
</script>
