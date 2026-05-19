<template>
  <div class="order-center-page tibet-page-shell min-h-screen relative overflow-hidden">
    <div class="absolute inset-0 pointer-events-none opacity-40 tibet-cloud-pattern"></div>

    <main class="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pt-28 pb-16">
      <motion.header
        class="mb-8 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"
        :initial="{ opacity: 0, y: 18 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.42, ease: [0.16, 1, 0.3, 1] }"
      >
        <div>
          <div class="inline-flex items-center gap-2 rounded-full border border-tibet-gold/25 bg-white/75 px-3 py-1 text-xs font-semibold text-tibet-red">
            <ReceiptText class="h-3.5 w-3.5" />
            统一旅行履约
          </div>
          <h1 class="mt-4 text-3xl font-bold text-tibet-dark md:text-4xl">订单中心</h1>
          <p class="mt-2 max-w-2xl text-sm leading-6 text-tibet-brown/65">
            集中查看景点、酒店和行程节点预订，处理取消、退款、凭证和发票。
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <router-link
            to="/route-planner"
            class="inline-flex items-center gap-2 rounded-xl border border-tibet-gold/25 bg-white/75 px-4 py-2.5 text-sm font-semibold text-tibet-brown transition hover:bg-amber-50"
          >
            <Sparkles class="h-4 w-4" />
            继续规划
          </router-link>
          <motion.button
            type="button"
            @click="loadOrders(selectedOrderId || undefined)"
            :disabled="loading"
            :whileHover="{ y: -1, scale: 1.01 }"
            :whileTap="{ scale: 0.98 }"
            class="inline-flex items-center gap-2 rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow shadow-md shadow-tibet-red/20 disabled:opacity-50"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
            刷新订单
          </motion.button>
        </div>
      </motion.header>

      <section class="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="stat in stats"
          :key="stat.label"
          class="rounded-2xl border border-white/60 bg-white/75 px-4 py-3 shadow-sm shadow-slate-900/3"
        >
          <div class="flex items-center justify-between gap-3">
            <p class="text-xs font-semibold text-tibet-brown/55">{{ stat.label }}</p>
            <component :is="stat.icon" class="h-4 w-4 text-tibet-gold" />
          </div>
          <p class="mt-2 text-2xl font-bold text-tibet-dark">{{ stat.value }}</p>
          <p class="mt-1 text-xs text-tibet-brown/50">{{ stat.hint }}</p>
        </div>
      </section>

      <section class="mb-6 rounded-2xl border border-white/60 bg-white/70 p-3 shadow-sm shadow-slate-900/3">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="flex gap-1 overflow-x-auto">
            <button
              v-for="tab in statusTabs"
              :key="tab.key"
              type="button"
              @click="selectedTab = tab.key"
              class="inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-semibold transition"
              :class="selectedTab === tab.key ? 'bg-tibet-dark text-white shadow-sm' : 'text-tibet-brown/65 hover:bg-white/80'"
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
              placeholder="搜索订单号、商品或凭证"
            >
          </label>
        </div>
      </section>

      <div v-if="errorMessage" class="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">
        {{ errorMessage }}
      </div>
      <div v-if="statusMessage" class="mb-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
        {{ statusMessage }}
      </div>

      <div v-if="loading && !orders.length" class="rounded-3xl border border-white/60 bg-white/70 p-12 text-center text-tibet-brown/60">
        <div class="mx-auto mb-4 h-11 w-11 animate-spin rounded-full border-2 border-tibet-gold/30 border-b-tibet-gold"></div>
        <p class="text-sm font-semibold">正在加载订单</p>
      </div>

      <div v-else-if="!orders.length" class="rounded-3xl border border-white/60 bg-white/75 px-6 py-12 text-center shadow-sm shadow-slate-900/3">
        <PackageCheck class="mx-auto h-12 w-12 text-tibet-gold" />
        <h2 class="mt-4 text-xl font-bold text-tibet-dark">还没有订单</h2>
        <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-tibet-brown/60">
          从 AI 行程生成可预订节点，或直接预订酒店后，这里会汇总所有履约状态。
        </p>
        <div class="mt-6 flex flex-wrap justify-center gap-3">
          <router-link to="/route-planner" class="rounded-xl bg-tibet-red px-4 py-2.5 text-sm font-semibold text-tibet-yellow">生成行程</router-link>
          <router-link to="/hotels" class="rounded-xl border border-tibet-gold/25 bg-white px-4 py-2.5 text-sm font-semibold text-tibet-brown">浏览酒店</router-link>
        </div>
      </div>

      <div v-else class="grid gap-6 lg:grid-cols-[minmax(0,1.02fr)_minmax(360px,0.98fr)]">
        <section class="space-y-3">
          <motion.article
            v-for="(order, index) in filteredOrders"
            :key="order.id"
            class="cursor-pointer rounded-2xl border bg-white/78 p-4 shadow-sm shadow-slate-900/3 transition"
            :class="selectedOrderId === order.id ? 'border-tibet-red/35 ring-2 ring-tibet-red/10' : 'border-white/65 hover:border-tibet-gold/30 hover:bg-white/90'"
            :initial="{ opacity: 0, y: 12 }"
            :animate="{ opacity: 1, y: 0 }"
            :transition="{ duration: 0.28, delay: Math.min(index * 0.03, 0.18) }"
            @click="selectOrder(order.id)"
          >
            <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold" :class="statusMeta(order.status).className">
                    {{ statusMeta(order.status).label }}
                  </span>
                  <span class="text-xs text-tibet-brown/45">{{ order.orderNo }}</span>
                </div>
                <h2 class="mt-2 truncate text-base font-bold text-tibet-dark">{{ order.productSummary || '旅行订单' }}</h2>
                <p class="mt-1 line-clamp-2 text-sm text-tibet-brown/58">
                  {{ itemSummary(order) }}
                </p>
              </div>

              <div class="shrink-0 text-left sm:text-right">
                <p class="text-lg font-bold tabular-nums text-tibet-dark">{{ formatCurrency(order.payableAmount, order.currency) }}</p>
                <p class="mt-1 text-xs font-medium" :class="paymentMeta(order.paymentStatus).textClass">
                  {{ paymentMeta(order.paymentStatus).label }}
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
                {{ order.invoices?.length || 0 }} 张发票
              </span>
            </div>
          </motion.article>

          <div v-if="!filteredOrders.length" class="rounded-2xl border border-white/60 bg-white/70 p-8 text-center text-sm text-tibet-brown/60">
            当前筛选下没有订单。
          </div>
        </section>

        <aside class="lg:sticky lg:top-28">
          <section v-if="selectedOrder" class="overflow-hidden rounded-3xl border border-white/65 bg-white/82 shadow-lg shadow-slate-900/5">
            <div class="border-b border-tibet-gold/10 bg-white/55 px-5 py-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-xs font-semibold text-tibet-brown/45">{{ selectedOrder.orderNo }}</p>
                  <h2 class="mt-1 text-xl font-bold text-tibet-dark">{{ selectedOrder.productSummary || '旅行订单' }}</h2>
                </div>
                <span class="shrink-0 rounded-full border px-3 py-1 text-xs font-semibold" :class="statusMeta(selectedOrder.status).className">
                  {{ statusMeta(selectedOrder.status).label }}
                </span>
              </div>

              <div class="mt-4 grid gap-3 sm:grid-cols-3">
                <div>
                  <p class="text-xs text-tibet-brown/45">应付金额</p>
                  <p class="mt-1 text-lg font-bold tabular-nums text-tibet-dark">{{ formatCurrency(selectedOrder.payableAmount, selectedOrder.currency) }}</p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">支付状态</p>
                  <p class="mt-1 text-sm font-semibold" :class="paymentMeta(selectedOrder.paymentStatus).textClass">{{ paymentMeta(selectedOrder.paymentStatus).label }}</p>
                </div>
                <div>
                  <p class="text-xs text-tibet-brown/45">下单时间</p>
                  <p class="mt-1 text-sm font-semibold text-tibet-dark">{{ formatDate(selectedOrder.createdAt) }}</p>
                </div>
              </div>
            </div>

            <div class="divide-y divide-tibet-gold/10">
              <section class="px-5 py-4">
                <div class="mb-3 flex items-center justify-between gap-3">
                  <h3 class="text-sm font-bold text-tibet-dark">订单项目</h3>
                  <span class="text-xs text-tibet-brown/45">{{ selectedOrder.items?.length || 0 }} 项</span>
                </div>
                <div class="space-y-3">
                  <div v-for="item in selectedOrder.items" :key="item.id" class="flex gap-3">
                    <span class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-tibet-gold/10 text-tibet-gold">
                      <component :is="itemIcon(item.productType)" class="h-4 w-4" />
                    </span>
                    <div class="min-w-0 flex-1">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="font-semibold text-tibet-dark">{{ item.productName }}</p>
                        <span class="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] text-gray-600">{{ itemTypeLabel(item.productType) }}</span>
                      </div>
                      <p class="mt-1 text-xs text-tibet-brown/55">
                        {{ item.skuName || '标准票/服务' }} · {{ formatServiceDate(item) }} · 数量 {{ item.quantity || 1 }}
                      </p>
                    </div>
                    <p class="shrink-0 text-sm font-bold tabular-nums text-tibet-dark">{{ formatCurrency(item.subtotal, selectedOrder.currency) }}</p>
                  </div>
                </div>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">凭证与履约</h3>
                <div v-if="selectedOrder.vouchers?.length" class="space-y-2">
                  <div v-for="voucher in selectedOrder.vouchers" :key="voucher.id" class="flex items-center justify-between gap-3 rounded-xl bg-emerald-50/75 px-3 py-2">
                    <div class="min-w-0">
                      <p class="truncate text-sm font-bold text-emerald-800">{{ voucher.voucherCode }}</p>
                      <p class="text-xs text-emerald-700/65">{{ voucher.validFrom }} 至 {{ voucher.validUntil }}</p>
                    </div>
                    <span class="rounded-full bg-white/75 px-2 py-1 text-[11px] font-semibold text-emerald-700">{{ voucher.status }}</span>
                  </div>
                </div>
                <p v-else class="text-sm text-tibet-brown/55">暂无凭证，支付确认后会自动生成。</p>
              </section>

              <section class="px-5 py-4">
                <h3 class="mb-3 text-sm font-bold text-tibet-dark">资金记录</h3>
                <div class="space-y-2">
                  <div v-for="payment in selectedOrder.paymentTransactions" :key="payment.id" class="flex items-center justify-between gap-3 text-sm">
                    <span class="text-tibet-brown/60">{{ payment.provider }} · {{ payment.status }}</span>
                    <span class="font-semibold text-tibet-dark">{{ formatCurrency(payment.amount, selectedOrder.currency) }}</span>
                  </div>
                  <div v-for="refund in selectedOrder.refunds" :key="refund.id" class="flex items-center justify-between gap-3 text-sm">
                    <span class="text-tibet-brown/60">{{ refund.refundNo }} · {{ refund.status }}</span>
                    <span class="font-semibold text-rose-700">-{{ formatCurrency(refund.amount, selectedOrder.currency) }}</span>
                  </div>
                  <p v-if="!selectedOrder.paymentTransactions?.length && !selectedOrder.refunds?.length" class="text-sm text-tibet-brown/55">暂无支付或退款记录。</p>
                </div>
              </section>

              <section class="px-5 py-4">
                <div class="mb-3 flex items-center justify-between gap-3">
                  <h3 class="text-sm font-bold text-tibet-dark">发票</h3>
                  <span class="text-xs text-tibet-brown/45">{{ selectedOrder.invoices?.length || 0 }} 张</span>
                </div>
                <div v-if="selectedOrder.invoices?.length" class="space-y-2">
                  <div v-for="invoice in selectedOrder.invoices" :key="invoice.id" class="rounded-xl bg-sky-50/75 px-3 py-2">
                    <div class="flex items-center justify-between gap-3">
                      <p class="truncate text-sm font-bold text-sky-800">{{ invoice.invoiceTitle }}</p>
                      <span class="text-xs font-semibold text-sky-700">{{ invoice.status }}</span>
                    </div>
                    <p class="mt-1 text-xs text-sky-700/65">{{ invoice.invoiceNo }} · {{ formatCurrency(invoice.amount, selectedOrder.currency) }}</p>
                  </div>
                </div>
                <p v-else class="text-sm text-tibet-brown/55">暂无发票申请。</p>
              </section>

              <section class="px-5 py-4">
                <div class="grid gap-2 sm:grid-cols-3">
                  <button
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40"
                    :class="activeAction === 'cancel' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-tibet-gold/20 bg-white/75 text-tibet-brown hover:bg-amber-50'"
                    :disabled="!canCancel(selectedOrder)"
                    @click="beginAction('cancel')"
                  >
                    <Ban class="h-4 w-4" />
                    取消
                  </button>
                  <button
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40"
                    :class="activeAction === 'refund' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-tibet-gold/20 bg-white/75 text-tibet-brown hover:bg-amber-50'"
                    :disabled="!canRefund(selectedOrder)"
                    @click="beginAction('refund')"
                  >
                    <RotateCcw class="h-4 w-4" />
                    退款
                  </button>
                  <button
                    type="button"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40"
                    :class="activeAction === 'invoice' ? 'border-sky-200 bg-sky-50 text-sky-700' : 'border-tibet-gold/20 bg-white/75 text-tibet-brown hover:bg-amber-50'"
                    :disabled="!canInvoice(selectedOrder)"
                    @click="beginAction('invoice')"
                  >
                    <FileText class="h-4 w-4" />
                    开票
                  </button>
                </div>

                <form v-if="activeAction" class="mt-4 rounded-2xl border border-tibet-gold/15 bg-white/75 p-4" @submit.prevent="submitAction">
                  <div v-if="activeAction === 'refund'" class="mb-3">
                    <label class="mb-1 block text-xs font-semibold text-tibet-brown/60">退款范围</label>
                    <select
                      v-model="refundItemId"
                      class="w-full rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                    >
                      <option value="">整单退款</option>
                      <option v-for="item in selectedOrder.items" :key="item.id" :value="String(item.id)">
                        {{ item.productName }} · {{ formatCurrency(item.subtotal, selectedOrder.currency) }}
                      </option>
                    </select>
                  </div>

                  <div v-if="activeAction === 'invoice'" class="space-y-3">
                    <label class="block">
                      <span class="mb-1 block text-xs font-semibold text-tibet-brown/60">发票抬头</span>
                      <input
                        v-model.trim="invoiceTitle"
                        type="text"
                        class="w-full rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                        placeholder="个人姓名或公司名称"
                      >
                    </label>
                    <label class="block">
                      <span class="mb-1 block text-xs font-semibold text-tibet-brown/60">税号</span>
                      <input
                        v-model.trim="invoiceTaxNo"
                        type="text"
                        class="w-full rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm uppercase outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                        placeholder="企业开票时填写"
                      >
                    </label>
                  </div>

                  <label v-else class="block">
                    <span class="mb-1 block text-xs font-semibold text-tibet-brown/60">{{ activeAction === 'cancel' ? '取消原因' : '退款原因' }}</span>
                    <textarea
                      v-model.trim="actionReason"
                      rows="3"
                      class="w-full resize-none rounded-xl border border-tibet-gold/20 bg-white px-3 py-2 text-sm outline-none focus:border-tibet-gold/50 focus:ring-2 focus:ring-tibet-gold/10"
                      placeholder="可选，便于客服处理"
                    ></textarea>
                  </label>

                  <p v-if="actionError" class="mt-3 text-xs font-medium text-rose-600">{{ actionError }}</p>

                  <div class="mt-4 flex justify-end gap-2">
                    <button type="button" class="rounded-xl px-3 py-2 text-sm font-semibold text-tibet-brown/60 hover:bg-gray-100" @click="resetActionForm">收起</button>
                    <button type="submit" class="rounded-xl bg-tibet-dark px-4 py-2 text-sm font-semibold text-white disabled:opacity-50" :disabled="actionLoading">
                      {{ actionLoading ? '处理中' : actionSubmitLabel }}
                    </button>
                  </div>
                </form>
              </section>
            </div>
          </section>

          <section v-else class="rounded-3xl border border-white/65 bg-white/75 p-8 text-center text-tibet-brown/60">
            <WalletCards class="mx-auto mb-3 h-10 w-10 text-tibet-gold" />
            <p class="text-sm font-semibold">选择左侧订单查看详情。</p>
          </section>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { motion } from 'motion-v'
