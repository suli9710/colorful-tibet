<template>
  <div class="tibet-page-shell min-h-screen">
    <main class="mx-auto max-w-7xl px-4 pb-12 pt-24 sm:px-6 sm:pt-28 lg:px-8">
      <header class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div class="inline-flex items-center gap-2 rounded-full border border-tibet-gold/25 bg-white/80 px-3 py-1 text-xs font-semibold text-tibet-red">
            <ReceiptText class="h-3.5 w-3.5" />
            {{ t('orderCenter.noPlatformPaymentBadge') }}
          </div>
          <h1 class="mt-4 text-2xl font-bold text-tibet-dark sm:text-4xl">{{ t('orderCenter.title') }}</h1>
          <p class="mt-2 max-w-2xl text-sm leading-6 text-tibet-brown/65">
            {{ t('orderCenter.subtitle') }}
          </p>
        </div>

        <div class="flex flex-wrap gap-2">
          <router-link
            to="/route-planner"
            class="inline-flex items-center justify-center gap-2 rounded-xl border border-tibet-gold/25 bg-white/80 px-4 py-2.5 text-sm font-semibold text-tibet-brown hover:bg-amber-50"
          >
            <Sparkles class="h-4 w-4" />
            {{ t('orderCenter.continuePlanning') }}
          </router-link>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow shadow-md shadow-tibet-red/20 disabled:opacity-50"
            :disabled="loading"
            :aria-busy="loading"
            @click="loadOrders(selectedOrderId)"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
            {{ t('common.refresh') }}
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
              :aria-pressed="selectedTab === tab.key"
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
              :placeholder="t('orderCenter.searchPlaceholder')"
              :aria-label="t('orderCenter.searchPlaceholder')"
            >
          </label>
        </div>
      </section>

      <div v-if="errorMessage && orders.length" role="alert" aria-live="assertive" aria-atomic="true" class="mb-5 flex flex-col gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 sm:flex-row sm:items-center sm:justify-between">
        <span>{{ errorMessage }}</span>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 disabled:opacity-50"
          :disabled="loading"
          :aria-busy="loading"
          @click="loadOrders(selectedOrderId)"
        >
          <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': loading }" />
          {{ t('common.retry') }}
        </button>
      </div>
      <div v-if="statusMessage" role="status" aria-live="polite" class="mb-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
        {{ statusMessage }}
      </div>

      <div
        v-if="loading && !orders.length"
        class="rounded-3xl border border-white/60 bg-white/75 p-12 text-center text-tibet-brown/60"
        role="status"
        aria-live="polite"
        aria-busy="true"
        aria-atomic="true"
      >
        <div class="mx-auto mb-4 h-11 w-11 animate-spin rounded-full border-2 border-tibet-gold/30 border-b-tibet-gold"></div>
        <p class="text-sm font-semibold">{{ t('orderCenter.loading') }}</p>
      </div>

      <div v-else-if="errorMessage && !orders.length" role="alert" aria-live="assertive" aria-atomic="true" class="rounded-3xl border border-rose-200 bg-rose-50 px-6 py-12 text-center shadow-sm">
        <PackageCheck class="mx-auto h-12 w-12 text-rose-500" />
        <h2 class="mt-4 text-xl font-bold text-rose-900">{{ t('orderCenter.loadErrorTitle') }}</h2>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-rose-700">
          {{ errorMessage }}
        </p>
        <button
          type="button"
          class="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow disabled:opacity-50"
          :disabled="loading"
          :aria-busy="loading"
          @click="loadOrders(selectedOrderId)"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          {{ t('common.reload') }}
        </button>
      </div>

      <div v-else-if="!orders.length" class="rounded-3xl border border-white/60 bg-white/80 px-6 py-12 text-center shadow-sm">
        <PackageCheck class="mx-auto h-12 w-12 text-tibet-gold" />
        <h2 class="mt-4 text-xl font-bold text-tibet-dark">{{ t('orderCenter.emptyTitle') }}</h2>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-tibet-brown/60">
          {{ t('orderCenter.emptyDescription') }}
        </p>
        <div class="mt-6 flex flex-wrap justify-center gap-3">
          <router-link to="/route-planner" class="rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow">{{ t('orderCenter.createItinerary') }}</router-link>
          <router-link to="/hotels" class="rounded-xl border border-tibet-gold/25 bg-white px-4 py-2.5 text-sm font-semibold text-tibet-brown">{{ t('orderCenter.browseHotels') }}</router-link>
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
                <h2 class="mt-2 truncate text-base font-bold text-tibet-dark">{{ order.productSummary || t('orderCenter.defaultProductSummary') }}</h2>
                <p class="mt-1 line-clamp-2 text-sm text-tibet-brown/58">{{ itemSummary(order) }}</p>
              </div>

              <div class="shrink-0 text-left sm:text-right">
                <p class="text-xs text-tibet-brown/45">{{ t('orderCenter.referencePrice') }}</p>
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
                {{ voucherSummaryLabel(order) }}
              </span>
              <span class="inline-flex items-center gap-1.5">
                <FileText class="h-3.5 w-3.5 text-tibet-gold" />
                {{ confirmationSummaryLabel(order) }}
              </span>
            </div>
          </article>

          <div v-if="!filteredOrders.length" class="rounded-2xl border border-white/60 bg-white/75 p-8 text-center text-sm text-tibet-brown/60">
            <p>{{ t('orderCenter.noFilteredResults') }}</p>
            <button
              v-if="selectedTab !== 'ALL' || query"
              type="button"
              class="mt-4 rounded-xl border border-tibet-gold/25 bg-white px-4 py-2 text-sm font-semibold text-tibet-brown transition hover:bg-amber-50"
              @click="resetFilters"
            >
              {{ t('orderCenter.clearFilters') }}
            </button>
          </div>

          <div
            v-if="ordersTotalPages > 1"
            class="mt-5 flex flex-wrap items-center justify-center gap-3"
            role="navigation"
            :aria-label="t('orderCenter.paginationLabel')"
          >
            <span class="text-sm text-tibet-brown/55" role="status" aria-live="polite">
              {{ ordersPage + 1 }} / {{ ordersTotalPages }}
            </span>
            <button
              type="button"
              class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-semibold text-tibet-brown transition hover:bg-amber-50 disabled:cursor-wait disabled:opacity-50"
              :disabled="loadingMoreOrders || !hasMoreOrders"
              :aria-busy="loadingMoreOrders"
              @click="loadNextOrdersPage"
            >
              {{ loadingMoreOrders ? t('common.loading') : t('community.nextPage') }}
            </button>
          </div>
        </section>

        <aside v-if="selectedOrder" id="order-detail-panel" class="min-w-0 lg:sticky lg:top-28">
          <section class="overflow-hidden rounded-3xl border border-white/65 bg-white/85 shadow-lg shadow-slate-900/5">
            <div class="border-b border-tibet-gold/10 bg-white/55 px-5 py-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div class="min-w-0">
                  <p class="text-xs font-semibold text-tibet-brown/45">{{ selectedOrder.orderNo }}</p>
                  <h2 class="mt-1 text-xl font-bold text-tibet-dark">{{ selectedOrder.productSummary || t('orderCenter.defaultProductSummary') }}</h2>
                </div>
                <button
                  type="button"
                  class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-tibet-gold/20 bg-white/80 text-tibet-brown transition hover:bg-amber-50"
                  :aria-label="t('orderCenter.closeDetail')"
                  @click="closeOrderDetail"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>

              <div class="mt-4 grid gap-3 sm:grid-cols-3">
                <div>
                  <p class="text-xs text-tibet-brown/45">{{ t('orderCenter.referencePrice') }}</p>
                  <p class="mt-1 text-lg font-bold tabular-nums text-tibet-dark">{{ formatCurrency(selectedOrder.payableAmount, selectedOrder.currency) }}</p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">{{ t('orderCenter.confirmationStatus') }}</p>
                  <p class="mt-1 text-sm font-semibold" :class="confirmationMeta(selectedOrder.paymentStatus).textClass">
                    {{ confirmationMeta(selectedOrder.paymentStatus).label }}
                  </p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">{{ t('orderCenter.createdAt') }}</p>
                  <p class="mt-1 text-sm font-semibold text-tibet-dark">{{ formatDate(selectedOrder.createdAt) }}</p>
                </div>
              </div>
            </div>

            <div
              v-if="detailLoading"
              class="px-5 py-8 text-center text-sm font-semibold text-tibet-brown/55"
              role="status"
              aria-live="polite"
            >
              <div class="mx-auto mb-3 h-8 w-8 animate-spin rounded-full border-2 border-tibet-gold/30 border-b-tibet-gold"></div>
              {{ t('orderCenter.detailLoading') }}
            </div>
            <div v-else-if="detailErrorMessage" role="alert" aria-live="assertive" aria-atomic="true" class="px-5 py-6 text-center">
              <p class="text-sm font-medium text-rose-700">{{ detailErrorMessage }}</p>
              <button
                type="button"
                class="mt-4 inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 py-2 text-xs font-semibold text-rose-700 transition hover:bg-rose-100"
                :disabled="detailLoading"
                :aria-busy="detailLoading"
                @click="loadOrderDetail(selectedOrder.id, true)"
              >
                <RefreshCw class="h-3.5 w-3.5" />
                {{ t('orderCenter.reloadDetail') }}
              </button>
            </div>
            <div v-else class="divide-y divide-tibet-gold/10">
              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">{{ t('orderCenter.itemsTitle') }}</h3>
                <div class="space-y-3">
                  <div v-for="item in orderItems(selectedOrder)" :key="item.id" class="flex flex-col gap-2 sm:flex-row sm:gap-3">
                    <span class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-tibet-gold/10 text-tibet-gold">
                      <component :is="itemIcon(item.productType)" class="h-4 w-4" />
                    </span>
                    <div class="min-w-0 flex-1">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="font-semibold text-tibet-dark">{{ item.productName }}</p>
                        <span class="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] text-gray-600">{{ itemTypeLabel(item.productType) }}</span>
                      </div>
                      <p class="mt-1 text-xs text-tibet-brown/55">
                        {{ item.skuName || t('orderCenter.standardConsultation') }} · {{ formatServiceDate(item) }} · {{ t('orderCenter.quantity', { count: item.quantity || 1 }) }}
                      </p>
                    </div>
                    <p class="shrink-0 text-sm font-bold tabular-nums text-tibet-dark sm:text-right">{{ formatCurrency(item.subtotal, selectedOrder.currency) }}</p>
                  </div>
                  <p v-if="!orderItems(selectedOrder).length" class="text-sm text-tibet-brown/55">
                    {{ t('orderCenter.noItemDetails') }}
                  </p>
                </div>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">{{ t('orderCenter.vouchersTitle') }}</h3>
                <div v-if="orderVouchers(selectedOrder).length" class="space-y-2">
                  <div v-for="voucher in orderVouchers(selectedOrder)" :key="voucher.id" class="flex items-center justify-between gap-3 rounded-xl bg-emerald-50/75 px-3 py-2">
                    <div class="min-w-0">
                      <p class="truncate text-sm font-bold text-emerald-800">{{ voucher.voucherCode }}</p>
                      <p class="text-xs text-emerald-700/65">{{ formatVoucherDate(voucher.validFrom) }} {{ t('orderCenter.dateRangeSeparator') }} {{ formatVoucherDate(voucher.validUntil) }}</p>
                    </div>
                    <span class="rounded-full bg-white/75 px-2 py-1 text-[11px] font-semibold text-emerald-700">{{ voucherStatusLabel(voucher.status) }}</span>
                  </div>
                </div>
                <p v-else class="text-sm text-tibet-brown/55">{{ t('orderCenter.noVouchers') }}</p>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">{{ t('orderCenter.thirdPartyTitle') }}</h3>
                <div class="space-y-2">
                  <div v-for="confirmation in thirdPartyConfirmations(selectedOrder)" :key="confirmation.id" class="flex items-center justify-between gap-3 text-sm">
                    <span class="text-tibet-brown/60">{{ confirmation.provider }} · {{ transactionStatusLabel(confirmation.status) }}</span>
                    <span class="font-semibold text-tibet-dark">{{ formatCurrency(confirmation.amount, selectedOrder.currency) }}</span>
                  </div>
                  <p v-if="!thirdPartyConfirmations(selectedOrder).length" class="text-sm text-tibet-brown/55">
                    {{ t('orderCenter.noThirdPartyConfirmations') }}
                  </p>
                </div>
              </section>

              <section class="px-5 py-4">
                <p class="mb-4 rounded-2xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
                  {{ t('orderCenter.platformDisclaimer') }}
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
                    {{ t('orderCenter.cancelConsultation') }}
                  </button>
                  <button
                    v-if="canDelete(selectedOrder)"
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="deletingOrderId === selectedOrder.id"
                    @click="deleteClosedOrder(selectedOrder)"
                  >
                    <Trash2 class="h-4 w-4" />
                    {{ deletingOrderId === selectedOrder.id ? t('orderCenter.deleting') : t('orderCenter.deleteRecord') }}
                  </button>
                </div>

                <form v-if="cancelActionOpen" class="mt-4 rounded-2xl border border-tibet-gold/15 bg-white/75 p-4" @submit.prevent="submitCancelAction">
                  <label class="block">
                    <span class="mb-1 block text-xs font-semibold text-tibet-brown/60">{{ t('orderCenter.cancelReason') }}</span>
                    <textarea
                      v-model.trim="actionReason"
                      rows="3"
                      class="w-full resize-none rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                      :placeholder="t('orderCenter.cancelReasonPlaceholder')"
                    ></textarea>
                  </label>
                  <p v-if="actionError" role="alert" class="mt-3 text-xs font-medium text-rose-600">{{ actionError }}</p>
                  <div class="mt-4 flex justify-end gap-2">
                    <button type="button" class="rounded-xl px-3 py-2 text-sm font-semibold text-tibet-brown/60 hover:bg-gray-100" @click="resetActionForm">{{ t('orderCenter.collapse') }}</button>
                    <button type="submit" class="rounded-xl bg-tibet-dark px-4 py-2 text-sm font-semibold text-white disabled:opacity-50" :disabled="actionLoading">
                      {{ actionLoading ? t('orderCenter.processing') : t('orderCenter.confirmCancel') }}
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
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata, type PaginatedHttpResponse } from '../api/endpoints'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import { useAuthStore } from '../stores/auth'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
import { toFiniteAmount, toIntlLocale } from '../i18n/formatting'

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
  items?: OrderItem[] | null
  paymentTransactions?: ThirdPartyConfirmation[] | null
  vouchers?: Voucher[] | null
}

