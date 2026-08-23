<template>
  <div class="min-h-screen bg-tibet-white">
    <div v-if="loading" class="flex h-screen items-center justify-center" role="status" :aria-label="t('spotDetail.loadingSpot')">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-tibet-gold"></div>
      <span class="sr-only">{{ t('spotDetail.loadingSpot') }}</span>
    </div>

    <div v-else-if="detailError" class="flex min-h-[70vh] items-center justify-center px-4 py-20">
      <div class="mx-auto max-w-xl rounded-3xl border border-tibet-gold/20 bg-white p-6 text-center shadow-xl shadow-tibet-dark/10 sm:p-8" role="alert">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-tibet-red/10 text-tibet-red">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 4h.01M4.93 19h14.14c1.54 0 2.5-1.67 1.73-3L13.73 4c-.77-1.33-2.69-1.33-3.46 0L3.2 16c-.77 1.33.19 3 1.73 3z" />
          </svg>
        </div>
        <h1 class="mb-2 text-2xl font-bold text-tibet-dark tibetan-font">{{ t('spotDetail.detailErrorTitle') }}</h1>
        <p class="mx-auto mb-6 max-w-md text-sm leading-6 text-tibet-brown/70 tibetan-font">{{ detailError }}</p>
        <div class="flex flex-col justify-center gap-3 sm:flex-row">
          <button
            type="button"
            class="inline-flex min-h-11 items-center justify-center rounded-full bg-tibet-dark px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 active:scale-95 tibetan-font"
            @click="fetchSpotDetail"
          >
            {{ t('spotDetail.retryLoad') }}
          </button>
          <router-link
            to="/spots"
            class="inline-flex min-h-11 items-center justify-center rounded-full border border-tibet-gold/25 px-6 py-2.5 text-sm font-semibold text-tibet-brown transition hover:bg-tibet-gold/10 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
          >
            {{ t('spotDetail.backToSpots') }}
          </router-link>
        </div>
      </div>
    </div>

    <div v-else-if="spot" class="relative">
      <!-- Immersive Header Image -->
      <motion.div
        class="relative h-[46vh] min-h-[420px] w-full overflow-hidden bg-gray-200 sm:h-[60vh]"
        :initial="{ opacity: 0, scale: 1.02 }"
        :animate="{ opacity: 1, scale: 1 }"
        :transition="{ duration: 0.5, ease: motionEase }"
      >
        <img v-if="hasSpotImage"
             :src="spot.imageUrl"
             :alt="spot.name"
             loading="eager"
             class="w-full h-full object-cover img-fade-in"
             @error="handleSpotImageError">
        <div v-else class="w-full h-full flex items-center justify-center" :class="getGradientClass(spot)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-40 w-40 text-white opacity-30 animate-pulse-slow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="spot.category === 'NATURAL'" stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <div class="absolute inset-0 bg-gradient-to-t from-tibet-dark/80 via-transparent to-transparent"></div>
        
        <motion.div
          class="absolute bottom-0 left-0 w-full p-5 sm:p-8 md:p-16 text-white"
          :initial="{ opacity: 0, y: 24 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.6, delay: 0.2, ease: motionEase }"
        >
          <div class="max-w-7xl mx-auto">
            <div class="mb-4 flex flex-wrap items-center gap-2 sm:gap-4">
              <span class="px-4 py-1.5 bg-white/20 backdrop-blur-md rounded-full text-sm font-bold border border-white/30 tibetan-font">
                {{ spot.category === 'NATURAL' ? t('spotDetail.natural') : t('spotDetail.cultural') }}
              </span>
              <div class="flex min-w-0 flex-wrap gap-2">
                <span v-for="tag in spot.tags || []" :key="tag.id" class="max-w-full truncate rounded-full border border-white/10 bg-black/30 px-3 py-1 text-xs font-medium backdrop-blur-sm">
                  #{{ tag.tag }}
                </span>
              </div>
            </div>
            <h1 class="mb-4 break-words text-3xl font-bold leading-tight sm:text-5xl md:text-6xl tibetan-font">{{ spot.name }}</h1>
            <div class="flex flex-wrap items-center gap-3 text-white/80 sm:gap-6">
              <span class="flex min-w-0 items-center break-words tibetan-font">
                <svg xmlns="http://www.w3.org/2000/svg" class="mr-2 h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {{ t('spotDetail.tibetAutonomousRegion') }}
              </span>
              <span class="break-words text-2xl font-bold leading-tight text-tibet-gold">{{ formattedUnitPrice }}</span>
            </div>
          </div>
        </motion.div>
      </motion.div>

      <!-- Content Section -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 -mt-12 sm:-mt-20 relative z-10">
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-3 lg:gap-12">
          <!-- Left Column: Description -->
          <motion.div
            class="lg:col-span-2 space-y-8"
            :initial="revealInitial"
            :whileInView="revealInView"
            :inViewOptions="inViewOnce"
            :transition="{ duration: 0.5, delay: 0.1, ease: motionEase }"
          >
            <div class="bg-white rounded-3xl p-5 shadow-xl border border-tibet-gold/20 sm:p-8">
              <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">{{ t('spotDetail.introduction') }}</h2>
              <p class="break-words text-lg leading-loose text-tibet-brown/80 whitespace-pre-line tibetan-font">
                {{ spot.description || t('spotDetail.noDescription') }}
              </p>
            </div>

            <!-- Comments Section -->
            <div class="bg-white rounded-3xl p-5 shadow-xl border border-tibet-gold/20 sm:p-8">
              <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">
                {{ t('spotDetail.comments') }}
                <span v-if="commentsTotalElements > 0" class="text-sm font-normal text-gray-500">
                  ({{ comments.length }}<template v-if="commentsTotalElements > comments.length"> / {{ commentsTotalElements }}</template>)
                </span>
              </h2>
              
              <!-- Comment Form -->
              <div v-if="user" class="mb-8 rounded-2xl bg-gray-50 p-4 sm:p-6">
                <h3 class="text-lg font-bold text-gray-800 mb-4 tibetan-font">{{ t('spotDetail.postComment') }}</h3>
                <div class="flex items-center mb-4">
                  <span class="mr-4 text-gray-600 tibetan-font">{{ t('spotDetail.rating') }}:</span>
                  <div class="flex space-x-1">
                    <button v-for="star in 5" :key="star" type="button" @click="commentForm.rating = star"
                            class="flex min-h-11 min-w-11 items-center justify-center rounded-full text-2xl transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60"
                            :aria-label="t('spotDetail.setRating', { rating: star })"
                            :aria-pressed="star <= commentForm.rating"
                            :class="star <= commentForm.rating ? 'text-yellow-400' : 'text-gray-300'">
                      ★
                    </button>
                  </div>
                </div>
                <textarea v-model="commentForm.content" rows="3"
                          class="w-full p-4 rounded-xl border border-tibet-gold/25 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition-all duration-300 input-focus mb-3 resize-none tibetan-font"
                          :aria-label="t('spotDetail.shareExperience')"
                          :placeholder="t('spotDetail.shareExperience')"></textarea>
                <div class="mb-4">
                  <label class="block text-sm font-medium text-gray-600 mb-2 tibetan-font">{{ t('spotDetail.addPhoto') }}</label>
                  <div class="flex flex-wrap items-center gap-3 sm:gap-4">
                    <button v-if="!commentImageFile" type="button"
                           class="inline-flex min-h-11 items-center rounded-full border border-tibet-gold/25 bg-white px-4 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-blue-50 hover:text-blue-600 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
                           @click="triggerCommentImageInput">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 15a4 4 0 004 4h10a4 4 0 004-4m-4-8h-4m0 0V3m0 4l3-3m-3 3L9 4" />
                      </svg>
                      {{ t('spotDetail.selectImage') }}
                    </button>
                    <span v-if="uploadingCommentImage" class="text-sm text-blue-500 tibetan-font flex items-center">
                      <svg class="animate-spin w-4 h-4 mr-1" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                      {{ t('spotDetail.uploading') }}
                    </span>
                    <span v-else-if="uploadedCommentImageUrl" class="text-sm text-green-600 tibetan-font flex items-center">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                      {{ t('spotDetail.imageUploaded') }}
                    </span>
                    <span v-else-if="commentImageFileName" class="text-sm text-gray-500 truncate max-w-[200px]">{{ commentImageFileName }}</span>
                    <span v-else class="text-sm text-gray-400 tibetan-font">{{ t('spotDetail.imageFormats') }}</span>
                  </div>
                  <input id="comment-image-input" ref="commentImageInput" type="file" accept="image/*" class="hidden" @change="handleCommentImageChange" :disabled="uploadingCommentImage">
                  <p class="text-xs text-gray-400 mt-1 tibetan-font">{{ t('spotDetail.imageSizeHint') }}</p>
                  <div v-if="commentImagePreview" class="mt-4 relative w-40 h-28">
                    <img :src="commentImagePreview" :alt="t('spotDetail.comments')" class="w-full h-full object-cover rounded-2xl border border-tibet-gold/20 shadow-sm">
                    <div v-if="uploadingCommentImage" class="absolute inset-0 bg-black/40 rounded-2xl flex items-center justify-center">
                      <svg class="animate-spin w-6 h-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                    </div>
                    <button v-if="!uploadingCommentImage" type="button" @click="removeSelectedCommentImage"
                            class="absolute -right-2 -top-2 flex min-h-8 min-w-8 items-center justify-center rounded-full bg-white p-2 text-gray-500 shadow hover:text-red-500 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60"
                            :aria-label="t('spotDetail.removeSelectedImage')">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="mt-4 text-right">
                  <button type="button" @click="submitComment" :disabled="submittingComment || uploadingCommentImage"
                          class="min-h-11 rounded-full bg-tibet-red px-6 py-3 font-semibold text-tibet-yellow transition-all duration-300 transform hover:scale-105 hover:bg-tibet-red/85 hover:shadow-lg hover:shadow-tibet-red/25 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 disabled:transform-none focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font">
                    {{ submittingComment ? t('spotDetail.submitting') : t('spotDetail.publishComment') }}
                  </button>
                </div>
              </div>
              <div v-else class="mb-8 p-6 bg-gray-50 rounded-2xl text-center">
                <p class="text-gray-500 tibetan-font">{{ t('spotDetail.loginToComment') }}</p>
                <router-link to="/login" class="inline-block mt-2 text-blue-600 hover:underline tibetan-font">{{ t('spotDetail.goToLogin') }}</router-link>
              </div>

              <!-- Comment List -->
              <div v-if="commentsLoading" class="rounded-2xl bg-gray-50 px-4 py-8 text-center text-sm text-gray-500 tibetan-font" role="status">
                <div class="mx-auto mb-3 h-8 w-8 animate-spin rounded-full border-b-2 border-tibet-gold"></div>
                {{ t('spotDetail.commentsLoading') }}
              </div>
              <div v-else-if="commentsError" class="rounded-2xl border border-tibet-red/15 bg-tibet-red/5 px-4 py-6 text-center" role="alert">
                <p class="mb-4 text-sm leading-6 text-tibet-brown/75 tibetan-font">{{ commentsError }}</p>
                <button
                  type="button"
                  class="inline-flex min-h-11 items-center justify-center rounded-full bg-white px-5 py-2 text-sm font-semibold text-tibet-red shadow-sm transition hover:bg-tibet-red/5 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
                  @click="fetchComments"
                >
                  {{ t('spotDetail.retryComments') }}
                </button>
              </div>
              <div v-else class="space-y-6">
                <div v-for="comment in comments" :key="comment.id" class="border-b border-tibet-gold/20 pb-6 last:border-0">
                  <div class="mb-3 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div class="flex min-w-0 items-center space-x-3">
                      <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-100 font-bold text-blue-600">
                        {{ getCommentAuthor(comment).charAt(0) }}
                      </div>
                      <div class="min-w-0">
                        <p class="truncate font-bold text-gray-900">{{ getCommentAuthor(comment) }}</p>
                        <p class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</p>
                      </div>
                    </div>
                    <div class="flex shrink-0 items-center gap-3">
                      <button
                        v-if="isOwnComment(comment)"
                        type="button"
                        @click="deleteComment(comment)"
                        class="inline-flex min-h-9 items-center rounded-full px-2 text-xs font-medium text-red-500 transition-colors hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 tibetan-font"
                        :aria-label="t('spotDetail.deleteCommentAria')"
                      >
                        {{ t('common.delete') }}
                      </button>
                      <div class="flex text-yellow-400" role="img" :aria-label="t('spotDetail.commentRating', { rating: comment.rating || 0 })">
                        <span v-for="n in 5" :key="n">{{ n <= (comment.rating || 0) ? '★' : '☆' }}</span>
                      </div>
                    </div>
                  </div>
                  <p class="mb-3 break-words text-gray-600 leading-relaxed sm:pl-[3.25rem]">{{ comment.content }}</p>
                  
                  <!-- 评论图片 -->
                  <div v-if="comment.imageUrl" class="mb-3 sm:pl-[3.25rem]">
                    <img :src="comment.imageUrl" :alt="getCommentImageAlt(comment)"
                         loading="lazy"
                         role="button"
                         tabindex="0"
                         :aria-label="t('spotDetail.openCommentImage')"
                         class="h-auto w-full max-w-md cursor-pointer rounded-2xl bg-gray-100 object-cover shadow-md transition-shadow hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2"
                         @click="openImageModal(comment.imageUrl)"
                         @keydown.enter.prevent="openImageModal(comment.imageUrl)"
                         @keydown.space.prevent="openImageModal(comment.imageUrl)"
                         @error="handleCommentImageError($event)">
                  </div>
                  
                  <!-- 点赞按钮 -->
                  <div class="flex items-center space-x-4 sm:pl-[3.25rem]">
                    <button v-if="user" type="button" @click="toggleLike(comment)"
                            class="group flex min-h-11 items-center space-x-2 rounded-full px-2 text-gray-500 transition-all duration-300 hover:text-red-500 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60"
                            :aria-label="comment.liked ? t('spotDetail.unlikeComment') : t('spotDetail.likeComment')"
                            :aria-pressed="Boolean(comment.liked)">
                      <svg xmlns="http://www.w3.org/2000/svg" 
                           :class="comment.liked ? 'fill-red-500 text-red-500' : 'fill-none'"
                           class="w-5 h-5 transition-all duration-300 group-hover:scale-125 transform" 
                           viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" 
                              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span :class="comment.liked ? 'text-red-500 font-semibold transform scale-110' : ''" 
                            class="transition-all duration-300">
                        {{ comment.likeCount || 0 }}
                      </span>
                    </button>
                    <span v-else class="flex items-center space-x-2 text-gray-400" :aria-label="t('spotDetail.likeCount', { count: comment.likeCount || 0 })">
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" 
                              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span>{{ comment.likeCount || 0 }}</span>
                    </span>
                  </div>
                </div>
                <div v-if="comments.length === 0" class="text-center text-gray-400 py-8 tibetan-font">
                  {{ t('spotDetail.noComments') }}
                </div>
                <div
                  v-if="commentsTotalPages > 1"
                  class="mt-6 flex flex-wrap items-center justify-center gap-3"
                  role="navigation"
                  :aria-label="t('spotDetail.comments')"
                >
                  <span class="text-sm text-gray-500" role="status" aria-live="polite">
                    {{ commentsPage + 1 }} / {{ commentsTotalPages }}
                  </span>
                  <button
                    type="button"
                    @click="loadNextCommentsPage"
                    :disabled="commentsLoadingMore || !hasMoreComments"
                    :aria-busy="commentsLoadingMore"
                    class="min-h-11 rounded-xl border border-tibet-gold/25 bg-white px-5 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50 tibetan-font"
                  >
                    {{ commentsLoadingMore ? t('common.loading') : t('community.nextPage') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Interactive Map -->
            <div class="bg-white rounded-3xl p-5 shadow-xl border border-tibet-gold/20 overflow-hidden sm:p-8">
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                <div>
                  <h2 class="text-2xl font-bold text-tibet-dark tibetan-font">{{ t('spotDetail.location') }}</h2>
                  <p class="text-sm text-tibet-brown/70 mt-1 tibetan-font">{{ t('spotDetail.mapHint') }}</p>
                </div>
                <button
                  type="button"
                  @click="recenterMap"
                  :disabled="!mapReady || !hasValidLocation"
                  :aria-label="t('spotDetail.backToSpot')"
                  class="inline-flex min-h-11 items-center justify-center rounded-2xl px-4 py-2 text-sm font-semibold transition-all focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font"
                  :class="!mapReady || !hasValidLocation
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-blue-600/10 text-blue-600 hover:bg-blue-600/20'">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none"
                       stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364l-1.414 1.414M7.05 16.95l-1.414 1.414m0-11.314L7.05 7.05m10.607 10.607l1.414 1.414" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  {{ t('spotDetail.backToSpot') }}
                </button>
              </div>
              <div class="relative rounded-2xl overflow-hidden">
                <div v-if="!hasValidLocation" class="bg-tibet-gold/5 h-72 flex items-center justify-center text-tibet-brown/50 text-center px-6">
                  <div class="tibetan-font">
                    <p>{{ t('spotDetail.noLocationInfo') }}</p>
                    <p class="text-sm mt-2">{{ t('spotDetail.tryLater') }}</p>
                  </div>
                </div>
                <div v-else>
                  <div ref="mapContainer" class="h-72 w-full"></div>
                  <div
                    v-if="mapLoading"
                    class="absolute inset-0 bg-white/70 backdrop-blur-sm flex flex-col items-center justify-center text-tibet-brown/70 text-sm tibetan-font">
                    <svg class="animate-spin h-6 w-6 text-blue-500 mb-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                    </svg>
                    {{ t('spotDetail.mapLoading') }}
                  </div>
                </div>
              </div>
              <div class="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm text-tibet-brown/80">
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.longitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.longitude || '—' }}</p>
                </div>
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.latitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.latitude || '—' }}</p>
                </div>
                <div class="bg-tibet-white rounded-2xl px-4 py-3">
                  <p class="text-xs text-tibet-brown/50 tibetan-font">{{ t('spotDetail.altitude') }}</p>
                  <p class="font-semibold mt-1">{{ spot.altitude ? spot.altitude + ' m' : '—' }}</p>
                </div>
              </div>
            </div>
          </motion.div>

          <!-- Right Column: Booking Form -->
          <motion.div
            class="lg:col-span-1"
            :initial="revealInitial"
            :whileInView="revealInView"
            :inViewOptions="inViewOnce"
            :transition="{ duration: 0.5, delay: 0.25, ease: motionEase }"
          >
              <div class="sticky top-20">
              <div id="spot-booking-panel" class="glass-card rounded-3xl p-5 border border-white/50 sm:p-8">
                <h2 class="text-2xl font-bold text-tibet-dark mb-6 tibetan-font">{{ t('spotDetail.bookNow') }}</h2>
                
                <form @submit.prevent="handleBooking" class="space-y-6">
                  <div>
                    <label class="block text-sm font-medium text-tibet-dark/80 mb-2 tibetan-font">{{ t('spotDetail.visitDate') }}</label>
                    <input type="date" v-model="bookingForm.visitDate" required :min="minVisitDate"
                           :aria-label="t('spotDetail.visitDate')"
                           class="min-h-11 w-full rounded-xl border border-tibet-gold/25 bg-white/50 px-4 py-3 outline-none transition-all focus:border-tibet-gold focus:ring-2 focus:ring-blue-100">
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-tibet-dark/80 mb-2 tibetan-font">{{ t('spotDetail.ticketCount') }}</label>
                    <div class="flex items-center space-x-4">
                      <button type="button" @click="decreaseTickets" :disabled="!canDecreaseTickets"
                              :aria-label="t('spotDetail.decreaseTickets')"
                              class="flex h-11 w-11 items-center justify-center rounded-full bg-tibet-gold/5 text-tibet-brown/80 transition-all duration-300 transform hover:scale-110 hover:bg-tibet-gold/15 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:transform-none focus:outline-none focus:ring-2 focus:ring-tibet-gold/60">
                        <Minus class="h-4 w-4" aria-hidden="true" />
                      </button>
                      <span class="w-8 text-center text-xl font-bold text-tibet-dark transition-all duration-300" aria-live="polite">{{ bookingForm.ticketCount }}</span>
                      <button type="button" @click="increaseTickets" :disabled="!canIncreaseTickets"
                              :aria-label="t('spotDetail.increaseTickets')"
                              class="flex h-11 w-11 items-center justify-center rounded-full bg-tibet-gold/5 text-tibet-brown/80 transition-all duration-300 transform hover:scale-110 hover:bg-tibet-gold/15 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:transform-none focus:outline-none focus:ring-2 focus:ring-tibet-gold/60">
                        <Plus class="h-4 w-4" aria-hidden="true" />
                      </button>
                    </div>
                  </div>

                  <div class="pt-6 border-t border-tibet-gold/25">
                    <div class="mb-6 flex items-start justify-between gap-3">
                      <span class="text-tibet-brown/80 tibetan-font">{{ t('spotDetail.totalAmount') }}</span>
                      <span class="max-w-[55%] break-words text-right text-3xl font-bold leading-tight text-tibet-gold">{{ formattedTotalPrice }}</span>
                    </div>
                    <p class="mb-3 rounded-xl bg-tibet-gold/10 px-3 py-2 text-xs leading-5 text-tibet-brown/75 tibetan-font">
                      {{ priceSeasonHint }}
                    </p>
                    <p class="mb-4 rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800 tibetan-font">
                      {{ t('spotDetail.noPlatformPaymentHint') }}
                    </p>
                    
                    <button type="submit" :disabled="submitting"
                            :aria-label="submitting ? t('spotDetail.processing') : t('spotDetail.externalBook')"
                            class="w-full rounded-2xl bg-tibet-red px-6 py-4 font-bold text-tibet-yellow shadow-lg transition-all duration-300 transform hover:-translate-y-0.5 hover:scale-[1.02] hover:shadow-tibet-red/30 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:transform-none focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2 tibetan-font">
                      {{ submitting ? t('spotDetail.processing') : t('spotDetail.externalBook') }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  </div>

  <MobileStickyActionBar
    :show="Boolean(spot)"
    :eyebrow="t('spotDetail.totalAmount')"
    :title="formattedTotalPrice"
    :meta="spot?.name"
    :primary-label="submitting ? t('spotDetail.processing') : t('spotDetail.externalBook')"
    :primary-disabled="submitting"
    @primary="handleStickyPrimary"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { motion } from 'motion-v'
import { motionEase, revealInitial, revealInView, inViewOnce } from '../motion/presets'
import api, { endpoints } from '../api'
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata } from '../api/endpoints'
import type { ScenicSpotCommentItem } from '../api'
import MobileStickyActionBar from '../components/MobileStickyActionBar.vue'
import { useAuthStore } from '../stores/auth'
import { openExternalBooking } from '../utils/externalBooking'
import { createTextPopupContent } from '../utils/domText'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import { summarizeClientError } from '../utils/errorMonitoring'
import { toIntlLocale } from '../i18n/formatting'
import type * as Leaflet from 'leaflet'
import { Minus, Plus } from 'lucide-vue-next'

