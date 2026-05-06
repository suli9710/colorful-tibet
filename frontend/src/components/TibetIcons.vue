<template>
  <!-- 藏式元素 SVG 图标集 -->
</template>

<!--
  集中管理的藏式装饰 SVG 元素
  使用方式: <TibetIcon type="knot" class="w-8 h-8" />
-->

<!-- 金刚结 / 无尽结 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" id="tibet-knot">
  <path d="M50 10v30M30 20h40M20 40v20M80 40v20M30 70h40M50 60v30" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round"/>
  <path d="M50 10c-15 0-20 15-20 30s5 30 20 30M50 10c15 0 20 15 20 30s-5 30-20 30" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round"/>
  <circle cx="50" cy="40" r="4" fill="currentColor"/>
</svg>

<!-- 法轮 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" id="tibet-dharma">
  <circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="4" fill="none"/>
  <circle cx="50" cy="50" r="8" fill="currentColor"/>
  <circle cx="50" cy="50" r="18" stroke="currentColor" stroke-width="3" fill="none"/>
  <line x1="50" y1="10" x2="50" y2="32" stroke="currentColor" stroke-width="4"/>
  <line x1="50" y1="68" x2="50" y2="90" stroke="currentColor" stroke-width="4"/>
  <line x1="10" y1="50" x2="32" y2="50" stroke="currentColor" stroke-width="4"/>
  <line x1="68" y1="50" x2="90" y2="50" stroke="currentColor" stroke-width="4"/>
  <line x1="22" y1="22" x2="37" y2="37" stroke="currentColor" stroke-width="4"/>
  <line x1="63" y1="63" x2="78" y2="78" stroke="currentColor" stroke-width="4"/>
  <line x1="22" y1="78" x2="37" y2="63" stroke="currentColor" stroke-width="4"/>
  <line x1="63" y1="37" x2="78" y2="22" stroke="currentColor" stroke-width="4"/>
</svg>

<!-- 祥云 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 60" id="tibet-cloud">
  <path d="M20 40c-10-5-12-18-4-24 4-4 10-4 15-2 3-8 12-12 20-10 10 2 16 10 16 20 0 2 0 4-1 6 8 2 14 10 14 18 0 10-8 18-18 18H24c-8 0-14-6-14-14 0-6 4-12 10-14z" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round"/>
</svg>

<!-- 莲花 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" id="tibet-lotus">
  <ellipse cx="50" cy="60" rx="40" ry="12" stroke="currentColor" stroke-width="2" fill="none"/>
  <path d="M50 30c-15 5-25 15-30 30 5-10 15-20 30-25 15 5 25 15 30 25-5-15-15-25-30-30z" stroke="currentColor" stroke-width="2" fill="none"/>
  <line x1="50" y1="25" x2="50" y2="15" stroke="currentColor" stroke-width="2"/>
</svg>

<!-- 藏式花纹边框角 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60" id="tibet-corner">
  <path d="M10 50V10h40M10 10l10 10M50 10L40 20M10 50l10-10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>
  <circle cx="10" cy="10" r="3" fill="currentColor"/>
</svg>

<!-- 玛尼石纹理 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80" id="tibet-mani">
  <rect x="10" y="10" width="60" height="60" rx="8" stroke="currentColor" stroke-width="2" fill="none"/>
  <circle cx="40" cy="40" r="20" stroke="currentColor" stroke-width="2" fill="none"/>
  <path d="M22 22l36 36M22 58l36-36" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
</svg>

<!-- 藏式雪山 -->
<svg v-if="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 60" id="tibet-mountain">
  <polygon points="60,5 80,30 75,30 85,50 35,50 45,30 40,30" stroke="currentColor" stroke-width="2" fill="none"/>
  <polygon points="30,15 45,35 42,35 48,50 15,50 22,35 18,35" stroke="currentColor" stroke-width="1.5" fill="none" opacity="0.6"/>
  <polygon points="85,12 95,32 92,32 98,50 75,50 80,32 78,32" stroke="currentColor" stroke-width="1.5" fill="none" opacity="0.6"/>
  <rect x="0" y="50" width="120" height="10" fill="currentColor" opacity="0.1"/>
</svg>
</template>