const router = useRouter()
const { t, locale } = useI18n()
const auth = useAuthStore()
const { showConfirm } = useConfirm()
const { showToast } = useToast()

const ordersPageSize = 20
const orders = ref<Order[]>([])
const ordersPageInfo = ref<PageMetadata>({
  page: 0,
  size: ordersPageSize,
  totalElements: 0,
  totalPages: 0
})
const selectedOrderId = ref<number | null>(null)
const selectedTab = ref('ALL')
const query = ref('')
const loading = ref(false)
const loadingMoreOrders = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const detailErrorMessage = ref('')
const statusMessage = ref('')
const cancelActionOpen = ref(false)
const deletingOrderId = ref<number | null>(null)
const actionLoading = ref(false)
const actionError = ref('')
const actionReason = ref('')
const selectedOrderDetail = ref<Order | null>(null)
let ordersRequestSequence = 0
let activeOrdersRefreshRequest = 0
let activeOrdersLoadMoreRequest = 0
let orderDetailRequestToken = 0
let selectedOrderRevision = 0

const statusLabelKeys: Record<string, string> = {
  PENDING_PAYMENT: 'orderCenter.status.pendingPayment',
  PAID: 'orderCenter.status.paid',
  CONFIRMED: 'orderCenter.status.confirmed',
  CANCELLED: 'orderCenter.status.cancelled',
  REFUND_PENDING: 'orderCenter.status.refundPending',
  REFUNDED: 'orderCenter.status.refunded',
  EXPIRED: 'orderCenter.status.expired'
}