import {
  Ban,
  CalendarDays,
  FileText,
  Hotel,
  Landmark,
  PackageCheck,
  ReceiptText,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  TicketCheck,
  WalletCards
} from 'lucide-vue-next'
import api, { endpoints } from '../api'
import { useAuthStore } from '../stores/auth'

interface OrderItem {
  id: number
  productType: string
  productId: number
  skuId?: number | null
  productName: string
  skuName?: string | null
  serviceStartDate?: string | null
  serviceEndDate?: string | null
  quantity?: number | null
  unitPrice?: number | string | null
  subtotal?: number | string | null
  status: string
  legacyReferenceType?: string | null
  legacyReferenceId?: number | null
}

interface Voucher {
  id: number
  orderItemId?: number | null
  voucherCode: string
  status: string
  validFrom?: string | null
  validUntil?: string | null
  issuedAt?: string | null
}

interface PaymentTransaction {
  id: number
  transactionNo: string
  provider: string
  amount?: number | string | null
  status: string
  signatureValid?: boolean | null
  paidAt?: string | null
  createdAt?: string | null
}

interface RefundRecord {
  id: number
  orderItemId?: number | null
  refundNo: string
  amount?: number | string | null
  status: string
  reason?: string | null
  requestedAt?: string | null
  processedAt?: string | null
}