const { t, locale } = useI18n()
const { showConfirm } = useConfirm()
const { showToast } = useToast()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const spot = ref<any>(null)
let detailRequestId = 0

// Number('abc') is NaN and NaN never equals itself, so comparing raw Number(route.params.id) values
// would make every stale-request guard below fail open and strand the loading flags forever.
const routeSpotId = (): number | null => {
  const parsed = Number(route.params.id)
  return Number.isFinite(parsed) ? parsed : null
}
const isCurrentSpot = (id: number | null): boolean => id !== null && id === routeSpotId()
const loading = ref(true)
const detailError = ref('')
const submitting = ref(false)
const spotImageFailed = ref(false)
const mapContainer = ref<HTMLElement | null>(null)
const mapReady = ref(false)
const mapLoading = ref(true)
let map: Leaflet.Map | null = null
let marker: Leaflet.CircleMarker | null = null
let leafletLoader: Promise<typeof Leaflet> | null = null
const MAX_TICKET_COUNT = 9

const toDateInputValue = (date: Date) => {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 10)
}

const minVisitDate = toDateInputValue(new Date())

const loadLeaflet = async () => {
  if (!leafletLoader) {
    leafletLoader = Promise.all([
      import('leaflet'),
      import('leaflet/dist/leaflet.css')
    ]).then(([leaflet]) => leaflet)
  }

  return leafletLoader
}