const statusClassNames: Record<string, string> = {
  PENDING_PAYMENT: 'border-amber-200 bg-amber-50 text-amber-700',
  PAID: 'border-sky-200 bg-sky-50 text-sky-700',
  CONFIRMED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  CANCELLED: 'border-gray-200 bg-gray-50 text-gray-600',
  REFUND_PENDING: 'border-rose-200 bg-rose-50 text-rose-700',
  REFUNDED: 'border-purple-200 bg-purple-50 text-purple-700',
  EXPIRED: 'border-gray-200 bg-gray-50 text-gray-500'
}

const confirmationLabelKeys: Record<string, string> = {
  UNPAID: 'orderCenter.confirmation.unpaid',
  PAID: 'orderCenter.confirmation.paid',
  PARTIALLY_REFUNDED: 'orderCenter.confirmation.partiallyRefunded',
  REFUNDED: 'orderCenter.confirmation.refunded',
  FAILED: 'orderCenter.confirmation.failed'
}

const confirmationTextClasses: Record<string, string> = {
  UNPAID: 'text-amber-700',
  PAID: 'text-emerald-700',
  PARTIALLY_REFUNDED: 'text-rose-700',
  REFUNDED: 'text-purple-700',
  FAILED: 'text-rose-700'
}

const statusMeta = (status?: string) => ({
  label: status && statusLabelKeys[status] ? t(statusLabelKeys[status]) : (status || t('orderCenter.status.unknown')),
  className: statusClassNames[status || ''] || 'border-gray-200 bg-gray-50 text-gray-600'
})