interface InvoiceRecord {
  id: number
  invoiceNo: string
  invoiceTitle: string
  taxNo?: string | null
  amount?: number | string | null
  status: string
  requestedAt?: string | null
  issuedAt?: string | null
}

interface Order {
  id: number
  orderNo: string
  status: string
  paymentStatus: string
  currency?: string | null
  productSummary?: string | null
  totalAmount?: number | string | null
  discountAmount?: number | string | null
  payableAmount?: number | string | null
  customerName?: string | null
  customerPhone?: string | null
  customerNote?: string | null
  sourceType?: string | null
  sourceReferenceId?: number | null
  lockedUntil?: string | null
  expiresAt?: string | null
  paidAt?: string | null
  confirmedAt?: string | null
  cancelledAt?: string | null
  createdAt?: string | null
  items: OrderItem[]
  paymentTransactions: PaymentTransaction[]
  refunds: RefundRecord[]
  vouchers: Voucher[]
  invoices: InvoiceRecord[]
}

type ActionType = 'cancel' | 'refund' | 'invoice'

const router = useRouter()
const auth = useAuthStore()

const orders = ref<Order[]>([])
const selectedOrderId = ref<number | null>(null)
const selectedTab = ref('ALL')
const query = ref('')
const loading = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const activeAction = ref<ActionType | null>(null)
const actionLoading = ref(false)
const actionError = ref('')
const actionReason = ref('')
const refundItemId = ref('')
const invoiceTitle = ref('')
const invoiceTaxNo = ref('')

