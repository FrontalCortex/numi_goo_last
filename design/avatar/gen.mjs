// assets/avatar/<stil>_parts.json dosyalarını DiceBear paketlerinden üretir (bkz. AvatarArt.kt).
//
// Kullanım (her stil için):
//   npm pack @dicebear/<stil>  →  tar xzf ...tgz  (package/ klasörü çıkar)
//   package/node_modules/@dicebear/core/index.js içine: export const escape={xml:s=>s};
//   (package.json'u {"name":"@dicebear/core","type":"module","main":"index.js"})
//   node gen.mjs <package klasörü> > app/src/main/assets/avatar/<stil>_parts.json
//
// Çıktı: { grup: { seçenek: "SVG şablonu" } }. Renkler {{c:hair}}, iç parçalar {{p:eyes}}
// yer tutucusu. Paketin index.js'indeki gövde de "root.default" olarak eklenir.
//
// mix-blend-mode: AndroidSVG desteklemiyor, sessizce normal karıştırmaya düşüyor ve beyaz
// "overlay" parıltıları yüzü soluk gösteriyordu. Bu yüzden burada yaklaşık karşılıklarına
// çevriliyor: overlay → normal, opaklık yarıya; lighten ve screen (beyaz parıltılar) → normal; multiply
// (saç renginde hafif koyulaştırma) → %10 siyah.
import fs from 'fs';
import path from 'path';
import { pathToFileURL } from 'url';

const dir = path.resolve(process.argv[2] ?? '.');
const C = await import(pathToFileURL(path.join(dir, 'lib/components/index.js')));
const escape = { xml: (s) => s };
const components = new Proxy({}, { get: (_, k) => ({ value: () => `{{p:${String(k)}}}` }) });
const colors = new Proxy({}, { get: (_, k) => `{{c:${String(k)}}}` });

function fixBlend(svg) {
  return svg
    .replace(/style="mix-blend-mode:overlay"\s*opacity="([\d.]+)"/g, (_, o) => `opacity="${(+o / 2).toFixed(2)}"`)
    .replace(/opacity="([\d.]+)"\s*style="mix-blend-mode:overlay"/g, (_, o) => `opacity="${(+o / 2).toFixed(2)}"`)
    .replace(/style="mix-blend-mode:overlay"/g, 'opacity=".15"')
    .replace(/\s*style="mix-blend-mode:lighten"/g, '')
    .replace(/\s*style="mix-blend-mode:screen"/g, '')
    .replace(/style="mix-blend-mode:multiply" opacity="[\d.]+" fill="[^"]*"/g, 'opacity=".1" fill="#000"');
}

const out = {};
for (const [group, obj] of Object.entries(C)) {
  out[group] = {};
  for (const [name, fn] of Object.entries(obj)) out[group][name] = fixBlend(fn(components, colors));
}

// Gövde: index.js'deki create() içindeki `body: \`...\`` şablonu.
const src = fs.readFileSync(path.join(dir, 'lib/index.js'), 'utf8');
const m = src.match(/body: (`[\s\S]*?`),\n\s*extra/);
if (m) {
  const body = new Function('components', 'colors', 'escape', `var _a,_b,_c,_d,_e,_f,_g,_h,_j,_k,_l,_m,_o,_p,_q,_r; return ${m[1]};`);
  out.root = { default: fixBlend(body(components, colors, escape)) };
}
if (/mix-blend-mode/.test(JSON.stringify(out))) console.error('UYARI: dönüştürülmemiş mix-blend-mode kaldı');
process.stdout.write(JSON.stringify(out));