const confirmationMeta = (status?: string) => ({
  label: status && confirmationLabelKeys[status] ? t(confirmationLabelKeys[status]) : (status || t('orderCenter.confirmation.unknown')),
  textClass: confirmationTextClasses[status || ''] || 'text-tibet-brown/65'
})

const orderItems = (order: Order) => Array.isArray(order.items) ? order.items : []
const orderVouchers = (order: Order) => Array.isArray(order.vouchers) ? order.vouchers : []
const thirdPartyConfirmations = (order: Order) => Array.isArray(order.paymentTransactions) ? order.paymentTransactions : []
const voucherSummaryLabel = (order: Order) => {
  const count = orderVouchers(order).length
  return count > 0 ? t('orderCenter.voucherCount', { count }) : t('orderCenter.voucherDetails')
}
const confirmationSummaryLabel = (order: Order) => {
  const count = thirdPartyConfirmations(order).length
  return count > 0 ? t('orderCenter.thirdPartyCount', { count }) : t('orderCenter.thirdPartyDetails')
}

const selectedOrder = computed(() => {
  if (selectedOrderDetail.value?.id === selectedOrderId.value) return selectedOrderDetail.value
  return filteredOrders.value.find(order => order.id === selectedOrderId.value) || null
})

const statusBucket = (order: Order) => {
  if (['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status)) return 'CLOSED'
  return order.status === 'PAID' ? 'CONFIRMED' : order.status
}

const countByBucket = (bucket: string) =>
  bucket === 'ALL'
    ? orders.value.length
    : orders.value.filter(order => statusBucket(order) === bucket).length

const statusTabs = computed(() => [
  { key: 'ALL', label: t('orderCenter.tabs.all'), count: countByBucket('ALL') },
  { key: 'PENDING_PAYMENT', label: t('orderCenter.tabs.pending'), count: countByBucket('PENDING_PAYMENT') },
  { key: 'CONFIRMED', label: t('orderCenter.tabs.confirmed'), count: countByBucket('CONFIRMED') },
  { key: 'REFUND_PENDING', label: t('orderCenter.tabs.afterSale'), count: countByBucket('REFUND_PENDING') },
  { key: 'CLOSED', label: t('orderCenter.tabs.closed'), count: countByBucket('CLOSED') }
])

const filteredOrders = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return orders.value.filter(order => {
    const matchesStatus = selectedTab.value === 'ALL' || statusBucket(order) === selectedTab.value
    if (!matchesStatus) return false
    if (!keyword) return true

    const searchable = [
      order.orderNo,
      order.productSummary
    ].join(' ').toLowerCase()
    return searchable.includes(keyword)
  })
})