const bookingForm = ref({
  visitDate: minVisitDate,
  ticketCount: 1
})

// 计算当前日期下的单张门票价格（考虑淡季/旺季/冬季免票）
// 规则：
// - 旺季：5月1日-10月31日，优先使用 peakSeasonPrice，否则 ticketPrice
// - 淡季：11月1日-次年4月30日，优先使用 offSeasonPrice，否则 ticketPrice
// - 其中每年 1 月 1 日 - 3 月 31 日统一实施免票政策（价格为 0）
const unitPrice = computed(() => {
  if (!spot.value) return 0

  const base = Number(spot.value.ticketPrice || 0)
  const visitDateStr = bookingForm.value.visitDate
  if (!visitDateStr) return base

  const visit = new Date(`${visitDateStr}T00:00:00`)

  const month = visit.getMonth() + 1 // 1-12
  const day = visit.getDate()

  // 1-3 月统一免票
  if (month >= 1 && month <= 3) {
    return 0
  }

  const inRange = (start?: string, end?: string) => {
    if (!start || !end) return false
    const s = new Date(`${start}T00:00:00`)
    const e = new Date(`${end}T00:00:00`)
    return visit >= s && visit <= e
  }

  const isOnOrAfter = (m: number, d: number) =>
    month > m || (month === m && day >= d)
  const isOnOrBefore = (m: number, d: number) =>
    month < m || (month === m && day <= d)

  const isPeakSeason =
    isOnOrAfter(5, 1) && isOnOrBefore(10, 31) // 5月1日-10月31日

  // 旺季：优先使用 peakSeasonPrice
  if (isPeakSeason && spot.value.peakSeasonPrice != null) {
    return Number(spot.value.peakSeasonPrice)
  }

  // 淡季：11月1日-次年4月30日，优先使用 offSeasonPrice
  const isOffSeason = !isPeakSeason // 其余日期全部视为淡季
  if (isOffSeason && spot.value.offSeasonPrice != null) {
    return Number(spot.value.offSeasonPrice)
  }

  // 默认基础票价
  return base
})

