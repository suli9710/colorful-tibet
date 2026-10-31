<template>
  <section class="trip-workbench" aria-labelledby="trip-workbench-heading">
    <header class="workbench-top">
      <div>
          <p class="eyebrow">YOUR TIBET JOURNEY · 行前工作台</p>
          <h2 id="trip-workbench-heading">把向往，变成清楚的安排</h2>
          <p class="intro">先看待确认事项，再逐日调整。估价不代表可订，准备完成不等于安全保证。</p>
          </div>
      <button type="button" class="export-button" @click="exportPack">
          <Download :size="17" aria-hidden="true" /> 导出离线行前包</button>
    </header>
    <div class="trip-stats">
      <div>
          <span>出发日期</span>
          <strong>{{ trip.startDate || '尚未确定' }}</strong>
          </div>
      <div>
          <span>行程长度</span>
          <strong>{{ trip.days }} 天 · {{ trip.travelers || '人数待确认' }}{{ trip.travelers ? '人' : '' }}</strong>
          </div>
      <div>
          <span>总参考估价</span>
          <strong>{{ tripEstimate(trip.totalEstimatedCost) }}</strong>
          </div>
      <div>
          <span>当前状态</span>
          <strong class="status-text">{{ blockingCount ? `${blockingCount} 项需处理` : '仍需人工确认' }}</strong>
          </div>
    </div>
    <details class="readiness-panel" :open="blockingCount > 0 || !trip.readiness">
      <summary>
          <ShieldAlert :size="18" aria-hidden="true" /> 待解决与待确认 <span>{{ issues.length }} 项</span>
          </summary>
      <p v-if="!trip.readiness" class="muted">当前行程没有完整核验记录，不能据此认定可执行或可预订。</p>
      <ul v-else class="issue-list">
          <li v-for="(issue, index) in issues" :key="`${issue.code}-${index}`">
          <strong>{{ issue.severity === 'BLOCKING' ? '需处理' : '需确认' }}</strong>
          <span>{{ issue.dayNumber ? `第 ${issue.dayNumber} 天 · ` : '' }}{{ issue.message }}</span>
          </li>
          </ul>
      <p class="muted">{{ trip.readiness?.disclaimer || '信息会随日期变化，请向实际提供方核对。高原风险提示不构成医学判断。' }}</p>
    </details>
    <div class="workbench-layout">
      <div class="day-workspace">
        <nav class="day-nav" aria-label="选择行程日期">
          <button v-for="day in trip.itineraryDays" :key="day.id" type="button" :aria-pressed="selectedDay === day.dayNumber" @click="selectedDay = day.dayNumber">
          <span>DAY {{ String(day.dayNumber).padStart(2, '0') }}</span>
          <strong>第 {{ day.dayNumber }} 天</strong>
          <small>{{ day.region || day.travelDate || '查看安排' }}</small>
          </button>
          </nav>
        <section v-if="activeDay" class="day-panel" :aria-label="`第 ${activeDay.dayNumber} 天行程`">
          <header>
          <div>
          <p class="eyebrow">{{ activeDay.travelDate || '日期待确认' }}</p>
          <h3>{{ activeDay.title }}</h3>
          </div>
          <MapPin :size="22" aria-hidden="true" />
          </header>
          <ol class="timeline">
          <li v-for="item in activeDay.items" :key="item.id">
          <span class="timeline-dot" aria-hidden="true">
          </span>
          <div class="item-time">{{ item.startTime || '时间待确认' }} · {{ item.itemType === 'HOTEL' ? '住宿' : item.itemType === 'SCENIC_SPOT' ? '景点' : '行程安排' }}</div>
          <h4>{{ item.title }}</h4>
          <p v-if="item.description">{{ item.description }}</p>
          <div class="item-meta">
          <span>估价 {{ tripEstimate(item.estimatedCost) }}</span>
          <span>{{ ['BOOKED', 'INQUIRY_SUBMITTED'].includes(item.bookingStatus || '') ? '咨询已提交 · 履约待确认' : '预约 / 库存待确认' }}</span>
          </div>
          <p v-if="item.alternatives" class="alternative">
          <Shuffle :size="14" aria-hidden="true" /> {{ item.alternatives }}</p>
          <button type="button" class="supplier-button edit-time-button" :disabled="Boolean(editingItemId) || item.bookingReferenceId != null || item.version == null" :title="item.bookingReferenceId != null ? '已有咨询记录，请先处理对应订单' : item.version == null ? '请重新加载以获取可编辑版本' : undefined" @click="startEdit(item)">调整时间</button>
            <form v-if="editingId === item.id" class="time-editor" @submit.prevent="saveEdit(item)">
              <label :for="`item-time-${item.id}`">开始时间<input :id="`item-time-${item.id}`" v-model="editedTime" type="time" required :disabled="Boolean(editingItemId)">
          </label>
              <label :for="`item-duration-${item.id}`">时长（分钟）<input :id="`item-duration-${item.id}`" v-model.number="editedDuration" type="number" min="1" max="1440" step="1" required :disabled="Boolean(editingItemId)">
          </label>
              <div class="time-editor-actions">
          <button type="submit" :disabled="Boolean(editingItemId)" class="supplier-button">{{ editingItemId === item.id ? '保存并重新检查…' : '保存并重新检查' }}</button>
          <button type="button" :disabled="Boolean(editingItemId)" class="supplier-button" @click="editingId = null">取消</button>
          </div>
              <p v-if="editError" role="alert">{{ editError }}</p>
            </form>
            <button v-if="['BOOK_HOTEL', 'BOOK_SPOT'].includes(item.bookingAction || '') && !['BOOKED', 'INQUIRY_SUBMITTED'].includes(item.bookingStatus || '')" type="button" class="supplier-button" :disabled="bookingItemId === item.id" @click="$emit('book', item)">{{ bookingItemId === item.id ? '正在打开…' : '前往第三方查询' }} <ExternalLink :size="14" aria-hidden="true" />
          </button>
          </li>
          </ol>
        </section>
      </div>
      <aside class="preparation-panel">
        <div class="aside-heading">
          <ListChecks :size="20" aria-hidden="true" />
          <h3>出发前，一项项确认</h3>
          </div>
          <p class="muted">{{ completedCount }} / {{ checklist.length }} 项个人准备完成。勾选不会改变平台核验状态。</p>
        <label v-for="item in checklist" :key="item.id" class="checklist-item">
          <input type="checkbox" :checked="completed.has(item.id)" @change="toggleItem(item.id)">
          <span>{{ item.title }}</span>
          </label>
        <p class="session-note">勾选仅保留在本次页面中；导出可带走当前进度，不保存证件或健康信息。</p>
        <h3 class="sources-heading">查证信息，从来源开始</h3>
          <ul class="source-list">
          <li v-for="source in safeSources" :key="source.id">
          <a :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.title }} <ExternalLink :size="13" aria-hidden="true" />
          </a>
          <span>{{ source.lastVerifiedAt ? `最后核验：${source.lastVerifiedAt}` : '参考入口 · 当前有效性待核验' }}</span>
          </li>
          </ul>
          <p v-if="!safeSources.length" class="muted">尚无可靠来源记录，请向景区与官方渠道核实。</p>
      </aside>
    </div>
    <p class="workbench-footnote" role="status">{{ exportMessage || '离线包包含逐日安排、来源和准备清单，可用浏览器打开、打印；出发前仍需重新确认时效。' }}</p>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Download, ExternalLink, ListChecks, MapPin, ShieldAlert, Shuffle } from 'lucide-vue-next'
