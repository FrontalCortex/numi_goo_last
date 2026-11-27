'use strict';
/*
 * Cloud Functions deploy planı: son başarılı deploy'un commit'i (base) ile şimdiki kod (head)
 * arasında hangi fonksiyonların davranışının değişmiş olabileceğini bulur.
 *
 * Neden: firebase-tools "değişmeyeni atla" özelliği kaynak hash'ini bütün functions/
 * klasöründen tek parça hesaplıyor (deploy/functions/prepare.js → prepareFunctionsUpload);
 * tek satır değişince bütün fonksiyonlar yeniden deploy ediliyor (~25 dk, güncelleme kotası).
 *
 * Nasıl: her export için bir "parmak izi" hesaplanır:
 *   - export ifadesinin metni,
 *   - kullandığı bütün üst düzey bildirimlerin (function, const...) metni, geçişli olarak,
 *   - require ettiği yerel modül dosyalarının bütün içeriği, geçişli olarak
 *     (yerel modüller dosya bütünü olarak ele alınıyor, içleri ayrıştırılmıyor),
 *   - herkesi etkileyen ortak parça: package.json, package-lock.json, firebase.json'daki
 *     functions ayarı, üst düzey yan etkili ifadeler (admin.initializeApp() gibi), küresel
 *     durumu değiştiren satırlar (db.settings(, process.env.X = ...), kimsenin adını anmadığı
 *     JS dışı dosyalar.
 * Yükleme anında yerel kod çalıştıran export'lar (ör. scheduleFinalize(functions, admin, db, ...))
 * paylaşılan nesneleri değiştirebilir; bunlar, aynı nesneleri kullanan her fonksiyonun
 * parmak izine de katılıyor.
 *
 * Güvenlik ilkesi: emin olunamayan her durumda "full" (hepsi) döner. Bağımlılık isimle bulunur
 * (gölgeleme dikkate alınmaz); yani fazladan fonksiyon seçilebilir ama eksik seçilmez.
 *
 * Kullanım: node deploy-plan.js <acornYolu> <baseKökü> <headKökü>
 *   Kökler, içinde firebase.json ve functions/ olan repo kökleridir. Head'in
 *   functions/node_modules'u kurulu olmalı: gerçek fonksiyon listesi index.js yüklenerek alınıyor.
 * Çıktı (stdout, tek satır JSON): {mode: "none"|"partial"|"full", functions: [...], reason: "..."}
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

class FullDeploy extends Error {}
function full(reason) {
  throw new FullDeploy(reason);
}

function sha(parts) {
  const h = crypto.createHash('sha256');
  for (const p of parts) {
    const b = Buffer.isBuffer(p) ? p : Buffer.from(String(p), 'utf8');
    h.update(`${b.length}:`);
    h.update(b);
  }
  return h.digest('hex');
}

// Yükleme anında çağrılınca yalnızca değer üreten, yan etkisiz bilinen çağrılar.
const PURE_CALLS = new Set([
  'Object.freeze', 'Object.keys', 'Object.values', 'Object.entries', 'Object.fromEntries',
  'Array.from', 'Array.isArray', 'Array.of', 'JSON.parse', 'JSON.stringify',
  'Number', 'String', 'Boolean', 'BigInt', 'Symbol', 'parseInt', 'parseFloat', 'Date.now',
  'Number.parseInt', 'Number.parseFloat', 'encodeURIComponent', 'decodeURIComponent',
]);
const PURE_CTORS = new Set(['Set', 'Map', 'WeakMap', 'WeakSet', 'Date', 'RegExp', 'Error', 'URL']);
// Dış kütüphane nesnesi üzerinde çağrılınca küresel durumu değiştiren yöntemler.
const GLOBAL_METHODS = new Set([
  'initializeApp', 'settings', 'setGlobalOptions', 'config', 'setLogLevel', 'enableLogging', 'use',
]);
// Hangi dosyada olursa olsun küresel durumu değiştiren satırlar: herkesin parmak izine girer.
const GLOBAL_EFFECT_LINE = new RegExp([
  String.raw`initializeApp\s*\(`,
  String.raw`\.settings\s*\(`,
  String.raw`setGlobalOptions\s*\(`,
  String.raw`process\.env(\.\w+|\[[^\]]*\])\s*(=(?!=)|\+=|\|\|=|\?\?=|&&=)`,
  String.raw`delete\s+process\.env`,
  String.raw`\bglobal(This)?\s*(\.\w+|\[[^\]]*\])\s*=(?!=)`,
  String.raw`\.prototype\.\w+\s*=(?!=)`,
  String.raw`Object\.defineProperty\s*\(\s*(global|globalThis|process)\b`,
  String.raw`process\.on\s*\(`,
].join('|'));
const FUNCTION_TYPES = new Set(['FunctionDeclaration', 'FunctionExpression', 'ArrowFunctionExpression']);

function listFiles(dir, rel = '') {
  const out = [];
  for (const e of fs.readdirSync(path.join(dir, rel), { withFileTypes: true })) {
    if (e.name === 'node_modules' || e.name === '.git') continue;
    const r = rel ? `${rel}/${e.name}` : e.name;
    if (e.isDirectory()) out.push(...listFiles(dir, r));
    else if (e.isFile()) out.push(r);
  }
  return out;
}

function functionsConfig(root) {
  const fb = JSON.parse(fs.readFileSync(path.join(root, 'firebase.json'), 'utf8'));
  let cfg = fb.functions;
  if (!cfg) full('firebase.json içinde functions ayarı yok');
  if (!Array.isArray(cfg)) cfg = [cfg];
  if (cfg.length !== 1) full('birden fazla functions codebase var');
  return cfg[0];
}

function memberPath(expr) {
  // a.b.c → ['a','b','c']; çağrı zincirinde (a.b().c) çağrılar atlanır, require('x') köktür.
  const names = [];
  let e = expr;
  for (;;) {
    if (e.type === 'ChainExpression') e = e.expression;
    else if (e.type === 'CallExpression' && e.callee.type === 'Identifier' && e.callee.name === 'require') break;
    else if (e.type === 'MemberExpression') {
      names.unshift(e.computed ? '[]' : e.property.name);
      e = e.object;
    } else if (e.type === 'CallExpression') e = e.callee;
    else break;
  }
  return { root: e, names };
}

// require('x') çağrısının sabit yolu; sabit değilse null.
function requireSpec(call) {
  const a = call.arguments[0];
  if (call.arguments.length !== 1) return null;
  if (a.type === 'Literal' && typeof a.value === 'string') return a.value;
  if (a.type === 'TemplateLiteral' && a.expressions.length === 0) return a.quasis[0].value.cooked;
  return null;
}

/**
 * Bir AST alt ağacında kullanılan isimleri, require'ları, exports.X erişimlerini ve
 * yükleme anında (fonksiyon gövdesi dışında) çalışan çağrı/atamaları toplar.
 */