const totalPrice = computed(() => {
  return unitPrice.value * bookingForm.value.ticketCount
})

const formatCurrency = (price: number) => {
  if (price <= 0) return t('common.freeTicket')
  return t('common.priceCny', { price })
}

const formattedUnitPrice = computed(() => formatCurrency(unitPrice.value))
const formattedTotalPrice = computed(() => formatCurrency(totalPrice.value))

const canDecreaseTickets = computed(() => bookingForm.value.ticketCount > 1)
const canIncreaseTickets = computed(() => bookingForm.value.ticketCount < MAX_TICKET_COUNT)

const decreaseTickets = () => {
  if (canDecreaseTickets.value) {
    bookingForm.value.ticketCount -= 1
  }
}

const increaseTickets = () => {
  if (canIncreaseTickets.value) {
    bookingForm.value.ticketCount += 1
  }
}

const priceSeasonHint = computed(() => {
  const visitDateStr = bookingForm.value.visitDate
  if (!visitDateStr) return t('spotDetail.basePriceHint')

  const visit = new Date(`${visitDateStr}T00:00:00`)
  const month = visit.getMonth() + 1
  const day = visit.getDate()
  const isOnOrAfter = (m: number, d: number) =>
    month > m || (month === m && day >= d)
  const isOnOrBefore = (m: number, d: number) =>
    month < m || (month === m && day <= d)

  if (month >= 1 && month <= 3) {
    return t('spotDetail.winterFreeHint')
  }

  if (isOnOrAfter(5, 1) && isOnOrBefore(10, 31)) {
    return t('spotDetail.peakSeasonHint')
  }

  return t('spotDetail.offSeasonHint')
})

