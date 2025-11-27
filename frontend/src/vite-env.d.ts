/// <reference types="vite/client" />

declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}

// 高德地图类型声明
declare global {
  interface Window {
    AMap: any
    _AMapSecurityConfig: {
      securityJsCode: string
    }
  }
}