import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: '',
        // 不重写路径，保留 /api 前缀，因为后端路径包含 /api
      },
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (!normalizedId.includes('/node_modules/')) return
          if (
            normalizedId.includes('/node_modules/echarts/') ||
            normalizedId.includes('/node_modules/zrender/')
          ) return 'echarts'
          if (normalizedId.includes('/node_modules/leaflet/')) return 'leaflet'
          if (normalizedId.includes('/node_modules/motion-')) return 'motion'
          if (normalizedId.includes('/node_modules/lucide-vue-next/')) return 'ui'
          if (normalizedId.includes('/node_modules/marked/') || normalizedId.includes('/node_modules/dompurify/')) return 'markdown'
          if (
            normalizedId.includes('/node_modules/@vue/') ||
            normalizedId.includes('/node_modules/vue/') ||
            normalizedId.includes('/node_modules/vue-router/') ||
            normalizedId.includes('/node_modules/pinia/') ||
            normalizedId.includes('/node_modules/vue-i18n/')
          ) return 'vendor'
        },
      },
    },
  },
  esbuild: command === 'build'
    ? {
        drop: ['console', 'debugger'],
      }
    : undefined,
  css: {
    postcss: './postcss.config.js',
  },
}))
