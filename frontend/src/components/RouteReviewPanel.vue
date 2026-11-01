<template>
  <section class="route-review" aria-labelledby="route-review-title">
    <header class="review-heading">
      <span class="review-number" aria-hidden="true">02</span>
      <div>
        <h2 id="route-review-title">{{ t('plannerReview.title') }}</h2>
        <p id="route-review-note-hint">{{ t('plannerReview.localNotice') }}</p>
      </div>
    </header>

    <div v-if="days.length" class="review-days">
      <details v-for="(day, index) in days" :key="day.day" :open="index === 0" class="review-day">
        <summary>
          <span class="review-day-label">{{ day.day }}</span>
          <span class="review-day-title">{{ day.title }}</span>
          <span v-if="notes[day.day]?.trim()" class="review-note-indicator">{{ t('plannerReview.hasNote') }}</span>
        </summary>
        <div class="review-day-body">
          <p class="review-summary">{{ day.summary }}</p>
          <label :for="`route-review-note-${index}`">{{ t('plannerReview.noteLabel') }}</label>
          <textarea
            :id="`route-review-note-${index}`"
            :value="notes[day.day] || ''"
            :placeholder="t('plannerReview.notePlaceholder')"
            :aria-describedby="`route-review-note-hint route-review-note-count-${index}`"
            maxlength="600"
            rows="3"
            @input="updateNote(day.day, ($event.target as HTMLTextAreaElement).value)"
          ></textarea>
          <div class="review-note-footer">
            <span :id="`route-review-note-count-${index}`">{{ t('plannerReview.noteCount', { count: (notes[day.day] || '').length }) }}</span>
            <button
              type="button"
              class="review-text-button"
              :aria-label="t('plannerReview.jumpAria', { day: day.day })"
              @click="emit('jump', day.day)"
            >{{ t('plannerReview.jump') }}</button>
            <button
              v-if="notes[day.day]"
              type="button"
              class="review-text-button"
              :disabled="Boolean(clearingDay)"
              :aria-label="t('plannerReview.clearDayAria', { day: day.day })"
              @click="clearNote(day.day)"
            >{{ t('plannerReview.clearNote') }}</button>
          </div>
        </div>
      </details>
    </div>
    <p v-else class="review-empty">{{ t('plannerReview.noDays') }}</p>

    <div v-if="lastCleared" class="review-undo" role="status">
      <span>{{ t('plannerReview.clearedDay', { day: lastCleared.day }) }}</span>
      <button type="button" class="review-text-button" @click="undoClear">{{ t('plannerReview.undo') }}</button>
    </div>

    <fieldset class="review-checklist">
      <legend>{{ t('plannerReview.checklistTitle') }}</legend>
      <p id="route-review-checklist-hint">{{ t('plannerReview.checklistNotice') }}</p>
      <label v-for="item in checklistItems" :key="item" class="review-check">
        <input v-model="checkedItems" type="checkbox" :value="item" aria-describedby="route-review-checklist-hint">
        <span>{{ t(`plannerReview.checklist.${item}`) }}</span>
      </label>
      <p class="review-check-count" role="status" aria-live="polite">{{ t('plannerReview.checklistProgress', { count: checkedItems.length, total: checklistItems.length }) }}</p>
    </fieldset>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { showConfirm } from '../composables/useConfirm'

const props = defineProps<{
  days: { day: string; title: string; summary: string }[]
  routeKey: string
}>()
const emit = defineEmits<{
  'update:notes': [notes: Record<string, string>]
  jump: [day: string]
}>()
const { t } = useI18n()
// Deliberately memory-only: private trip notes are never written to browser storage.
const notes = ref<Record<string, string>>({})
const lastCleared = ref<{ day: string; text: string } | null>(null)
const clearingDay = ref('')
const checklistItems = ['budget', 'availability', 'highland'] as const
const checkedItems = ref<string[]>([])
const hasNotes = computed(() => Object.values(notes.value).some(note => note.trim()))

const updateNote = (day: string, value: string) => {
  notes.value = { ...notes.value, [day]: value.slice(0, 600) }
  if (lastCleared.value?.day === day) lastCleared.value = null
}

