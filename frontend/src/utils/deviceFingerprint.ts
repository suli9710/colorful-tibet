let fpPromise: Promise<string> | null = null

export function getDeviceFingerprint(): Promise<string> {
  if (!fpPromise) {
    fpPromise = (async () => {
      try {
        const FingerprintJS = (await import('@fingerprintjs/fingerprintjs')).default
        const fp = await FingerprintJS.load()
        const result = await fp.get()
        return result.visitorId
      } catch {
        return ''
      }
    })()
  }
  return fpPromise
}