function scan(node, file, isMain) {
  const out = { names: new Set(), requires: new Set(), exportRefs: new Set(), loadCalls: [], loadMutation: false, loadUnknown: false };
  const visit = (n, parent, key, inFn) => {
    if (!n || typeof n.type !== 'string') return;
    switch (n.type) {
      case 'Identifier': {
        if (parent) {
          if (parent.type === 'MemberExpression' && key === 'property' && !parent.computed) return;
          if ((parent.type === 'Property' || parent.type === 'PropertyDefinition' || parent.type === 'MethodDefinition') &&
              key === 'key' && !parent.computed && !parent.shorthand) return;
          if ((parent.type === 'LabeledStatement' || parent.type === 'BreakStatement' || parent.type === 'ContinueStatement') && key === 'label') return;
          if (parent.type === 'MetaProperty') return;
        }
        if (n.name === 'require' && !(parent && parent.type === 'CallExpression' && key === 'callee')) {
          full(`${file}: require doğrudan çağrı dışında kullanılmış`);
        }
        if (isMain) {
          if (n.name === 'module' || n.name === 'eval') full(`${file}: '${n.name}' kullanılmış`);
          if (n.name === 'exports' && !(parent && parent.type === 'MemberExpression' && key === 'object' && !parent.computed)) {
            full(`${file}: exports nesnesi bütün olarak kullanılmış`);
          }
        }
        out.names.add(n.name);
        return;
      }
      case 'MemberExpression':
        if (isMain && n.object.type === 'Identifier' && n.object.name === 'exports') {
          if (n.computed) full(`${file}: exports[...] kullanılmış`);
          out.exportRefs.add(n.property.name);
        }
        break;
      case 'CallExpression':
        if (n.callee.type === 'Identifier' && n.callee.name === 'require') {
          const spec = requireSpec(n);
          if (spec === null) full(`${file}: değişken yollu require`);
          out.requires.add(spec);
        }
        if (!inFn) out.loadCalls.push(n);
        break;
      case 'NewExpression':
        if (!inFn) out.loadCalls.push(n);
        break;
      case 'ImportExpression':
        full(`${file}: dinamik import()`);
        break;
      case 'AssignmentExpression':
      case 'UpdateExpression':
        if (!inFn) out.loadMutation = true;
        break;
      case 'UnaryExpression':
        if (n.operator === 'delete' && !inFn) out.loadMutation = true;
        break;
      case 'TaggedTemplateExpression':
      case 'StaticBlock':
      case 'AwaitExpression':
        if (!inFn) out.loadUnknown = true;
        break;
      case 'PropertyDefinition':
        if (n.static && n.value && !inFn) out.loadUnknown = true;
        break;
    }
    const childInFn = inFn || FUNCTION_TYPES.has(n.type);
    for (const k of Object.keys(n)) {
      if (k === 'type' || k === 'start' || k === 'end' || k === 'loc' || k === 'range') continue;
      const v = n[k];
      if (Array.isArray(v)) for (const c of v) visit(c, n, k, childInFn);
      else if (v && typeof v.type === 'string') visit(v, n, k, childInFn);
    }
  };
  visit(node, null, null, false);
  return out;
}