const hasSpotImage = computed(() => Boolean(spot.value?.imageUrl) && !spotImageFailed.value)

const handleSpotImageError = () => {
  spotImageFailed.value = true
}

const hasValidLocation = computed(() => {
  if (!spot.value) return false
  const lat = Number(spot.value.latitude)
  const lng = Number(spot.value.longitude)
  return !Number.isNaN(lat) && !Number.isNaN(lng)
})

const fetchSpotDetail = async () => {
  const requestId = ++detailRequestId
  const requestedSpotId = routeSpotId()
  loading.value = true
  detailError.value = ''
  if (requestedSpotId === null) {
    spot.value = null
    mapReady.value = false
    mapLoading.value = false
    detailError.value = t('spotDetail.detailErrorMessage')
    loading.value = false
    return
  }
  try {
    const response = await api.get(endpoints.spots.detail(requestedSpotId))
    if (requestId !== detailRequestId || !isCurrentSpot(requestedSpotId)) return
    spotImageFailed.value = false
    spot.value = response.data
    await nextTick()
    if (requestId !== detailRequestId || !isCurrentSpot(requestedSpotId)) return
    initOrUpdateMap()
  } catch (error) {
    if (requestId !== detailRequestId || !isCurrentSpot(requestedSpotId)) return
    console.error('Failed to fetch spot detail:', summarizeClientError(error))
    spot.value = null
    mapReady.value = false
    mapLoading.value = false
    detailError.value = t('spotDetail.detailErrorMessage')
  } finally {
    if (requestId === detailRequestId) {
      loading.value = false
    }
  }
}

