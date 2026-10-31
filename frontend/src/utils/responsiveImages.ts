import imageManifest from '../data/responsiveImages.json'
type Variant = { src: string; width: number; height: number; bytes: number }
const manifest: Record<string, Variant[]> = imageManifest
const variantsFor = (url: string): Variant[] => {
  // Only bundled, known artwork is rewritten; external/user-uploaded images are not fetched.
  try { return manifest[decodeURI(url)] ?? [] } catch { return [] }
}
export const responsiveImage = (url: string) => variantsFor(url).at(-1)?.src ?? url
export const responsiveSrcset = (url: string) => variantsFor(url).map((item) => `${item.src} ${item.width}w`).join(', ') || undefined