function patternNames(p, out = []) {
  if (!p) return out;
  switch (p.type) {
    case 'Identifier': out.push(p.name); break;
    case 'ObjectPattern': for (const prop of p.properties) patternNames(prop.type === 'RestElement' ? prop.argument : prop.value, out); break;
    case 'ArrayPattern': for (const el of p.elements) patternNames(el, out); break;
    case 'RestElement': patternNames(p.argument, out); break;
    case 'AssignmentPattern': patternNames(p.left, out); break;
    default: full(`bilinmeyen bildirim kalıbı: ${p.type}`);
  }
  return out;
}

function analyze(root, acorn) {
  const cfg = functionsConfig(root);
  const srcDir = path.join(root, cfg.source || 'functions');
  const files = listFiles(srcDir);
  const fileSet = new Set(files);
  const read = (rel) => fs.readFileSync(path.join(srcDir, rel));
  const pkg = JSON.parse(read('package.json').toString('utf8'));
  let main = path.posix.normalize(pkg.main || 'index.js');
  if (!fileSet.has(main) && fileSet.has(`${main}.js`)) main = `${main}.js`;
  if (!fileSet.has(main)) full(`ana dosya bulunamadı: ${main}`);

  const parse = (text, rel) => {
    try {
      return acorn.parse(text, { ecmaVersion: 'latest', sourceType: 'script', allowHashBang: true, allowReturnOutsideFunction: true });
    } catch (e) {
      full(`${rel} ayrıştırılamadı: ${e.message}`);
    }
  };
  const isLocal = (spec) => spec.startsWith('./') || spec.startsWith('../') || spec.startsWith('/');
  const resolve = (fromRel, spec) => {
    const base = path.posix.normalize(path.posix.join(path.posix.dirname(fromRel), spec));
    for (const c of [base, `${base}.js`, `${base}.cjs`, `${base}.json`, `${base}/index.js`]) {
      if (fileSet.has(c)) return c;
    }
    full(`${fromRel}: '${spec}' çözülemedi`);
  };
  const isJs = (rel) => /\.(c?js)$/.test(rel);

  // Yerel modül grafiği (ana dosya hariç modüller bütün dosya olarak ele alınıyor).
  const moduleRequires = new Map();
  const reachable = new Set();
  const queue = [main];
  while (queue.length) {
    const rel = queue.pop();
    if (reachable.has(rel)) continue;
    reachable.add(rel);
    if (!isJs(rel)) continue; // .json veri dosyası
    const text = read(rel).toString('utf8');
    const s = scan(parse(text, rel), rel, rel === main);
    const deps = [...s.requires].filter(isLocal).map((spec) => resolve(rel, spec));
    moduleRequires.set(rel, deps);
    queue.push(...deps);
  }

  // JS dışı dosyalar (prompt_tr.md gibi): adını anan modüle bağlanır; kimse anmıyorsa herkese.
  const nonJs = files.filter((f) => !isJs(f) && !reachable.has(f));
  const mentions = new Map(); // dosya → onu anan JS dosyaları
  const unattributed = [];
  for (const f of nonJs) {
    const b = path.posix.basename(f);
    const by = [...reachable].filter((m) => isJs(m) && read(m).toString('utf8').includes(b));
    if (by.length) mentions.set(f, by);
    else unattributed.push(f);
  }
  const dataFilesOf = (jsRel) => nonJs.filter((f) => (mentions.get(f) || []).includes(jsRel));

  const moduleClosureCache = new Map();
  const moduleClosure = (rel) => {
    if (moduleClosureCache.has(rel)) return moduleClosureCache.get(rel);
    const seen = new Set();
    const q = [rel];
    while (q.length) {
      const r = q.pop();
      if (seen.has(r)) continue;
      seen.add(r);
      for (const d of dataFilesOf(r)) seen.add(d);
      q.push(...(moduleRequires.get(r) || []));
    }
    moduleClosureCache.set(rel, seen);
    return seen;
  };

  // Küresel etkili satırlar (ulaşılabilir bütün JS dosyalarında).
  const globalLines = [];
  for (const rel of reachable) {
    if (!isJs(rel)) continue;
    for (const line of read(rel).toString('utf8').split(/\r?\n/)) {
      if (GLOBAL_EFFECT_LINE.test(line)) globalLines.push(`${rel}\n${line.trim()}`);
    }
  }
  globalLines.sort();

  // Ana dosyanın üst düzey birimleri.
  const mainText = read(main).toString('utf8');
  const ast = parse(mainText, main);
  const units = [];
  const declares = new Map(); // isim → birim indeksleri
  const exportUnits = new Map(); // export adı → birim indeksleri
  const infra = new Set(); // dış kütüphane nesneleri (functions, admin, db...)
  const declaredTop = new Set();
  for (const st of ast.body) {
    if (st.type === 'FunctionDeclaration' || st.type === 'ClassDeclaration') declaredTop.add(st.id.name);
    if (st.type === 'VariableDeclaration') for (const d of st.declarations) patternNames(d.id).forEach((n) => declaredTop.add(n));
  }

  const classifyCall = (c) => {
    if (c.type === 'NewExpression') {
      if (c.callee.type === 'Identifier' && PURE_CTORS.has(c.callee.name)) return 'pure';
      if (c.callee.type === 'Identifier' && declaredTop.has(c.callee.name)) return 'local';
      return 'global';
    }
    if (c.callee.type === 'Identifier' && c.callee.name === 'require') return 'pure';
    const { root, names } = memberPath(c.callee);
    if (root.type === 'CallExpression' && root.callee.type === 'Identifier' && root.callee.name === 'require') {
      // require('x')(...) ya da require('x').y(...)
      const spec = requireSpec(root);
      if (isLocal(spec)) return 'local';
      return names.some((n) => GLOBAL_METHODS.has(n)) ? 'global' : 'pure';
    }
    if (root.type !== 'Identifier') return 'global';
    if (infra.has(root.name)) return names.some((n) => GLOBAL_METHODS.has(n)) ? 'global' : 'pure';
    if (declaredTop.has(root.name)) return 'local';
    if (PURE_CALLS.has([root.name, ...names].join('.'))) return 'pure';
    if (root.name === 'Math' || root.name === 'JSON') return 'pure';
    return 'global';
  };
  const isInfraInit = (init) => {
    if (!init) return false;
    const { root, names } = memberPath(init);
    if (names.some((n) => GLOBAL_METHODS.has(n))) return false;
    if (root.type === 'CallExpression' && root.callee.type === 'Identifier' && root.callee.name === 'require') {
      return !isLocal(requireSpec(root));
    }
    return root.type === 'Identifier' && infra.has(root.name);
  };

  for (const st of ast.body) {
    const u = { text: mainText.slice(st.start, st.end), kind: 'decl', declares: [], fnValued: false, varDecl: false };
    const s = scan(st, main, true);
    u.names = s.names;
    u.exportRefs = s.exportRefs;
    u.requires = [...s.requires].filter(isLocal).map((spec) => resolve(main, spec));
    u.callish = s.loadCalls.length > 0;
    if (st.type === 'FunctionDeclaration' || st.type === 'ClassDeclaration') {
      u.declares = [st.id.name];
      u.fnValued = st.type === 'FunctionDeclaration';
    } else if (st.type === 'VariableDeclaration') {
      u.varDecl = true;
      for (const d of st.declarations) u.declares.push(...patternNames(d.id));
      u.fnValued = st.declarations.every((d) => d.init && (d.init.type === 'ArrowFunctionExpression' || d.init.type === 'FunctionExpression'));
      if (st.declarations.every((d) => isInfraInit(d.init))) {
        u.infra = true;
        u.declares.forEach((n) => infra.add(n));
      }
    } else if (st.type === 'ExpressionStatement' && st.expression.type === 'AssignmentExpression' &&
               st.expression.operator === '=' && st.expression.left.type === 'MemberExpression' &&
               st.expression.left.object.type === 'Identifier' && st.expression.left.object.name === 'exports' &&
               !st.expression.left.computed) {
      u.kind = 'export';
      u.exportName = st.expression.left.property.name;
      if (st.expression.right.type === 'AssignmentExpression') full(`${main}: zincirli export ataması`);
      // Sol taraftaki tek atama export'un kendisi; sağ taraftaki atamalara bakılıyor.
      const right = scan(st.expression.right, main, true);
      s.loadMutation = right.loadMutation;
    } else {
      u.kind = 'root';
    }
    // Yükleme anı davranışı: pure (yalnız değer üretir) / local (yerel kod çalıştırır) / global.
    let load = 'pure';
    if (s.loadUnknown) load = 'global';
    for (const c of s.loadCalls) {
      const k = classifyCall(c);
      if (k === 'global') load = 'global';
      else if (k === 'local' && load === 'pure') load = 'local';
    }
    if (s.loadMutation && load === 'pure') load = 'local';
    if (u.kind === 'root' || load === 'global') u.global = true;
    u.local = load === 'local';
    // Birimin metninde adı geçen JS dışı dosyalar.
    u.dataFiles = nonJs.filter((f) => (mentions.get(f) || []).includes(main) && u.text.includes(path.posix.basename(f)));
    const idx = units.length;
    units.push(u);
    for (const n of u.declares) {
      if (!declares.has(n)) declares.set(n, []);
      declares.get(n).push(idx);
    }
    if (u.kind === 'export') {
      if (!exportUnits.has(u.exportName)) exportUnits.set(u.exportName, []);
      exportUnits.get(u.exportName).push(idx);
    }
  }
  // Ana dosyada adı geçen ama hiçbir birime bağlanamayan JS dışı dosya → herkese.
  for (const f of nonJs) {
    const by = mentions.get(f) || [];
    if (by.length === 1 && by[0] === main && !units.some((u) => u.dataFiles.includes(f))) unattributed.push(f);
  }

  const unitDeps = (i) => {
    const u = units[i];
    const out = [];
    for (const n of u.names) for (const j of declares.get(n) || []) out.push(j);
    for (const n of u.exportRefs) for (const j of exportUnits.get(n) || []) out.push(j);
    return out;
  };
  const closeUnits = (start) => {
    const seen = new Set();
    const q = [...start];
    while (q.length) {
      const i = q.pop();
      if (seen.has(i)) continue;
      seen.add(i);
      q.push(...unitDeps(i));
    }
    return seen;
  };
  const filesOf = (unitSet) => {
    const fs_ = new Set();
    for (const i of unitSet) {
      for (const r of units[i].requires) for (const f of moduleClosure(r)) fs_.add(f);
      for (const f of units[i].dataFiles) fs_.add(f);
    }
    return fs_;
  };

  const roots = units.map((u, i) => (u.global ? i : -1)).filter((i) => i >= 0);
  // Yerel kod çalıştıran birimlerin değiştirebileceği hedefler: kapanışındaki değişken
  // bildirimleri (dış kütüphane nesneleri ve fonksiyon değerliler hariç) ve modül dosyaları.
  const locals = units.map((u, i) => (u.local && !u.global ? i : -1)).filter((i) => i >= 0).map((i) => {
    const cl = closeUnits([i]);
    return {
      i,
      units: new Set([...cl].filter((j) => units[j].varDecl && !units[j].infra && !units[j].fnValued)),
      files: filesOf(cl),
    };
  });

  const fileHash = (rel) => sha([rel, read(rel)]);
  const globalHash = sha([
    'deploy-plan v1',
    read('package.json'),
    fileSet.has('package-lock.json') ? read('package-lock.json') : '',
    JSON.stringify(cfg),
    ...globalLines,
    ...unattributed.sort().map(fileHash),
  ]);

  const fingerprints = {};
  const callish = {};
  for (const [name, idxs] of exportUnits) {
    let set = closeUnits([...idxs, ...roots]);
    for (;;) {
      const files = filesOf(set);
      const add = locals.filter((l) => !set.has(l.i) &&
        ([...l.units].some((j) => set.has(j)) || [...l.files].some((f) => files.has(f))));
      if (!add.length) break;
      set = closeUnits([...set, ...add.map((l) => l.i)]);
    }
    const texts = [...set].map((i) => units[i].text).sort();
    const files = [...filesOf(set)].sort().map(fileHash);
    fingerprints[name] = sha([globalHash, ...texts, '--', ...files]);
    callish[name] = idxs.some((i) => units[i].callish);
  }
  return { srcDir, fingerprints, callish };
}

