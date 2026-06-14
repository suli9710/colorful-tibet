import fs from 'fs';

const bo = JSON.parse(fs.readFileSync('frontend/src/i18n/locales/heritage-content.bo.json', 'utf8'));
const zh = JSON.parse(fs.readFileSync('frontend/src/i18n/locales/heritage-content.zh.json', 'utf8'));
const raw = fs.readFileSync('frontend/src/i18n/locales/heritage-content.bo.json', 'utf8');
const lines = raw.split('\n');

const cjk = /[\u4e00-\u9fff\u3400-\u4dbf\uf900-\ufaff]/;
const tibetan = /[\u0F00-\u0FFF]/;

// PyEWTS corruption heuristic: uses subjoined signs 0F90-0FBC with latin-like clusters
// or contains ASCII latin letters
const hasLatinAscii = (s) => /[A-Za-z]/.test(s);
const hasPyewtsPattern = (s) => /[\u0F90-\u0FBC].*[a-z]|[\u0F90-\u0FBC]{2,}/.test(s) && /[oaeiuAEIOU]/.test(s.replace(/[\u0F00-\u0FFF]/g, ''));

function isLikelyCorruptedTibetan(s) {
  if (hasLatinAscii(s)) return true;
  // Common corruption: lots of subjoined consonants with vowel signs that look like latin transliteration
  const subjoined = (s.match(/[\u0F90-\u0FBC]/g) || []).length;
  const tibChars = (s.match(/[\u0F00-\u0FFF]/g) || []).length;
  if (subjoined > 8 && tibChars < 200) return true;
  // Specific garbage chars seen in file
  if (/[oaeiuyAEIOUY]/.test(s.replace(/[\u0F00-\u0FFF\s།་，。、；：！？""''（）《》【】·…—]/g, ''))) return true;
  return false;
}

function lineOfNeedle(needle) {
  const idx = raw.indexOf(needle);
  if (idx < 0) return null;
  return raw.slice(0, idx).split('\n').length;
}

const national = [];
for (const [cat, val] of Object.entries(bo.heritageContent.national.categories)) {
  for (const item of val.items) {
    const nameOk = item.name && tibetan.test(item.name) && !cjk.test(item.name);
    const descOk = item.description && tibetan.test(item.description) && !cjk.test(item.description);
    const nameCorrupt = isLikelyCorruptedTibetan(item.name || '');
    const descCorrupt = isLikelyCorruptedTibetan(item.description || '');
    national.push({ id: item.id, cat, nameOk, descOk, nameCorrupt, descCorrupt, name: item.name?.slice(0,40) });
  }
}

const corruptItems = national.filter(i => i.nameCorrupt || i.descCorrupt);
const missingNameDesc = national.filter(i => !i.nameOk || !i.descOk);

console.log('=== NATIONAL 68 ===');
console.log('corrupt (PyEWTS/garbage):', corruptItems.length);
console.log('missing name/desc tibetan:', missingNameDesc.length);
console.log('corrupt ids:', corruptItems.map(i => i.id).join(','));

// representative
const repCjk = [];
for (const [key, r] of Object.entries(bo.heritageContent.representative)) {
  for (const f of ['name', 'description', 'significance']) {
    if (cjk.test(r[f] || '')) repCjk.push({ key, f, line: lineOfNeedle(`"${f}": "${r[f]}"`) });
  }
  if (cjk.test(r.imageUrl)) repCjk.push({ key, f: 'imageUrl', v: r.imageUrl, line: lineOfNeedle(r.imageUrl) });
}

// CJK lines
const cjkLines = [];
lines.forEach((line, i) => {
  if (cjk.test(line)) cjkLines.push({ line: i + 1, text: line.trim().slice(0, 100) });
});

console.log('\n=== CJK ===');
console.log('total lines with CJK:', cjkLines.length);

console.log('\n=== STRUCTURE ===');
console.log('experienceSpots zh:', zh.heritageContent.experienceSpots.length, 'bo:', bo.heritageContent.experienceSpots.length);
console.log('imageAliases zh:', Object.keys(zh.heritageContent.imageAliases).length, 'bo:', Object.keys(bo.heritageContent.imageAliases).length);
console.log('extra bo imageAlias keys:', Object.keys(bo.heritageContent.imageAliases).filter(k => !zh.heritageContent.imageAliases[k]).length);

const aliasCjk = Object.entries(bo.heritageContent.aliases).filter(([k, v]) => cjk.test(k) || cjk.test(v));
console.log('aliases CJK:', aliasCjk);

console.log('\n=== OUTPUT JSON ===');
console.log(JSON.stringify({ corruptItems, repCjk, cjkLines, aliasCjk }, null, 2));