const stats = computed(() => {
  return [
    { label: t('orderCenter.stats.all'), value: String(orders.value.length), hint: t('orderCenter.stats.allHint'), icon: ReceiptText },
    { label: t('orderCenter.stats.pending'), value: String(countByBucket('PENDING_PAYMENT')), hint: t('orderCenter.stats.pendingHint'), icon: PackageCheck },
    { label: t('orderCenter.stats.confirmed'), value: String(countByBucket('CONFIRMED')), hint: t('orderCenter.stats.confirmedHint'), icon: TicketCheck },
    { label: t('orderCenter.stats.archive'), value: `${countByBucket('CONFIRMED')}/${countByBucket('CLOSED')}`, hint: t('orderCenter.stats.archiveHint'), icon: FileText }
  ]
})

const ordersPage = computed(() => ordersPageInfo.value.page)
const ordersTotalPages = computed(() => ordersPageInfo.value.totalPages)
const hasMoreOrders = computed(() => hasNextPage(ordersPageInfo.value))

const normalizeOrder = (order: Order): Order => ({
  ...order,
  items: orderItems(order),
  paymentTransactions: thirdPartyConfirmations(order),
  vouchers: orderVouchers(order)
})

const applyOrdersPage = (response: PaginatedHttpResponse, append = false) => {
  const page = readPaginatedResponse<Order>(response, {
    page: append ? ordersPageInfo.value.page + 1 : 0,
    size: ordersPageSize
  })
  const normalizedContent = page.content.map(normalizeOrder)
  const nextOrders = append ? mergeUniqueById(orders.value, normalizedContent) : normalizedContent

  orders.value = nextOrders
  ordersPageInfo.value = {
    page: page.page,
    size: page.size || ordersPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }

  return nextOrders
}