function realFunctions(srcDir) {
  const tmp = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'deploy-plan-')), 'functions.json');
  const code = [
    'const m = require(process.argv[1]);',
    "const names = Object.keys(m).filter((k) => typeof m[k] === 'function' && m[k].__endpoint);",
    "require('fs').writeFileSync(process.argv[2], JSON.stringify(names));",
    'process.exit(0);',
  ].join('\n');
  try {
    execFileSync(process.execPath, ['-e', code, path.resolve(srcDir), tmp], {
      cwd: srcDir,
      env: { ...process.env, GCLOUD_PROJECT: process.env.GCLOUD_PROJECT || 'numigo-new' },
      stdio: ['ignore', 'ignore', 'inherit'],
      timeout: 120000,
    });
    return new Set(JSON.parse(fs.readFileSync(tmp, 'utf8')));
  } catch (e) {
    full(`index.js yüklenip fonksiyon listesi alınamadı: ${e.message}`);
  }
}

function plan(acorn, baseRoot, headRoot) {
  let base;
  try {
    base = analyze(baseRoot, acorn);
  } catch (e) {
    if (e instanceof FullDeploy) full(`base analiz edilemedi: ${e.message}`);
    throw e;
  }
  const head = analyze(headRoot, acorn);
  const real = realFunctions(head.srcDir);
  if (real.size === 0) full('index.js içinde hiç fonksiyon bulunamadı');
  for (const n of real) {
    if (!(n in head.fingerprints)) full(`'${n}' fonksiyonu statik analizde görünmüyor`);
  }
  const removed = Object.keys(base.fingerprints).filter((n) => !(n in head.fingerprints) && base.callish[n]);
  if (removed.length) full(`silinen/yeniden adlandırılan export var: ${removed.join(', ')}`);
  const changed = Object.keys(head.fingerprints).filter((n) => base.fingerprints[n] !== head.fingerprints[n]);
  for (const n of changed) {
    if (!real.has(n) && (head.callish[n] || base.callish[n])) full(`'${n}' değişmiş ama fonksiyon olup olmadığı anlaşılamadı`);
  }
  const fns = changed.filter((n) => real.has(n)).sort();
  if (fns.length === 0) return { mode: 'none', functions: [], reason: 'değişen fonksiyon yok' };
  if (fns.length === real.size) return { mode: 'full', functions: [], reason: 'bütün fonksiyonlar değişmiş' };
  return { mode: 'partial', functions: fns, reason: `${real.size} fonksiyondan ${fns.length} tanesi değişmiş` };
}

if (require.main === module) {
  const [acornPath, baseRoot, headRoot] = process.argv.slice(2);
  let result;
  try {
    result = plan(require(path.resolve(acornPath)), baseRoot, headRoot);
  } catch (e) {
    if (!(e instanceof FullDeploy)) throw e;
    result = { mode: 'full', functions: [], reason: e.message };
  }
  process.stdout.write(`${JSON.stringify(result)}\n`);
}

module.exports = { analyze, plan, FullDeploy };