watch(
  () => [spot.value?.longitude, spot.value?.latitude],
  () => {
    if (!loading.value) {
      initOrUpdateMap()
    }
  }
)

// WGS-84 → GCJ-02 坐标转换（国内地图需要）
const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

const outOfChina = (lng: number, lat: number): boolean =>
  lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

const transformLat = (x: number, y: number): number => {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

const transformLon = (x: number, y: number): number => {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

const wgs84ToGcj02 = (lng: number, lat: number): [number, number] => {
  if (outOfChina(lng, lat)) return [lng, lat]
  const dlat = transformLat(lng - 105.0, lat - 35.0)
  const dlng = transformLon(lng - 105.0, lat - 35.0)
  const radlat = lat / 180.0 * PI
  let magic = Math.sin(radlat)
  magic = 1 - EE * magic * magic
  const sqrtmagic = Math.sqrt(magic)
  const mglat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
  const mglng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI)
  return [lng + mglng, lat + mglat]
}

const buildSpotPopupContent = (name: string | undefined, wgsLng: number, wgsLat: number) => {
  return createTextPopupContent(
    name || t('spotDetail.location'),
    `${t('spotDetail.longitude')} ${wgsLng}, ${t('spotDetail.latitude')} ${wgsLat}`
  )
}

const initOrUpdateMap = async () => {
  if (!hasValidLocation.value || !mapContainer.value) {
    return
  }

  const wgsLat = Number(spot.value.latitude)
  const wgsLng = Number(spot.value.longitude)
  // 转为 GCJ-02 以匹配高德地图瓦片
  const [lng, lat] = wgs84ToGcj02(wgsLng, wgsLat)

  await nextTick()
  const L = await loadLeaflet()

  if (!map) {
    mapLoading.value = true
    map = L.map(mapContainer.value, {
      zoomControl: false,
      attributionControl: false
    })

    // 高德地图瓦片（国内加载快，无需 API Key）
    const tileLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      minZoom: 3,
      subdomains: '1234'
    })

    let tilesLoaded = false
    tileLayer.on('load', () => {
      tilesLoaded = true
      mapLoading.value = false
    })
    tileLayer.on('loading', () => {
      mapLoading.value = true
    })
    tileLayer.on('tileerror', () => {
      mapLoading.value = false
    })
    tileLayer.addTo(map)

    // 8 秒超时兜底
    setTimeout(() => {
      if (!tilesLoaded) {
        mapLoading.value = false
      }
    }, 8000)
  }

  mapReady.value = true
  map.setView([lat, lng], map.getZoom() || 8, { animate: true })

  if (marker) {
    marker.remove()
  }

  marker = L.circleMarker([lat, lng], {
    radius: 12,
    color: '#2563eb',
    weight: 3,
    fillColor: '#60a5fa',
    fillOpacity: 0.7
  }).addTo(map)

  marker.bindPopup(buildSpotPopupContent(spot.value.name, wgsLng, wgsLat))
}

const recenterMap = () => {
  if (!map || !hasValidLocation.value) return
  const wgsLat = Number(spot.value.latitude)
  const wgsLng = Number(spot.value.longitude)
  const [lng, lat] = wgs84ToGcj02(wgsLng, wgsLat)
  map.flyTo([lat, lng], 9, { duration: 0.6 })
  marker?.openPopup()
}

onBeforeUnmount(() => {
  map?.remove()
  map = null
  marker = null
  clearCommentImagePreview()
})

