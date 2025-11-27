<template>
  <footer class="relative bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-950 text-white mt-20 overflow-hidden">
    <!-- 装饰性背景元素 -->
    <div class="absolute inset-0 opacity-10">
      <div class="absolute top-0 left-0 w-96 h-96 bg-purple-500 rounded-full blur-3xl"></div>
      <div class="absolute bottom-0 right-0 w-96 h-96 bg-blue-500 rounded-full blur-3xl"></div>
    </div>
    
    <!-- 顶部装饰线 -->
    <div class="relative h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent"></div>
    
    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-12">
        <!-- 公司信息 -->
        <div class="lg:col-span-2">
          <div class="mb-6">
            <h3 class="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 mb-4">
              七彩西藏
            </h3>
            <div class="w-20 h-1 bg-gradient-to-r from-blue-400 to-purple-400 rounded-full mb-6"></div>
            <p class="text-gray-300 text-base leading-relaxed max-w-md">
              探索西藏的神秘与美丽，体验高原上的独特文化。我们致力于为您提供最优质的西藏旅游服务，让每一次旅行都成为难忘的回忆。
            </p>
          </div>
          
          <!-- 社交媒体图标 -->
          <div class="flex items-center space-x-4">
            <!-- 微信 -->
            <a href="javascript:void(0)" @click="shareToWeChat" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 backdrop-blur-sm border border-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 cursor-pointer group" title="分享到微信">
              <svg class="w-5 h-5 text-gray-400 group-hover:text-green-400 transition-colors" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.597-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 3.633c-1.385-.077-3.755.538-5.28 1.988-1.816 1.508-2.702 3.867-1.802 6.337.247.684.585 1.316 1.008 1.88.06.081.13.16.196.24.06.098.12.195.17.293.05.098.09.195.12.293.03.049.05.098.07.147.03.098.05.195.07.293 0 .05.02.098.02.147 0 .098.01.195.01.293 0 1.88 1.53 3.406 3.41 3.406.77 0 1.5-.26 2.09-.72a.722.722 0 0 1 .75-.098l1.71.98c.05.029.098.049.157.049.15 0 .27-.127.27-.283 0-.072-.02-.142-.05-.213l-.4-1.48a.59.59 0 0 1 .22-.665c1.83-1.347 3-3.338 3-5.55.01-2.19-1.75-4.033-3.95-4.33-.05-.01-.11 0-.16 0zm-1.28 5.348c.49 0 .89-.4.89-.891a.897.897 0 0 0-.89-.892.897.897 0 0 0-.89.892c0 .49.4.89.89.89zm-4.5 0c.49 0 .89-.4.89-.891a.897.897 0 0 0-.89-.892.897.897 0 0 0-.89.892c0 .49.4.89.89.89z"/>
              </svg>
            </a>
            <!-- QQ -->
            <a :href="qqShareUrl" target="_blank" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 backdrop-blur-sm border border-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 cursor-pointer group" title="分享到QQ">
              <svg class="w-5 h-5 text-gray-400 group-hover:text-blue-400 transition-colors" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12.017 0C5.396 0 .029 5.367.029 11.987c0 5.079 3.158 9.417 7.618 11.174-.105-.949-.199-2.403.041-3.439.219-.937 1.406-5.957 1.406-5.957s-.359-.72-.359-1.781c0-1.663.967-2.911 2.168-2.911 1.024 0 1.518.769 1.518 1.688 0 1.029-.653 2.567-.992 3.992-.285 1.193.6 2.165 1.775 2.165 2.128 0 3.768-2.245 3.768-5.487 0-2.861-2.063-4.869-5.008-4.869-3.41 0-5.409 2.562-5.409 5.199 0 1.033.394 2.143.889 2.741.099.12.112.225.085.345-.09.375-.293 1.199-.334 1.363-.053.225-.172.271-.401.165-1.495-.69-2.433-2.878-2.433-4.646 0-3.776 2.748-7.252 7.92-7.252 4.158 0 7.392 2.967 7.392 6.923 0 4.135-2.607 7.462-6.233 7.462-1.214 0-2.357-.629-2.75-1.378l-.748 2.853c-.271 1.043-1.002 2.35-1.492 3.146C9.57 23.812 10.763 24.009 12.017 24.009c6.624 0 11.99-5.367 11.99-11.988C24.007 5.367 18.641.001 12.017.001z"/>
              </svg>
            </a>
            <!-- 微博 -->
            <a :href="weiboShareUrl" target="_blank" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 backdrop-blur-sm border border-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 cursor-pointer group" title="分享到微博">
              <svg class="w-5 h-5 text-gray-400 group-hover:text-red-400 transition-colors" fill="currentColor" viewBox="0 0 24 24">
                <path d="M9.839 18.761c-4.784 0-8.662-3.877-8.662-8.662 0-4.784 3.878-8.662 8.662-8.662 4.784 0 8.662 3.878 8.662 8.662 0 4.785-3.878 8.662-8.662 8.662zm0-15.383c-3.712 0-6.721 3.009-6.721 6.721s3.009 6.721 6.721 6.721 6.721-3.009 6.721-6.721-3.009-6.721-6.721-6.721zm5.74 2.94c.113 0 .204.092.204.204v1.412a.204.204 0 0 1-.204.204h-1.412a.204.204 0 0 1-.204-.204V6.522c0-.112.092-.204.204-.204zm2.855.204v1.412a.204.204 0 0 1-.204.204h-1.412a.204.204 0 0 1-.204-.204V6.522c0-.112.092-.204.204-.204h1.412c.112 0 .204.092.204.204zm-5.74 1.63c-2.618 0-4.74 2.122-4.74 4.74s2.122 4.74 4.74 4.74 4.74-2.122 4.74-4.74-2.122-4.74-4.74-4.74zm0 7.728a2.99 2.99 0 0 1-2.988-2.988c0-1.65 1.338-2.988 2.988-2.988s2.988 1.338 2.988 2.988a2.99 2.99 0 0 1-2.988 2.988zm-2.776-4.74c0 .153.124.277.277.277a.277.277 0 1 0 0-.554.277.277 0 0 0-.277.277zm5.553 0c0 .153.124.277.277.277a.277.277 0 1 0 0-.554.277.277 0 0 0-.277.277zm-2.776 1.11a.832.832 0 0 0-.832.832c0 .291.189.54.45.628a3.324 3.324 0 0 0 1.306.265c.46 0 .9-.092 1.306-.265a.832.832 0 0 0 .45-.628.832.832 0 0 0-.832-.832h-1.848z"/>
              </svg>
            </a>
          </div>
        </div>

        <!-- 快速链接 -->
        <div>
          <h4 class="text-lg font-semibold mb-6 text-white">快速导航</h4>
          <div class="space-y-3">
            <router-link 
              v-for="link in quickLinks" 
              :key="link.to"
              :to="link.to" 
              class="block text-gray-400 hover:text-white transition-all duration-300 group relative pl-4">
              <span class="absolute left-0 top-1/2 -translate-y-1/2 w-0 h-0.5 bg-gradient-to-r from-blue-400 to-purple-400 group-hover:w-2 transition-all duration-300"></span>
              <span class="flex items-center">
                <span>{{ link.label }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 ml-2 opacity-0 group-hover:opacity-100 transform group-hover:translate-x-1 transition-all duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </span>
            </router-link>
          </div>
        </div>

        <!-- 联系方式 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h4 class="text-lg font-semibold text-white">联系我们</h4>
            <button @click="showContactModal = true" 
                    class="text-xs text-blue-400 hover:text-blue-300 transition-all duration-300 flex items-center space-x-1 px-3 py-1.5 rounded-lg hover:bg-white/10 backdrop-blur-sm border border-white/10 hover:border-blue-400/50">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
              <span>详情</span>
            </button>
          </div>
          <div class="space-y-4">
            <a href="tel:19532458802" class="flex items-start space-x-3 group hover:translate-x-1 transition-transform duration-300">
              <div class="w-10 h-10 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center flex-shrink-0 group-hover:bg-blue-500/20 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              </div>
              <div>
                <p class="font-medium text-white">19532458802</p>
                <p class="text-xs text-gray-400 mt-0.5">周一至周五 10:00 - 18:00</p>
              </div>
            </a>

            <a href="mailto:lengzhehao@gmail.com" class="flex items-start space-x-3 group hover:translate-x-1 transition-transform duration-300">
              <div class="w-10 h-10 rounded-lg bg-purple-500/10 border border-purple-500/20 flex items-center justify-center flex-shrink-0 group-hover:bg-purple-500/20 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <div>
                <p class="font-medium text-white">lengzhehao@gmail.com</p>
                <p class="text-xs text-gray-400 mt-0.5">通常在 24 小时内回复</p>
              </div>
            </a>

            <div class="flex items-start space-x-3">
              <div class="w-10 h-10 rounded-lg bg-green-500/10 border border-green-500/20 flex items-center justify-center flex-shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <div>
                <p class="font-medium text-white">湖南省长沙市长沙县</p>
                <p class="text-xs text-gray-400 mt-0.5">湖南信息学院</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部版权 -->
      <div class="border-t border-white/10 pt-8">
        <div class="flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
          <p class="text-sm text-gray-400">
            &copy; 2025 <span class="text-white font-medium">七彩西藏旅游网</span>. All rights reserved.
          </p>
          <div class="flex items-center space-x-6 text-xs text-gray-500">
            <router-link to="/privacy" class="hover:text-gray-300 transition-colors">隐私政策</router-link>
            <router-link to="/terms" class="hover:text-gray-300 transition-colors">服务条款</router-link>
            <a href="#" class="hover:text-gray-300 transition-colors">关于我们</a>
          </div>
        </div>
      </div>
    </div>
  </footer>

  <!-- Contact Modal -->
  <ContactModal v-model="showContactModal" />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ContactModal from './ContactModal.vue'

const showContactModal = ref(false)

const quickLinks = [
  { to: '/', label: '首页' },
  { to: '/spots', label: '景点导览' },
  { to: '/route-planner', label: 'AI行程' },
  { to: '/community', label: '社区' },
  { to: '/heritage', label: '非遗文化' },
  { to: '/news', label: '资讯' }
]

// 分享相关
const shareTitle = '七彩西藏旅游 - 探索西藏的神秘与美丽'
const shareUrl = computed(() => window.location.href)
const shareDesc = '探索西藏的神秘与美丽，体验高原上的独特文化。我们致力于为您提供最优质的西藏旅游服务。'

// QQ分享链接
const qqShareUrl = computed(() => {
  const url = encodeURIComponent(shareUrl.value)
  const title = encodeURIComponent(shareTitle)
  const desc = encodeURIComponent(shareDesc)
  return `https://connect.qq.com/widget/shareqq/index.html?url=${url}&title=${title}&summary=${desc}&pics=&site=七彩西藏旅游`
})

// 微博分享链接
const weiboShareUrl = computed(() => {
  const url = encodeURIComponent(shareUrl.value)
  const title = encodeURIComponent(shareTitle)
  return `https://service.weibo.com/share/share.php?url=${url}&title=${title}`
})

// 微信分享处理
const shareToWeChat = () => {
  // 在移动端，可以调用微信JS-SDK
  // 在PC端，提示用户使用微信扫码分享
  if (/MicroMessenger/i.test(navigator.userAgent)) {
    // 微信内置浏览器，可以使用微信JS-SDK
    alert('请点击右上角菜单，选择"分享到朋友圈"或"发送给朋友"')
  } else {
    // PC端或其他浏览器，提示用户
    alert('请使用微信扫描二维码分享，或复制链接发送给微信好友')
  }
}
</script>