const pageAfterItemRemoval = (pageInfo: PageMetadata) => {
  const totalElements = Math.max(0, pageInfo.totalElements - 1)
  const size = pageInfo.size || ordersPageSize

  return {
    ...pageInfo,
    totalElements,
    totalPages: size > 0 && totalElements > 0 ? Math.ceil(totalElements / size) : 0
  }
}

const loadOrders = async (preferredId?: number | null, page = 0, append = false) => {
  const requestSequence = ++ordersRequestSequence
  const selectionRevisionAtRequestStart = selectedOrderRevision
  if (append) {
    loadingMoreOrders.value = true
    activeOrdersLoadMoreRequest = requestSequence
  } else {
    loading.value = true
    activeOrdersRefreshRequest = requestSequence
    loadingMoreOrders.value = false
    activeOrdersLoadMoreRequest = 0
  }
  errorMessage.value = ''
  try {
    const response = await api.get(endpoints.orders.my, {
      params: { page, size: ordersPageSize, _ts: Date.now() }
    })
    if (requestSequence !== ordersRequestSequence) return
    const nextOrders = applyOrdersPage(response, append)
    const nextId = selectionRevisionAtRequestStart === selectedOrderRevision
      ? preferredId ?? selectedOrderId.value
      : selectedOrderId.value
    selectedOrderId.value = nextOrders.some(order => order.id === nextId) ? nextId : null
    if (selectedOrderId.value) {
      selectedOrderDetail.value = null
      void loadOrderDetail(selectedOrderId.value, true)
    } else {
      resetActionForm()
    }
  } catch (error: unknown) {
    if (requestSequence !== ordersRequestSequence) return
    console.error('Failed to load consultation records:', summarizeClientError(error))
    errorMessage.value = safeClientErrorMessage(error, t('orderCenter.loadFailed'))
  } finally {
    if (append) {
      if (activeOrdersLoadMoreRequest === requestSequence) {
        loadingMoreOrders.value = false
        activeOrdersLoadMoreRequest = 0
      }
    } else {
      if (activeOrdersRefreshRequest === requestSequence) {
        loading.value = false
        activeOrdersRefreshRequest = 0
      }
    }
  }
}

const loadNextOrdersPage = async () => {
  if (loading.value || loadingMoreOrders.value || !hasMoreOrders.value) return
  await loadOrders(selectedOrderId.value, ordersPageInfo.value.page + 1, true)
}

const loadOrderDetail = async (id: number, force = false) => {
  if (!force && selectedOrderDetail.value?.id === id && orderItems(selectedOrderDetail.value).length) return

  const requestToken = ++orderDetailRequestToken
  detailLoading.value = true
  detailErrorMessage.value = ''
  try {
    const response = await api.get(endpoints.orders.detail(id), {
      params: { _ts: Date.now() }
    })
    if (requestToken !== orderDetailRequestToken || selectedOrderId.value !== id) return
    const detail = normalizeOrder(response.data as Order)
    selectedOrderDetail.value = detail
    orders.value = orders.value.map(order => order.id === detail.id ? { ...order, ...detail } : order)
  } catch (error: unknown) {
    if (requestToken === orderDetailRequestToken && selectedOrderId.value === id) {
      console.error('Failed to load consultation detail:', summarizeClientError(error))
      detailErrorMessage.value = safeClientErrorMessage(error, t('orderCenter.detailLoadFailed'))
    }
  } finally {
    if (requestToken === orderDetailRequestToken) {
      detailLoading.value = false
    }
  }
}

const selectOrder = (id: number) => {
  selectedOrderRevision += 1
  selectedOrderId.value = id
  selectedOrderDetail.value = null
  resetActionForm()
  void loadOrderDetail(id)
}

