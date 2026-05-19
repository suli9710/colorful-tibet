let loadPromise: Promise<void> | null = null

function loadScript(siteKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.querySelector('script[src*="recaptcha"]')) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = `https://www.recaptcha.net/recaptcha/api.js?render=${siteKey}`
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load reCAPTCHA'))
    document.head.appendChild(script)
  })
}

export function loadRecaptcha(siteKey: string): Promise<void> {
  if (!loadPromise) {
    loadPromise = loadScript(siteKey)
  }
  return loadPromise
}

export async function getRecaptchaToken(action: string): Promise<string> {
  const siteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY
  if (!siteKey) return ''

  try {
    await loadRecaptcha(siteKey)
    return await window.grecaptcha.execute(siteKey, { action })
  } catch {
    return ''
  }
}
