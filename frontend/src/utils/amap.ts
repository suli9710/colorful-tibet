import { applyThirdPartyScriptSecurity } from './scriptSecurity'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

let amapLoader: Promise<any> | null = null

export const loadAmap = () => {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('AMap can only be loaded in the browser.'))
  }

  if ((window as any).AMap) {
    return Promise.resolve((window as any).AMap)
  }

  if (amapLoader) return amapLoader

  if (!AMAP_KEY) {
    return Promise.reject(new Error('AMap key is not configured. Set VITE_AMAP_KEY before building the frontend.'))
  }

  if (AMAP_SECURITY_CODE) {
    ;(window as any)._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE
    }
  }

  amapLoader = new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>('script[data-amap-loader="colorful-tibet"]')

    if (existingScript) {
      applyThirdPartyScriptSecurity(existingScript, 'amap')
      existingScript.addEventListener('load', () => resolve((window as any).AMap), { once: true })
      existingScript.addEventListener('error', () => reject(new Error('Failed to load AMap script.')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.async = true
    script.dataset.amapLoader = 'colorful-tibet'
    applyThirdPartyScriptSecurity(script, 'amap')
    script.onload = () => resolve((window as any).AMap)
    script.onerror = () => reject(new Error('Failed to load AMap script.'))
    document.head.appendChild(script)
  })

  return amapLoader
}