const closeOrderDetail = () => {
  selectedOrderRevision += 1
  orderDetailRequestToken += 1
  selectedOrderId.value = null
  selectedOrderDetail.value = null
  detailLoading.value = false
  detailErrorMessage.value = ''
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
    statusMessage.value = t('orderCenter.cancelSubmitted')
    resetActionForm()
    await loadOrders(order.id)
    await loadOrderDetail(order.id, true)
  } catch (error: unknown) {
    console.error('Consultation cancel failed:', summarizeClientError(error))
    actionError.value = safeClientErrorMessage(error, t('orderCenter.actionFailed'))
  } finally {
    actionLoading.value = false
  }
}

const canCancel = (order: Order) => ['PENDING_PAYMENT', 'PAID', 'CONFIRMED'].includes(order.status)
const canDelete = (order: Order) => ['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status)

const deleteClosedOrder = async (order: Order) => {
  if (!canDelete(order) || deletingOrderId.value) return
  const confirmed = await showConfirm({
    message: t('orderCenter.deleteConfirm', { orderNo: order.orderNo }),
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
    ordersPageInfo.value = pageAfterItemRemoval(ordersPageInfo.value)
    if (selectedOrderId.value === order.id) closeOrderDetail()
    statusMessage.value = t('orderCenter.deleteSuccess')
    showToast(statusMessage.value, 'success')
  } catch (error: unknown) {
    console.error('Failed to delete consultation record:', summarizeClientError(error))
    errorMessage.value = safeClientErrorMessage(error, t('orderCenter.deleteFailed'))
    showToast(errorMessage.value, 'error')
  } finally {
    deletingOrderId.value = null
  }
}

const formatCurrency = (value?: number | string | null, currency?: string | null) => {
  const amount = toFiniteAmount(value)
  return new Intl.NumberFormat(toIntlLocale(locale.value), {
    style: 'currency',
    currency: currency || 'CNY',
    maximumFractionDigits: amount % 1 === 0 ? 0 : 2
  }).format(amount)
}

const formatDate = (value?: string | null) => {
  if (!value) return t('common.pendingConfirm')
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(toIntlLocale(locale.value), {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const formatServiceDate = (item: OrderItem) => {
  if (!item.serviceStartDate) return t('orderCenter.pendingDate')
  if (!item.serviceEndDate || item.serviceEndDate === item.serviceStartDate) return item.serviceStartDate
  return `${item.serviceStartDate} ${t('orderCenter.dateRangeSeparator')} ${item.serviceEndDate}`
}

const formatVoucherDate = (value?: string | null) => value || t('common.pendingConfirm')

const voucherStatusLabelKeys: Record<string, string> = {
  ISSUED: 'orderCenter.voucherStatus.issued',
  CONSUMED: 'orderCenter.voucherStatus.consumed',
  CANCELLED: 'orderCenter.voucherStatus.cancelled'
}

const transactionStatusLabelKeys: Record<string, string> = {
  PENDING: 'orderCenter.transactionStatus.pending',
  SUCCESS: 'orderCenter.transactionStatus.success',
  FAILED: 'orderCenter.transactionStatus.failed',
  REFUNDED: 'orderCenter.transactionStatus.refunded'
}

const voucherStatusLabel = (status?: string | null) =>
  status && voucherStatusLabelKeys[status] ? t(voucherStatusLabelKeys[status]) : (status || t('common.unknown'))

const transactionStatusLabel = (status?: string | null) =>
  status && transactionStatusLabelKeys[status] ? t(transactionStatusLabelKeys[status]) : (status || t('common.unknown'))

const itemTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    SCENIC_SPOT: t('orderCenter.itemTypes.scenicSpot'),
    HOTEL_ROOM: t('orderCenter.itemTypes.hotelRoom'),
    ROUTE_PACKAGE: t('orderCenter.itemTypes.routePackage')
  }
  return labels[type] || type
}

const itemIcon = (type: string) => {
  if (type === 'HOTEL_ROOM') return Hotel
  if (type === 'SCENIC_SPOT') return Landmark
  return PackageCheck
}

const itemSummary = (order: Order) => {
  const items = orderItems(order)
  if (!items.length) return order.productSummary || t('orderCenter.detailFallback')
  return items
    .slice(0, 3)
    .map(item => `${item.productName}${item.skuName ? ` - ${item.skuName}` : ''}`)
    .join(' / ')
}

const orderCardActionLabel = (order: Order) =>
  t('orderCenter.orderCardAria', { summary: order.productSummary || order.orderNo })

onMounted(async () => {
  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }
  await loadOrders()
})
</script>