const scrollToBooking = () => {
  document.getElementById('spot-booking-panel')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const handleStickyPrimary = () => {
  handleBooking()
}

const handleBooking = async () => {
  if (!bookingForm.value.visitDate || bookingForm.value.visitDate < minVisitDate) {
    showToast(t('spotDetail.pleaseSelectDate'), 'warning')
    return
  }
  const opened = openExternalBooking({
    kind: 'spot',
    name: spot.value?.name,
    location: spot.value?.location || t('spotDetail.tibetAutonomousRegion'),
    date: bookingForm.value.visitDate
  })
  if (!opened) {
    showToast(t('spotDetail.externalBookBlocked'), 'warning')
  }
}

const user = computed(() => auth.user)
const comments = ref<ScenicSpotCommentItem[]>([])
const scenicSpotCommentsPageSize = 20
const commentsPageInfo = ref<PageMetadata>({
  page: 0,
  size: scenicSpotCommentsPageSize,
  totalElements: 0,
  totalPages: 0
})
const commentsLoadingMore = ref(false)
const submittingComment = ref(false)
let commentsRequestId = 0
let commentsPageRequestId = 0
const likingCommentIds = new Set<number>()
const commentForm = ref({
  content: '',
  rating: 5
})
const commentImageFile = ref<File | null>(null)
const commentImageInput = ref<HTMLInputElement | null>(null)
const commentImagePreview = ref('')
const uploadedCommentImageUrl = ref<string | null>(null)
const uploadingCommentImage = ref(false)
const commentsLoading = ref(false)
const commentsError = ref('')
const commentImageFileName = computed(() => commentImageFile.value?.name || '')
const commentsPage = computed(() => commentsPageInfo.value.page)
const commentsTotalPages = computed(() => commentsPageInfo.value.totalPages)
const commentsTotalElements = computed(() => commentsPageInfo.value.totalElements)
const hasMoreComments = computed(() => hasNextPage(commentsPageInfo.value))

const applyCommentsPage = (response: { data?: unknown; headers?: unknown }, append = false) => {
  const page = readPaginatedResponse<ScenicSpotCommentItem>(response, {
    page: append ? commentsPageInfo.value.page + 1 : 0,
    size: scenicSpotCommentsPageSize
  })
  const pageComments = page.content.map(comment => ({
    ...comment,
    liked: Boolean(comment.liked)
  }))

  comments.value = append ? mergeUniqueById(comments.value, pageComments) : pageComments
  commentsPageInfo.value = {
    page: page.page,
    size: page.size || scenicSpotCommentsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const fetchCommentsPage = async (
  page = 0,
  append = false,
  requestedSpotId: number | null = routeSpotId(),
  isStale: () => boolean = () => false
) => {
  if (requestedSpotId === null) return
  const response = await api.get(endpoints.comments.list(requestedSpotId), {
    params: { page, size: scenicSpotCommentsPageSize }
  })
  if (isStale() || !isCurrentSpot(requestedSpotId)) return
  applyCommentsPage(response, append)
}

const fetchComments = async () => {
  const requestId = ++commentsRequestId
  const requestedSpotId = routeSpotId()
  const isStale = () => requestId !== commentsRequestId
  commentsLoading.value = true
  commentsError.value = ''
  if (requestedSpotId === null) {
    commentsError.value = t('spotDetail.commentsLoadFailed')
    commentsLoading.value = false
    return
  }
  try {
    await fetchCommentsPage(0, false, requestedSpotId, isStale)
  } catch (error) {
    if (isStale() || !isCurrentSpot(requestedSpotId)) return
    console.error('Failed to fetch comments:', summarizeClientError(error))
    commentsError.value = t('spotDetail.commentsLoadFailed')
  } finally {
    // commentsRequestId is only bumped here, so whoever superseded us owns the flag and will clear it.
    if (!isStale()) {
      commentsLoading.value = false
    }
  }
}

const loadNextCommentsPage = async () => {
  if (commentsLoadingMore.value || !hasMoreComments.value) return
  // Pagination keeps its own counter: sharing commentsRequestId with fetchComments meant a refresh
  // invalidated the in-flight page and left commentsLoadingMore stuck true, disabling the button.
  const pageRequestId = ++commentsPageRequestId
  const refreshIdAtStart = commentsRequestId
  const requestedSpotId = routeSpotId()
  const isStale = () => pageRequestId !== commentsPageRequestId || refreshIdAtStart !== commentsRequestId
  if (requestedSpotId === null) return
  commentsLoadingMore.value = true
  try {
    await fetchCommentsPage(commentsPageInfo.value.page + 1, true, requestedSpotId, isStale)
  } catch (error) {
    if (isStale() || !isCurrentSpot(requestedSpotId)) return
    console.error('Failed to load more scenic spot comments:', summarizeClientError(error))
    showToast(t('spotDetail.commentsLoadFailed'), 'error')
  } finally {
    if (pageRequestId === commentsPageRequestId) {
      commentsLoadingMore.value = false
    }
  }
}

function clearCommentImagePreview() {
  if (commentImagePreview.value) {
    URL.revokeObjectURL(commentImagePreview.value)
    commentImagePreview.value = ''
  }
}

function triggerCommentImageInput() {
  if (!uploadingCommentImage.value) {
    commentImageInput.value?.click()
  }
}

function removeSelectedCommentImage() {
  commentImageFile.value = null
  uploadedCommentImageUrl.value = null
  if (commentImageInput.value) {
    commentImageInput.value.value = ''
  }
  clearCommentImagePreview()
}

async function handleCommentImageChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {
    removeSelectedCommentImage()
    return
  }

  if (!file.type.startsWith('image/')) {
    showToast(t('spotDetail.selectImageFile'), 'warning')
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    showToast(t('spotDetail.imageTooLarge'), 'warning')
    target.value = ''
    return
  }

  commentImageFile.value = file
  uploadedCommentImageUrl.value = null
  clearCommentImagePreview()
  commentImagePreview.value = URL.createObjectURL(file)

  // Upload immediately after selection
  uploadingCommentImage.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadResponse = await api.post(endpoints.comments.uploadImage, formData)
    uploadedCommentImageUrl.value = uploadResponse.data.imageUrl
  } catch (error) {
    console.error('Failed to upload image:', summarizeClientError(error))
    showToast(t('spotDetail.commentFailed'), 'error')
    removeSelectedCommentImage()
  } finally {
    uploadingCommentImage.value = false
  }
}

const submitComment = async () => {
  if (submittingComment.value || !spot.value || !commentForm.value.content.trim()) return

  const requestedSpotId = routeSpotId()
  if (requestedSpotId === null) return

  if (!(await auth.ensureSession())) {
    router.push('/login')
    return
  }

  submittingComment.value = true
  try {
    await api.post(endpoints.comments.create, {
      spotId: requestedSpotId,
      content: commentForm.value.content,
      rating: commentForm.value.rating,
      imageUrl: uploadedCommentImageUrl.value
    })
    
    // Reset form and refresh list
    if (!isCurrentSpot(requestedSpotId)) return
    commentForm.value.content = ''
    commentForm.value.rating = 5
    removeSelectedCommentImage()
    await fetchComments()
    showToast(t('spotDetail.commentSuccess'), 'success')
  } catch (error) {
    console.error('Failed to submit comment:', summarizeClientError(error))
    showToast(t('spotDetail.commentFailed'), 'error')
  } finally {
    submittingComment.value = false
  }
}

const getCommentAuthor = (comment: ScenicSpotCommentItem) => {
  return comment.user?.nickname || comment.nickname || t('routeDetail.anonymous')
}

const getCommentImageAlt = (comment: ScenicSpotCommentItem) => {
  return t('spotDetail.commentImageAlt', { name: getCommentAuthor(comment) })
}

const isOwnComment = (comment: ScenicSpotCommentItem) => {
  if (!user.value) return false
  return comment.owner === true
}

const deleteComment = async (comment: ScenicSpotCommentItem) => {
  const confirmed = await showConfirm({
    message: t('spotDetail.confirmDeleteComment'),
    confirmLabel: t('common.delete'),
    cancelLabel: t('common.cancel'),
    tone: 'danger'
  })
  if (!confirmed) return

  try {
    await api.delete(endpoints.comments.delete(comment.id))
    comments.value = comments.value.filter(item => item.id !== comment.id)
    const totalElements = Math.max(0, commentsPageInfo.value.totalElements - 1)
    commentsPageInfo.value = {
      ...commentsPageInfo.value,
      totalElements,
      totalPages: commentsPageInfo.value.size > 0 ? Math.ceil(totalElements / commentsPageInfo.value.size) : 0
    }
  } catch (error) {
    console.error('Failed to delete comment:', summarizeClientError(error))
    showToast(t('spotDetail.deleteCommentFailed'), 'error')
  }
}

const toggleLike = async (comment: ScenicSpotCommentItem) => {
  if (likingCommentIds.has(comment.id)) return
  likingCommentIds.add(comment.id)
  const requestedSpotId = routeSpotId()
  if (!(await auth.ensureSession())) {
    router.push('/login')
    likingCommentIds.delete(comment.id)
    return
  }
  
  try {
    const response = await api.post(endpoints.comments.like(comment.id))
    const currentComment = comments.value.find(item => item.id === comment.id)
    if (isCurrentSpot(requestedSpotId) && currentComment) {
      currentComment.liked = response.data.liked
      currentComment.likeCount = response.data.likeCount
    }
  } catch (error) {
    console.error('Failed to toggle like:', summarizeClientError(error))
    showToast(t('spotDetail.operationFailed'), 'error')
  } finally {
    likingCommentIds.delete(comment.id)
  }
}

const openImageModal = (imageUrl: string) => {
  try {
    const url = new URL(imageUrl, window.location.origin)
    if (!['http:', 'https:', 'blob:'].includes(url.protocol)) {
      return
    }
    window.open(url.href, '_blank', 'noopener,noreferrer')
  } catch {
    // Ignore malformed comment image URLs.
  }
}

const handleCommentImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  const errorText = encodeURIComponent(t('spotDetail.imageLoadFailed'))
  img.src = `data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%23e5e7eb" width="400" height="300"/%3E%3Ctext x="50%25" y="50%25" font-family="Arial" font-size="16" fill="%239ca3af" text-anchor="middle" dy=".3em"%3E${errorText}%3C/text%3E%3C/svg%3E`
}

