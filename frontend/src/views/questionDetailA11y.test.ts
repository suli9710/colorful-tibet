import { describe, expect, it } from 'vitest'
import questionDetailSource from './QuestionDetail.vue?raw'
import rawExtraMessages from '../i18n/locales/extra.json'

const extraMessages = rawExtraMessages as {
  zh: { questionDetail: Record<string, unknown> }
  bo: { questionDetail: Record<string, unknown> }
}

describe('question detail action accessibility safeguards', () => {
  it('keeps question action buttons disabled and busy while requests are in flight', () => {
    expect(questionDetailSource).toContain('const liking = ref(false)')
    expect(questionDetailSource).toContain(':disabled="liking"')
    expect(questionDetailSource).toContain(':aria-busy="liking"')

    expect(questionDetailSource).toContain('const acceptingAnswerId = ref<number | null>(null)')
    expect(questionDetailSource).toContain(':disabled="acceptingAnswerId !== null"')
    expect(questionDetailSource).toContain(':aria-busy="acceptingAnswerId === answer.id"')

    expect(questionDetailSource).toContain('const deletingQuestion = ref(false)')
    expect(questionDetailSource).toContain(':disabled="deletingQuestion"')
    expect(questionDetailSource).toContain(':aria-busy="deletingQuestion"')

    expect(questionDetailSource).toContain('const isSubmitAnswerDisabled = computed(() => !newAnswer.value.trim() || answering.value)')
    expect(questionDetailSource).toContain(':disabled="isSubmitAnswerDisabled"')
    expect(questionDetailSource).toContain(':aria-busy="answering"')
  })

  it('gives the question like button a stable name, pressed state, and hidden decorative icon', () => {
    expect(questionDetailSource).toContain(':aria-label="questionLikeAccessibleName"')
    expect(questionDetailSource).toContain(':aria-pressed="isLiked"')
    expect(questionDetailSource).toContain('<Heart class="h-4 w-4" :fill="isLiked ? \'currentColor\' : \'none\'" aria-hidden="true" />')

    const likeNameSource = getSourceSection(
      'const questionLikeAccessibleName = computed(() => {',
      'const normalizePublicUserName = (value: unknown)'
    )
    expect(likeNameSource).toContain('const count = question.value?.likeCount ?? 0')
    expect(likeNameSource).toContain("return t('questionDetail.likeQuestionAria', {")
    expect(likeNameSource).toContain("t(isLiked.value ? 'questionDetail.unlikeQuestionAction' : 'questionDetail.likeQuestionAction')")
    expect(likeNameSource).toContain("t(isLiked.value ? 'questionDetail.likedState' : 'questionDetail.notLikedState')")
    expect(likeNameSource).toContain('count')
  })

  it('keeps localized question like accessible-name messages for supported locales', () => {
    const requiredKeys = [
      'likeQuestionAction',
      'unlikeQuestionAction',
      'likedState',
      'notLikedState',
      'likeQuestionAria'
    ]

    for (const locale of ['zh', 'bo'] as const) {
      const questionDetail = extraMessages[locale].questionDetail

      for (const key of requiredKeys) {
        expect(questionDetail).toHaveProperty(key)
        expect(questionDetail[key]).toBeDefined()
      }
    }
  })

  it('sets in-flight guards before awaiting auth or confirmation work', () => {
    const submitAnswerSource = getSourceSection('const submitAnswer = async () => {', 'const toggleLike = async () => {')
    expect(submitAnswerSource).toContain('if (!content || answering.value || !question.value) return')
    expect(submitAnswerSource).toContain('answering.value = true')
    expect(indexOf(submitAnswerSource, 'answering.value = true')).toBeLessThan(indexOf(submitAnswerSource, 'await requireAuth()'))

    const toggleLikeSource = getSourceSection('const toggleLike = async () => {', 'const acceptAnswer = async (answerId: number) => {')
    expect(toggleLikeSource).toContain('if (liking.value || !question.value) return')
    expect(toggleLikeSource).toContain('liking.value = true')
    expect(indexOf(toggleLikeSource, 'liking.value = true')).toBeLessThan(indexOf(toggleLikeSource, 'await requireAuth()'))

    const acceptAnswerSource = getSourceSection('const acceptAnswer = async (answerId: number) => {', 'const deleteQuestion = async () => {')
    expect(acceptAnswerSource).toContain('if (acceptingAnswerId.value !== null || !question.value) return')
    expect(acceptAnswerSource).toContain('acceptingAnswerId.value = answerId')
    expect(indexOf(acceptAnswerSource, 'acceptingAnswerId.value = answerId')).toBeLessThan(indexOf(acceptAnswerSource, 'await requireAuth()'))

    const deleteQuestionSource = getSourceSection('const deleteQuestion = async () => {', 'const formatDate = (dateStr: string) => {')
    expect(deleteQuestionSource).toContain('if (deletingQuestion.value || !question.value) return')
    expect(deleteQuestionSource).toContain('deletingQuestion.value = true')
    expect(indexOf(deleteQuestionSource, 'deletingQuestion.value = true')).toBeLessThan(indexOf(deleteQuestionSource, 'await requireAuth()'))
    expect(indexOf(deleteQuestionSource, 'deletingQuestion.value = true')).toBeLessThan(indexOf(deleteQuestionSource, 'showConfirm({'))
  })

  it('keeps answer input labelled and visible failures on local safe copy', () => {
    expect(questionDetailSource).toContain(':aria-label="t(\'community.yourAnswer\')"')
    expect(questionDetailSource).toContain("showToast(t('questionDetail.answerFailed'), 'error')")
    expect(questionDetailSource).toContain("showToast(t('questionDetail.acceptFailed'), 'error')")
    expect(questionDetailSource).toContain("showToast(t('questionDetail.deleteFailed'), 'error')")
    expect(questionDetailSource).not.toMatch(/\b\w+\.response\??\.\s*data\??\.\s*(?:error|message)\b/)
    expect(questionDetailSource).not.toMatch(/\b\w+\.message\b/)
  })
})

const getSourceSection = (startMarker: string, endMarker: string) => {
  const startIndex = questionDetailSource.indexOf(startMarker)
  const endIndex = questionDetailSource.indexOf(endMarker, startIndex + startMarker.length)

  if (startIndex === -1 || endIndex === -1) {
    throw new Error(`Missing source section between "${startMarker}" and "${endMarker}"`)
  }

  return questionDetailSource.slice(startIndex, endIndex)
}

const indexOf = (source: string, marker: string) => {
  const index = source.indexOf(marker)

  if (index === -1) {
    throw new Error(`Missing source marker "${marker}"`)
  }

  return index
}