const statusLabels: Record<string, { label: string; className: string }> = {
  PENDING_PAYMENT: { label: '待支付', className: 'border-amber-200 bg-amber-50 text-amber-700' },
  PAID: { label: '已支付', className: 'border-sky-200 bg-sky-50 text-sky-700' },
  CONFIRMED: { label: '已确认', className: 'border-emerald-200 bg-emerald-50 text-emerald-700' },
  CANCELLED: { label: '已取消', className: 'border-gray-200 bg-gray-50 text-gray-600' },
  REFUND_PENDING: { label: '退款中', className: 'border-rose-200 bg-rose-50 text-rose-700' },
  REFUNDED: { label: '已退款', className: 'border-purple-200 bg-purple-50 text-purple-700' },
  EXPIRED: { label: '已过期', className: 'border-gray-200 bg-gray-50 text-gray-500' }
}

const paymentLabels: Record<string, { label: string; textClass: string }> = {
  UNPAID: { label: '未支付', textClass: 'text-amber-700' },
  PAID: { label: '已支付', textClass: 'text-emerald-700' },
  PARTIALLY_REFUNDED: { label: '部分退款', textClass: 'text-rose-700' },
  REFUNDED: { label: '已退款', textClass: 'text-purple-700' },
  FAILED: { label: '支付失败', textClass: 'text-rose-700' }
}