const getGradientClass = (spot: any) => {
  if (!spot) return 'bg-gradient-to-br from-blue-500 via-blue-600 to-indigo-700'
  
  const gradients = [
    'bg-gradient-to-br from-tibet-blue via-tibet-dark to-tibet-red',
    'bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow',
    'bg-gradient-to-br from-tibet-turquoise via-tibet-blue to-tibet-dark',
    'bg-gradient-to-br from-tibet-brown via-tibet-red to-tibet-gold',
    'bg-gradient-to-br from-tibet-dark via-tibet-blue to-tibet-turquoise',
    'bg-gradient-to-br from-tibet-gold via-tibet-yellow to-tibet-white'
  ]
  
  const index = spot.id % gradients.length
  return gradients[index]
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString(toIntlLocale(locale.value))
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  void fetchSpotDetail()
})

watch(() => route.params.id, () => {
  comments.value = []
  commentsPageInfo.value = {
    page: 0,
    size: scenicSpotCommentsPageSize,
    totalElements: 0,
    totalPages: 0
  }
  commentsLoadingMore.value = false
  commentsError.value = ''
  removeSelectedCommentImage()
  void fetchSpotDetail()
  void fetchComments()
})

onMounted(async () => {
  void fetchSpotDetail()
  await auth.refreshSession()
  void fetchComments()
})
</script>