import { buildOfflineTrip, defaultPreparation, safeSourceUrl, tripEstimate, type WorkbenchTrip, type TripItem } from '../utils/tripWorkbench'
const props = defineProps<{ trip: WorkbenchTrip; bookingItemId?: number | null; editingItemId?: number | null; editError?: string }>()
const emit = defineEmits<{ book: [item: TripItem]; edit: [itemId: number, values: { startTime: string; durationMinutes: number; expectedVersion: number }] }>()
const selectedDay = ref(props.trip.itineraryDays[0]?.dayNumber ?? 1)
const completed = ref(new Set<string>())
const exportMessage = ref('')
const editingId = ref<number | null>(null)
const editedTime = ref('09:00')
const editedDuration = ref(60)
function startEdit(item: TripItem) { if (props.editingItemId || item.bookingReferenceId != null || item.version == null) return; editingId.value = item.id; editedTime.value = item.startTime?.slice(0,5) || '09:00'; editedDuration.value = item.durationMinutes || 60 }
function saveEdit(item: TripItem) { if (props.editingItemId || item.version == null || !/^([01]\d|2[0-3]):[0-5]\d$/.test(editedTime.value) || !Number.isInteger(editedDuration.value) || editedDuration.value < 1 || editedDuration.value > 1440) return; emit('edit', item.id, { startTime: editedTime.value, durationMinutes: editedDuration.value, expectedVersion: item.version }) }
watch(() => props.trip, (next, previous) => { if (next.id !== previous.id) selectedDay.value = next.itineraryDays[0]?.dayNumber ?? 1; completed.value = new Set(); exportMessage.value = ''; editingId.value = null })
const issues = computed(() => props.trip.readiness?.issues ?? [])
const blockingCount = computed(() => issues.value.filter((issue) => issue.severity === 'BLOCKING').length)
const checklist = computed(() => props.trip.readiness?.checklist?.length ? props.trip.readiness.checklist : defaultPreparation)
const completedCount = computed(() => checklist.value.filter((item) => completed.value.has(item.id)).length)
const activeDay = computed(() => props.trip.itineraryDays.find((day) => day.dayNumber === selectedDay.value) ?? props.trip.itineraryDays[0])
const safeSources = computed(() => (props.trip.readiness?.sources ?? []).flatMap((source) => { const url = safeSourceUrl(source.url); return url ? [{ ...source, url }] : [] }))
function toggleItem(id: string) { const next = new Set(completed.value); next.has(id) ? next.delete(id) : next.add(id); completed.value = next }
function exportPack() {
  const url = URL.createObjectURL(new Blob([buildOfflineTrip(props.trip, completed.value)], { type: 'text/html;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = `tibet-trip-${props.trip.id}.html`; document.body.appendChild(link); link.click(); link.remove(); window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  exportMessage.value = '离线行前包已生成。内容可能过时，出发前请重新核实。'
}
</script>
<style scoped>
.time-editor{margin-top:12px;
  padding:12px;
  background:#f4eee4;
  border-radius:10px;
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:10px}
.time-editor label{font-size:11px;
  display:grid;
  gap:5px}
.time-editor input{min-width:0;
  width:100%;
  min-height:44px;
  border:1px solid #cdbfac;
  border-radius:8px;
  background:white;
  padding:6px}
.time-editor-actions{display:flex;
  flex-wrap:wrap;
  gap:8px;
  grid-column:1/-1}
.time-editor p{grid-column:1/-1;
  color:#9d2236}
.edit-time-button{margin-right:8px}
.trip-workbench{color:#382f29;
  background:#fcfaf6;
  border:1px solid #dfd5c4;
  border-radius:24px;
  overflow:hidden}
.workbench-top{display:flex;
  justify-content:space-between;
  align-items:flex-start;
  gap:24px;
  padding:28px 24px;
  background:#f4eee4}
.eyebrow{font-size:10px;
  font-weight:700;
  letter-spacing:.14em;
  color:#865c37;
  margin:0 0 8px}
.workbench-top h2{font-size:clamp(21px,3vw,28px);
  font-weight:750;
  line-height:1.4;
  margin:0}
.intro{font-size:13px;
  line-height:1.8;
  color:#655b50;
  margin:10px 0 0;
  max-width:560px}
.export-button,.supplier-button{display:inline-flex;
  gap:8px;
  align-items:center;
  justify-content:center;
  min-height:44px;
  border-radius:10px;
  font-weight:650;
  font-size:12px;
  padding:10px 14px}
.export-button{background:#7d2837;
  color:white;
  flex-shrink:0}
.trip-stats{display:grid;
  grid-template-columns:repeat(4,minmax(0,1fr));
  border-bottom:1px solid #dfd5c4;
  padding:20px 24px;
  gap:16px}
.trip-stats span{display:block;
  font-size:11px;
  color:#746754;
  margin-bottom:6px}
.trip-stats strong{font-size:15px}
.trip-stats .status-text{color:#8c5420}
.readiness-panel{margin:20px 24px;
  border:1px solid #e3cfa4;
  background:#fffaf0;
  border-radius:12px;
  padding:14px 16px}
.readiness-panel summary{cursor:pointer;
  display:flex;
  align-items:center;
  gap:8px;
  font-size:13px;
  font-weight:700;
  min-height:28px}
.readiness-panel summary span{margin-left:auto;
  font-size:11px}
.issue-list{list-style:none;
  padding:0;
  margin:12px 0;
  display:grid;
  gap:8px}
.issue-list li{display:flex;
  align-items:flex-start;
  gap:10px;
  font-size:12px;
  line-height:1.7}
.issue-list strong{flex-shrink:0;
  color:#8e4130}
.muted{color:#6b6255;
  font-size:12px;
  line-height:1.8}
.workbench-layout{display:grid;
  grid-template-columns:minmax(0,1.7fr) minmax(230px,1fr);
  gap:24px;
  padding:0 24px 24px}
.day-workspace{min-width:0}
.day-nav{display:flex;
  gap:8px;
  overflow-x:auto;
  padding:2px 2px 14px}
.day-nav button{min-width:88px;
  display:flex;
  flex-direction:column;
  align-items:flex-start;
  padding:12px;
  background:white;
  border:1px solid #ded5c9;
  border-radius:10px;
  text-align:left;
  gap:3px;
  flex-shrink:0}
.day-nav button[aria-pressed=true]{background:#7d2837;
  border-color:#7d2837;
  color:white}
.day-nav button span{font-size:9px;
  letter-spacing:.1em;
  opacity:.75}
.day-nav button strong{font-size:13px}
.day-nav button small{font-size:10px;
  max-width:120px;
  overflow:hidden;
  text-overflow:ellipsis;
  white-space:nowrap}
.day-panel{background:white;
  border:1px solid #e6dfd4;
  border-radius:14px;
  padding:20px}
.day-panel header{display:flex;
  justify-content:space-between;
  gap:16px;
  border-bottom:1px solid #eee6dc;
  padding-bottom:16px}
.day-panel h3{font-size:17px;
  font-weight:700;
  margin:0}
.timeline{list-style:none;
  padding:0 0 0 18px;
  margin:22px 0 0;
  border-left:1px solid #d5b680}
.timeline li{position:relative;
  padding:0 0 24px 0}
.timeline li:last-child{padding-bottom:0}
.timeline-dot{position:absolute;
  left:-24px;
  top:3px;
  width:11px;
  height:11px;
  border-radius:50%;
  background:#b68b48;
  border:3px solid white;
  box-shadow:0 0 0 1px #b68b48}
.item-time{font-size:10px;
  font-weight:650;
  color:#8b6233}
.timeline h4{font-size:14px;
  font-weight:700;
  margin:5px 0}
.timeline p{font-size:12px;
  line-height:1.8;
  color:#655e54;
  overflow-wrap:anywhere}
.item-meta{display:flex;
  flex-wrap:wrap;
  gap:6px;
  margin:8px 0}
.item-meta span{font-size:10px;
  padding:4px 7px;
  background:#f3f0eb;
  border-radius:5px;
  color:#625344}
.alternative{display:flex;
  gap:6px;
  align-items:flex-start}
.supplier-button{border:1px solid #cdbfac;
  color:#763240;
  margin-top:5px}
.supplier-button:disabled{opacity:.6}
.preparation-panel{background:#f1ede5;
  border-radius:14px;
  padding:20px;
  align-self:start;
  min-width:0}
.aside-heading{display:flex;
  gap:8px;
  align-items:center;
  color:#71402c}
.aside-heading h3,.sources-heading{font-size:14px;
  font-weight:700;
  margin:0}
.checklist-item{display:flex;
  align-items:flex-start;
  gap:10px;
  font-size:12px;
  line-height:1.7;
  padding:12px 0;
  border-bottom:1px solid #dfd6c7;
  cursor:pointer}
.checklist-item input{margin-top:4px;
  accent-color:#7d2837;
  width:16px;
  height:16px;
  flex-shrink:0}
.session-note{font-size:10px;
  color:#756b5c;
  line-height:1.7;
  margin-top:12px}
.sources-heading{margin-top:24px}
.source-list{list-style:none;
  padding:0}
.source-list li{padding:10px 0}
.source-list a{display:flex;
  gap:6px;
  align-items:center;
  font-size:12px;
  color:#235d68;
  text-decoration:underline;
  text-underline-offset:3px}
.source-list span{display:block;
  font-size:10px;
  color:#756b5c;
  margin-top:5px}
.workbench-footnote{padding:14px 24px;
  background:#f4eee4;
  font-size:11px;
  line-height:1.8;
  margin:0;
  color:#6e6253}
button:focus-visible,a:focus-visible,input:focus-visible,summary:focus-visible{outline:3px solid #235d68;
  outline-offset:3px}
@media(max-width:700px){.workbench-top{padding:22px 18px;
  flex-direction:column;
  gap:16px}
.export-button{width:100%}
.trip-stats{grid-template-columns:repeat(2,minmax(0,1fr));
  padding:18px}
.workbench-layout{grid-template-columns:minmax(0,1fr);
  padding:0 18px 18px;
  gap:18px}
.readiness-panel{margin:18px}
.day-panel{padding:16px}
.preparation-panel{padding:18px}
.workbench-footnote{padding:14px 18px}
.trip-stats strong{font-size:14px}
}
@media print{.day-nav,.export-button,.supplier-button{display:none}
}

</style>