const statusMeta = (status?: string) =>
  statusLabels[status || ''] || { label: status || '未知', className: 'border-gray-200 bg-gray-50 text-gray-600' }

const paymentMeta = (status?: string) =>
  paymentLabels[status || ''] || { label: status || '未知支付', textClass: 'text-tibet-brown/65' }

const selectedOrder = computed(() => orders.value.find(order => order.id === selectedOrderId.value) || null)

const statusBucket = (order: Order) => {
  if (['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status)) return 'CLOSED'
  return order.status
}

const countByBucket = (bucket: string) =>
  bucket === 'ALL'
    ? orders.value.length
    : orders.value.filter(order => statusBucket(order) === bucket).length

const statusTabs = computed(() => [
  { key: 'ALL', label: '全部', count: countByBucket('ALL') },
  { key: 'PENDING_PAYMENT', label: '待支付', count: countByBucket('PENDING_PAYMENT') },
  { key: 'CONFIRMED', label: '已确认', count: countByBucket('CONFIRMED') },
  { key: 'REFUND_PENDING', label: '退款中', count: countByBucket('REFUND_PENDING') },
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
  const paidOrders = orders.value.filter(order => order.paymentStatus !== 'UNPAID')
  const voucherCount = orders.value.reduce((sum, order) => sum + (order.vouchers?.length || 0), 0)
  const refundCount = orders.value.reduce((sum, order) => sum + (order.refunds?.length || 0), 0)

  return [
    { label: '全部订单', value: String(orders.value.length), hint: '景点、酒店和行程节点', icon: ReceiptText },
    { label: '待支付', value: String(countByBucket('PENDING_PAYMENT')), hint: '库存锁定中的订单', icon: WalletCards },
    { label: '已支付', value: String(paidOrders.length), hint: '可申请凭证/发票', icon: TicketCheck },
    { label: '履约单据', value: `${voucherCount}/${refundCount}`, hint: '凭证 / 退款单', icon: FileText }
  ]
})

const actionSubmitLabel = computed(() => {
  if (activeAction.value === 'cancel') return '确认取消'
  if (activeAction.value === 'refund') return '提交退款'
  return '申请开票'
})

const loadOrders = async (preferredId?: number) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await api.get(endpoints.orders.my, { params: { _ts: Date.now() } })
    orders.value = Array.isArray(data) ? data : []
    const nextId = preferredId || selectedOrderId.value
    selectedOrderId.value = orders.value.some(order => order.id === nextId)
      ? nextId
      : (orders.value[0]?.id || null)
  } catch (error: any) {
    console.error('Failed to load orders:', error)
    errorMessage.value = error.response?.data?.error || '订单加载失败'
  } finally {
    loading.value = false
  }
}

