const AMAP_KEY = '329d4cce7166c0e9644f064f96cad55c'
const AMAP_SECURITY_CODE = '745455a6d68cc4c31928c7a60fd11d0c'

let amapLoader: Promise<any> | null = null

export const loadAmap = () => {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('AMap can only be loaded in the browser.'))
  }

  if ((window as any).AMap) {
    return Promise.resolve((window as any).AMap)
  }

  if (amapLoader) return amapLoader

  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: AMAP_SECURITY_CODE
  }

  amapLoader = new Promise((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>('script[data-amap-loader="colorful-tibet"]')

    if (existingScript) {
      existingScript.addEventListener('load', () => resolve((window as any).AMap), { once: true })
      existingScript.addEventListener('error', () => reject(new Error('Failed to load AMap script.')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.async = true
    script.dataset.amapLoader = 'colorful-tibet'
    script.onload = () => resolve((window as any).AMap)
    script.onerror = () => reject(new Error('Failed to load AMap script.'))
    document.head.appendChild(script)
  })

  return amapLoader
}
