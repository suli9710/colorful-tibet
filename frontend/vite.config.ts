import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import fs from 'fs'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  define: {
    __INTLIFY_JIT_COMPILATION__: true,
  },
  plugins: [
    vue(),
    VueI18nPlugin({
      include: [path.resolve(__dirname, './src/i18n/locales/**')],
      runtimeOnly: true,
      compositionOnly: true
    })
  ],
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
        bypass: (req) => {
          const pathname = req.url?.split('?')[0]
          if (!pathname) return

          const publicAsset = path.resolve(__dirname, 'public', pathname.replace(/^\/+/, ''))
          if (fs.existsSync(publicAsset)) {
            return pathname
          }
        },
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
  test: {
    environment: 'node',
    globals: true,
    include: ['src/**/*.test.ts']
  },
}))