const selectOrder = (id: number) => {
  selectedOrderId.value = id
  resetActionForm()
}

const beginAction = (type: ActionType) => {
  activeAction.value = activeAction.value === type ? null : type
  actionError.value = ''
  actionReason.value = ''
  refundItemId.value = ''
  if (type === 'invoice' && selectedOrder.value && !invoiceTitle.value) {
    invoiceTitle.value = selectedOrder.value.customerName || ''
  }
}

const resetActionForm = () => {
  activeAction.value = null
  actionError.value = ''
  actionReason.value = ''
  refundItemId.value = ''
  invoiceTitle.value = ''
  invoiceTaxNo.value = ''
}

const submitAction = async () => {
  if (!selectedOrder.value || !activeAction.value) return
  actionLoading.value = true
  actionError.value = ''
  statusMessage.value = ''

  try {
    const order = selectedOrder.value
    if (activeAction.value === 'cancel') {
      await api.post(endpoints.orders.cancel(order.id), { reason: actionReason.value || undefined })
      statusMessage.value = '订单已提交取消处理'
    } else if (activeAction.value === 'refund') {
      await api.post(endpoints.orders.refunds(order.id), {
        orderItemId: refundItemId.value ? Number(refundItemId.value) : undefined,
        reason: actionReason.value || undefined
      })
      statusMessage.value = '退款申请已提交'
    } else {
      if (!invoiceTitle.value.trim()) {
        actionError.value = '请填写发票抬头'
        return
      }
      await api.post(endpoints.orders.invoice(order.id), {
        invoiceTitle: invoiceTitle.value,
        taxNo: invoiceTaxNo.value ? invoiceTaxNo.value.toUpperCase() : undefined
      })
      statusMessage.value = '发票申请已提交'
    }

    const currentId = order.id
    resetActionForm()
    await loadOrders(currentId)
  } catch (error: any) {
    console.error('Order action failed:', error)
    actionError.value = error.response?.data?.error || '操作失败，请稍后重试'
  } finally {
    actionLoading.value = false
  }
}

const canCancel = (order: Order) =>
  ['PENDING_PAYMENT', 'PAID', 'CONFIRMED'].includes(order.status)

const canRefund = (order: Order) =>
  order.paymentStatus !== 'UNPAID' && !['CANCELLED', 'EXPIRED', 'REFUNDED', 'REFUND_PENDING'].includes(order.status)

const canInvoice = (order: Order) =>
  order.paymentStatus !== 'UNPAID' && !['CANCELLED', 'EXPIRED'].includes(order.status)

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
    ROUTE_PACKAGE: '路线套餐'
  }
  return labels[type] || type
}

const itemIcon = (type: string) => {
  if (type === 'HOTEL_ROOM') return Hotel
  if (type === 'SCENIC_SPOT') return Landmark
  return PackageCheck
}

const itemSummary = (order: Order) => {
  if (!order.items?.length) return '暂无商品明细'
  return order.items
    .slice(0, 3)
    .map(item => `${item.productName}${item.skuName ? ` · ${item.skuName}` : ''}`)
    .join(' / ')
}

onMounted(async () => {
  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }
  await loadOrders()
})
</script>