const clearNote = async (day: string) => {
  if (clearingDay.value || !notes.value[day]) return
  clearingDay.value = day
  const routeKey = props.routeKey
  try {
    const confirmed = await showConfirm({
      title: t('plannerReview.clearTitle'),
      message: t('plannerReview.clearMessage', { day }),
      confirmLabel: t('plannerReview.clearNote'),
      cancelLabel: t('plannerReview.keepNote'),
      tone: 'danger'
    })
    if (!confirmed || routeKey !== props.routeKey) return
    lastCleared.value = { day, text: notes.value[day] || '' }
    notes.value = { ...notes.value, [day]: '' }
  } finally {
    clearingDay.value = ''
  }
}

const undoClear = () => {
  if (!lastCleared.value) return
  notes.value = { ...notes.value, [lastCleared.value.day]: lastCleared.value.text }
  lastCleared.value = null
}

watch(() => props.routeKey, () => {
  notes.value = {}
  checkedItems.value = []
  lastCleared.value = null
})
watch(notes, value => emit('update:notes', { ...value }), { flush: 'sync' })

const protectNotesOnRefresh = (event: BeforeUnloadEvent) => {
  if (!hasNotes.value) return
  event.preventDefault()
  event.returnValue = ''
}
onMounted(() => window.addEventListener('beforeunload', protectNotesOnRefresh))
onBeforeUnmount(() => window.removeEventListener('beforeunload', protectNotesOnRefresh))
</script>

<style scoped>
.route-review {
  min-width: 0;
  border: 1px solid #ddd3c7;
  border-radius: 20px;
  background: #fffefa;
  color: #322d29;
  overflow: hidden;
}
.review-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 24px;
  border-bottom: 1px solid #e7dfd5;
}
.review-number {
  color: #8b302c;
  font-size: 13px;
  font-weight: 700;
  line-height: 28px;
  letter-spacing: .12em;
}
h2 { margin: 0; font-size: 20px; font-weight: 700; line-height: 1.4; }
.review-heading p, .review-checklist > p, .review-summary, .review-empty {
  margin: 8px 0 0;
  color: #65584e;
  font-size: 13px;
  line-height: 1.75;
  overflow-wrap: anywhere;
}
.review-day { border-bottom: 1px solid #e7dfd5; }
.review-day summary {
  padding: 18px 24px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.review-day summary:hover { background: #f7f2eb; }
.review-day-label { color: #8b302c; font-weight: 700; margin-inline: 8px 12px; }
.review-day-title { font-weight: 600; }
.review-note-indicator { display: inline-block; margin-inline-start: 10px; font-size: 12px; color: #486143; }
.review-day-body { padding: 0 24px 18px; }
.review-summary { margin: 0 0 16px; }
.review-day-body > label { display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; }
textarea {
  display: block;
  width: 100%;
  resize: vertical;
  min-height: 100px;
  padding: 12px;
  border: 1px solid #b6a898;
  border-radius: 10px;
  background: #fff;
  color: #322d29;
  font: inherit;
  font-size: 14px;
  line-height: 1.65;
}
textarea::placeholder { color: #716256; }
.review-note-footer, .review-undo { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; }
.review-note-footer { margin-top: 6px; color: #65584e; font-size: 12px; }
.review-text-button { min-height: 44px; padding: 8px 4px; color: #8b302c; font-weight: 600; font-size: 13px; text-decoration: underline; text-underline-offset: 3px; }
.review-text-button:disabled { opacity: .6; cursor: wait; }
.review-undo { padding: 8px 24px; background: #f3f4ed; font-size: 13px; }
.review-checklist { margin: 24px; padding: 0; min-width: 0; }
.review-checklist legend { padding: 0; font-size: 15px; font-weight: 700; }
.review-checklist > p { margin: 0 0 12px; }
.review-check { display: flex; gap: 10px; align-items: flex-start; padding: 10px 0; cursor: pointer; font-size: 13px; line-height: 1.75; }
.review-check input { width: 18px; height: 18px; flex: none; margin-top: 3px; accent-color: #8b302c; }
.review-check span { overflow-wrap: anywhere; }
.review-checklist .review-check-count { margin: 12px 0 0; font-size: 12px; }
.review-empty { padding: 0 24px; }
summary:focus-visible, textarea:focus-visible, button:focus-visible, input:focus-visible { outline: 3px solid #2d5f8a; outline-offset: 3px; }
@media (max-width: 480px) {
  .review-heading { padding: 20px 16px; }
  .review-day summary { padding: 16px; }
  .review-day-body { padding: 0 16px 16px; }
  .review-checklist { margin: 20px 16px; }
  .review-undo { padding-inline: 16px; }
  .review-empty { padding-inline: 16px; }
}
</style>
