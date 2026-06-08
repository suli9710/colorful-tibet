/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_RECAPTCHA_ENABLED?: string
  readonly VITE_RECAPTCHA_MODE?: string
  readonly VITE_RECAPTCHA_SITE_KEY?: string
  readonly VITE_CSP_NONCE?: string
  readonly VITE_RECAPTCHA_SCRIPT_INTEGRITY?: string
  readonly VITE_RECAPTCHA_SCRIPT_CROSSORIGIN?: string
  readonly VITE_AMAP_KEY?: string
  readonly VITE_AMAP_SECURITY_CODE?: string
  readonly VITE_AMAP_SCRIPT_INTEGRITY?: string
  readonly VITE_AMAP_SCRIPT_CROSSORIGIN?: string
  readonly VITE_HEATMAP_REMOTE_GEO_FALLBACK_ENABLED?: string
  readonly VITE_HEATMAP_REMOTE_GEO_FALLBACK_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}

// 高德地图类型声明
declare global {
  interface Window {
    AMap: any
    __CSP_NONCE__?: string
    _AMapSecurityConfig?: {
      securityJsCode: string
    }
  }
}
