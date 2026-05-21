export {}

declare global {
  interface Window {
    grecaptcha?: {
      ready(callback: () => void): void
      execute(siteKey: string, options: { action: string }): Promise<string>
      render(
        container: string | HTMLElement,
        parameters: {
          sitekey: string
          theme?: 'light' | 'dark'
          size?: 'normal' | 'compact'
          callback?: (token: string) => void
          'expired-callback'?: () => void
          'error-callback'?: () => void
        }
      ): number
      getResponse(widgetId?: number): string
      reset(widgetId?: number): void
    }
  }
}
