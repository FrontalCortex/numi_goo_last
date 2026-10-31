#!/usr/bin/env node
// Runner scaffold for the build-eval / hillclimb loop. Copy this into the
// user's repo and fill in loadCases / runCase / gradeCase below - the I/O
// shape, file naming, resume, and CLI surface are already hillclimb-ready
// so adding v2, v3, ... is `--variant v3`, not a refactor.
//
//   node run-eval.mjs --flow .claude/hillclimb/<name> --variant baseline --reps 2
//
// Structural properties this encodes (so you don't have to remember them):
//   - parameterized by --variant / --model / --reps (no hardcoded A/B pair)
//   - rep-aware filenames + resume (traces/<id>_rep<k>.json)
//   - reads _state.json, never writes it (loop state belongs to the orchestrator) - 
//     the ONE exception is --approve-harness recording `harness_sha` (see below)
//   - refuses to run when the harness (this file + _state.json.harness_paths) has
//     changed since the sha a human last approved with --approve-harness, so a
//     round that edits the runner cannot execute unreviewed under a standing
//     session allowlist
//   - pairwise graders judge against frozen baseline/ref/<id>.* on disk
//   - writes rows as cases complete (crash-safe)
//   - jittered exponential backoff on transient 429/overloaded/5xx errors
//   - hard per-case wall-clock ceiling (--timeout-s; stream keepalives don't reset it)
//   - served-model assertion (response model must match --model; documented alias->snapshot
//     shapes tolerated: 'foo-latest'/'foo-0'/'foo' -> 'foo-20250101' / 'foo@20250101' / 'foo-2025-01-01')
//   - failed attempts land in errors.jsonl with a failure class and, when the call
//     completed, the billed model/usage (never in results.jsonl)
//   - row ids, trace filenames, and frozen refs share one path-safe id
//     (original id kept in meta.original_id when sanitization changed it)

import { createHash } from 'node:crypto';
import { closeSync, constants as FS, existsSync, fstatSync, ftruncateSync, lstatSync, mkdirSync, openSync, readFileSync, realpathSync, writeFileSync, writeSync } from 'node:fs';
import { dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Every output write refuses symlinks: the flow dir is model-influenced, and a
// prompt-injected round can plant `results.jsonl -> ~/.bashrc` where the next
// unattended run would append. POSIX opens O_NOFOLLOW (a symlink fails with
// ELOOP); Windows - where Node leaves O_NOFOLLOW undefined and Bun defines a
// meaningless value - lstat-refuses first. Symlinked parent dirs are
// refused the same way. Same discipline as the report builders' reads.
const WIN = process.platform === 'win32';
const NOFOLLOW = WIN ? 0 : FS.O_NOFOLLOW;
// A guard that cannot tell must refuse: only "no such entry" reads as absent;
// any other lstat failure (EACCES, ENAMETOOLONG, ...) is rethrown, never "no".
const lstatOrNull = p => { try { return lstatSync(p); } catch (e) { if (e?.code === 'ENOENT') return null; throw e; } };
const isSymlink = p => lstatOrNull(p)?.isSymbolicLink() === true;
// Stderr lines interpolate model-influenced bytes (case ids, error text that
// can echo model output, JSON.parse messages). Strip escape sequences and
// control characters, as build-report-lite.mjs's eprint does, so a planted
// OSC/CSI can't retitle the terminal or forge output lines.
const ESC_SEQ = /\x1b\[[0-?]*[ -\/]*[@-~]|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)?|\x1b[@-_]/g;
const CONTROL = /[\x00-\x1f\x7f-\x9f]/g;
const termSafe = s => String(s).replace(ESC_SEQ, '').replace(CONTROL, '');
const eprint = (...a) => console.error(...a.map(termSafe));
// The leaf checks above can't see a symlink on an INTERMEDIATE component
// (lstat and open both resolve those silently), so every open is also bound
// to the flow root: main() captures realpathSync(flow) once, and any path
// whose resolved parent leaves it - e.g. `vdir` or the flow dir itself
// replaced by a directory symlink - is refused when the check sees it.
// Residual, all platforms: the check and the open are separate path lookups
// (Node's sync fs has no openat-style call), so a directory swapped for a
// symlink in between is still followed. This stops a planted link, not a
// writer racing the run.
let flowRealRoot = null;
function assertInFlow(dir, what) {
  if (flowRealRoot == null) throw new Error(`refusing to ${what}: flow root not resolved yet`);
  const dirReal = realpathSync(dir);
  if (dirReal !== flowRealRoot && !dirReal.startsWith(flowRealRoot + (WIN ? '\\' : '/')))
    throw new Error(`refusing to ${what}: ${dir} resolves outside the flow directory`);
}
function openNoFollow(p, flags) {
  if (isSymlink(dirname(p))) throw new Error(`refusing to open through symlinked directory: ${dirname(p)}`);
  assertInFlow(dirname(p), 'open');
  if (WIN && isSymlink(p)) throw new Error(`refusing to open through symlink: ${p}`);
  const fd = openSync(p, flags | NOFOLLOW, 0o644);
  try {
    const st = fstatSync(fd);
    if (!st.isFile()) throw new Error(`refusing to use non-regular file: ${p}`);
    // O_NOFOLLOW and lstat cannot see a hard link: a second name for a file
    // outside the flow dir opens as an ordinary regular file. Nothing the
    // runner creates has more than one link, so refuse any that does.
    if (st.nlink > 1) throw new Error(`refusing to use ${p}: it has a second hard link (another name for the same file); replace it with a plain copy if it is yours`);
  } catch (e) { closeSync(fd); throw e; }
  return fd;
}
// writeFileSync on the fd loops until every byte lands (a bare writeSync is
// one write(2) that may return short on ENOSPC and silently truncate a
// results row or trace).
// Opened without O_TRUNC and truncated only after openNoFollow's checks, so a
// refused file keeps its bytes.
function writeFileNoFollow(p, data) {
  const fd = openNoFollow(p, FS.O_WRONLY | FS.O_CREAT);
  try { ftruncateSync(fd, 0); writeFileSync(fd, data); } finally { closeSync(fd); }
}
// POSIX appends atomically under O_APPEND with no position. On Windows, Bun
// writes an O_APPEND handle at offset 0 unless given a position, so there the
// write starts at the current size and re-issues any short write.
function appendFileNoFollow(p, data) {
  const fd = openNoFollow(p, FS.O_WRONLY | FS.O_CREAT | FS.O_APPEND);
  try {
    if (!WIN) { writeFileSync(fd, data); return; }
    const buf = Buffer.from(data);
    const start = fstatSync(fd).size;
    for (let off = 0; off < buf.length;) {
      const n = writeSync(fd, buf, off, buf.length - off, start + off);
      if (n <= 0) throw new Error(`append to ${p} made no progress`);
      off += n;
    }
  } finally { closeSync(fd); }
}
// Reads of the frozen pairwise refs get the same discipline as writes (same
// open guard): the flow dir is model-influenced, so `baseline/ref/<id> ->
// ~/.ssh/id_rsa` planted after the startup preflight must not be read into
// the judge prompt. lexists probes with lstat so a planted symlink still
// counts as "present" at the freeze guard (never overwritten - or followed).
const lexists = p => lstatOrNull(p) != null;
function readFileNoFollow(p) {
  const fd = openNoFollow(p, FS.O_RDONLY);
  try { return readFileSync(fd, 'utf8'); } finally { closeSync(fd); }
}
// null when the file is absent; any other failure (a planted link included) throws.
function readIfPresent(p) {
  try { return readFileNoFollow(p); } catch (e) { if (e?.code === 'ENOENT') return null; throw e; }
}
function mkdirNoFollow(dir) {
  if (isSymlink(dir)) throw new Error(`refusing to use symlinked directory: ${dir}`);
  mkdirSync(dir, { recursive: true });
  // Check after creating: mkdirSync(recursive) follows symlinked ancestors,
  // so a dir minted through one resolves outside the flow root and is refused
  // here before any file lands in it.
  assertInFlow(dir, 'create directory');
}
// Frozen pairwise refs may carry an extension; reader and freeze-guard probe
// the same list so a suffixed ref never gets an extensionless shadow.
const REF_EXTS = ['', '.html', '.txt', '.json'];

// --- Sobi'ye özel kısım -------------------------------------------------------
//
// SOBİ DEĞERLENDİRMESİ. Şablon: claude-api skill'inin runner-scaffold.mjs'i (altındaki "harness"
// kısmı olduğu gibi: kaldığı yerden devam, geri çekilmeli yeniden deneme, örnek başına süre sınırı,
// sunan modelin denetimi, errors.jsonl). Bu kısım Sobi'yi GERÇEK sunucu koduyla çağırıyor:
// functions/sobi/chat.js → runTurn (talimat, araçlar, guard denetimi, yedek metin dahil). Sobi'nin
// API çağrısını yeniden yazmıyoruz; yalnızca istemciyi saran ince bir kaydedici var (her isteğin
// modeli ve jetonları için; runTurn yalnızca toplamı dönüyor).
//
// DİKKAT: her çalıştırma ücretli (Sobi: Haiku 5.5, hakem: varsayılan Sonnet 5.5).
//
// Kullanım (depo kökünden):
//   node tools/sobi-eval/run-eval.mjs --flow tools/sobi-eval/sonuclar --variant baseline --sec ornek:20
//   node tools/sobi-eval/run-eval.mjs --flow tools/sobi-eval/sonuclar --variant baseline       (hepsi)
//   --hakem-model claude-opus-5-5   --hakem-effort low|medium|high
//   --sahte tools/sobi-eval/sahte.json   Sobi çağrılmaz; dosyadaki sabit cevaplar puanlanır
//                                        (hakemi bilinen iyi/kötü cevaplarla sınamak için)
//   --hakemsiz                           hakem çağrılmaz (yalnızca programlı denetimler)
//   --tum-adimlar                        bağlamda son 6 adım yerine dersin bütün önceki adımları
//   --sec dosya:<yol>                    yalnızca dosyadaki id'ler (satır başına bir id)
//   --kayittan tools/sobi-eval/sonuclar/baseline
//                                        Sobi yeniden çağrılmaz; o çalıştırmanın traces/ kayıtlarındaki
//                                        cevaplar yeniden puanlanır (rubrik değişince hakemi aynı
//                                        cevaplarla karşılaştırmak için). Sobi jetonu 0 yazılır:
//                                        yeni harcama yok, maliyeti kaynak çalıştırmada.

import { createRequire } from 'node:module';

const HERE = dirname(fileURLToPath(import.meta.url));
const FN = join(HERE, '..', '..', 'functions');
const fnRequire = createRequire(join(FN, 'package.json'));
// Anahtar functions/.env.local'da (Git'e girmiyor); var olan ortam değişkenini ezmez.
fnRequire('dotenv').config({ path: join(FN, '.env.local'), quiet: true });
const Anthropic = fnRequire('@anthropic-ai/sdk').default;
// SOBI_KOD=<klasör>: Sobi kodunu (chat/guard/tools/stepExplainer + prompt_tr.md) başka bir klasörden
// yükle; eski bir sürümü (ör. `git show HEAD:functions/sobi/...` ile çıkarılmış) yerel değişikliklere
// dokunmadan ölçmek için. O klasör harness parmak izine girmiyor.
const SOBI_DIR = process.env.SOBI_KOD ? resolve(process.env.SOBI_KOD) : join(FN, 'sobi');
const sobiRequire = createRequire(join(SOBI_DIR, 'index.js'));
const chat = sobiRequire('./chat');
const guard = sobiRequire('./guard');
const tools = sobiRequire('./tools');
const explainer = sobiRequire('./stepExplainer');

const SOBI_SYSTEM = readFileSync(join(SOBI_DIR, 'prompt_tr.md'), 'utf8');
const HAKEM_RUBRIK = readFileSync(join(HERE, 'hakem_tr.md'), 'utf8');
// Hakem Sobi'nin o anki talimatını değil, hedeflenen talimatın sabit kopyasını okur: talimat
// değişince baseline ile yeni sürüm aynı ölçütle puanlansın.
const HAKEM_SYSTEM = `${HAKEM_RUBRIK}\n\n<sobi_talimati>\n${readFileSync(join(HERE, 'sobi_talimati_hedef.md'), 'utf8')}\n</sobi_talimati>`;
const DEFAULT_HAKEM = 'claude-sonnet-5-5';

let apiClient = null;
const api = () => (apiClient ??= new Anthropic());

/** messages.create'i saran kaydedici: her isteğin modelini, jetonlarını ve durma sebebini tutar. */
function recordingClient(calls) {
  const wrap = (fn) => async (params) => {
    const r = await fn(params);
    calls.push({ model: r.model, usage: r.usage, stop_reason: r.stop_reason });
    return r;
  };
  return {
    messages: { create: wrap((p) => api().messages.create(p)) },
    beta: { messages: { create: wrap((p) => api().beta.messages.create(p)) } },
  };
}

const USAGE_KEYS = ['input_tokens', 'output_tokens', 'cache_read_input_tokens', 'cache_creation_input_tokens'];
function sumUsage(list) {
  const out = Object.fromEntries(USAGE_KEYS.map((k) => [k, 0]));
  for (const u of list) for (const k of USAGE_KEYS) out[k] += Number(u?.[k]) || 0;
  return out;
}

/** Bu turda eklenen mesajlardan araç çağrıları ve sonuçları, sırayla. */
function toolCallsOf(newMsgs) {
  const results = new Map();
  for (const m of newMsgs) {
    if (m.role !== 'user' || !Array.isArray(m.content)) continue;
    for (const b of m.content) if (b.type === 'tool_result') results.set(b.tool_use_id, { content: typeof b.content === 'string' ? b.content : JSON.stringify(b.content), isError: !!b.is_error });
  }
  const out = [];
  for (const m of newMsgs) {
    if (m.role !== 'assistant') continue;
    for (const b of m.content) if (b.type === 'tool_use') out.push({ name: b.name, input: b.input, result: results.get(b.id) || null });
  }
  return out;
}

async function loadCases() {
  let cases = JSON.parse(readFileSync(join(HERE, 'cases.json'), 'utf8'));
  const sec = argExtra.sec;
  if (sec) {
    const m = sec.match(/^ornek:(\d+)$/);
    const f = sec.match(/^dosya:(.+)$/);
    if (f) {
      // Satır başına bir id (aynı örneklerin baseline ve v1'de ölçülmesi için).
      const ids = new Set(readFileSync(f[1], 'utf8').split(/\r?\n/).map((x) => x.trim()).filter(Boolean));
      cases = cases.filter((c) => ids.has(c.id));
      if (cases.length !== ids.size) throw new Error(`seçim dosyasındaki ${ids.size} id'nin ${cases.length}'i bulundu`);
    } else if (m) {
      // Eşit aralıklı örneklem: set üretim sırasıyla öğreticilere ve kategorilere yayılı.
      const n = Math.min(Number(m[1]), cases.length);
      const step = cases.length / n;
      cases = Array.from({ length: n }, (_, i) => cases[Math.floor(i * step + step / 2)]);
    } else {
      const ids = new Set(sec.split(','));
      cases = cases.filter((c) => ids.has(c.id));
    }
  }
  return cases.map((c) => ({
    ...c,
    prompt: c.turns.map((t) => t.text).join('  ⏵  '),
    meta: { ...(c.meta || {}), beklenen: c.expected },
  }));
}

/**
 * Kayıttan sohbet: trace dosyasındaki sırayla user (tur başı) → tool_call/tool_result → assistant.
 * Bağlam cases.json'dan (trace'teki metin onun çıktısı). runCase'in döndüğü biçimle aynı.
 */
function runFromTrace(c, dir) {
  const trace = JSON.parse(readFileSync(join(dir, 'traces', `${c.id}_rep0.json`), 'utf8'));
  const src = readFileSync(join(dir, 'results.jsonl'), 'utf8').split('\n').filter(Boolean).map((l) => JSON.parse(l)).find((r) => r.prompt_id === c.id);
  if (!src) throw new Error(`kaynak satır yok: ${c.id}`);
  const turns = [];
  let cur = null;
  for (let i = 0; i < trace.length; i++) {
    const e = trace[i];
    if (e.role === 'user') {
      const t = c.turns[turns.length];
      cur = { child: t.text, ctxIndex: t.ctx, context: chat.sanitizeContext(c.contexts[t.ctx]), toolCalls: [], actions: [], stopReason: 'end_turn', masked: false };
      turns.push(cur);
    } else if (e.role === 'tool_call') {
      const res = trace[i + 1]?.role === 'tool_result' ? trace[++i].content : null;
      cur.toolCalls.push({ name: e.name, input: JSON.parse(e.content), result: res === null ? null : { content: res, isError: false } });
    } else if (e.role === 'assistant') {
      const m = e.content.match(/^\[MODELİN CEVABI — denetime takıldı: ([^\]]*)\]\n([\s\S]*?)\n\n\[ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN\]\n([\s\S]*)$/);
      Object.assign(cur, m
        ? { fellBack: true, reasons: m[1] ? m[1].split(', ') : [], modelReply: m[2] === '(cevap yok)' ? null : m[2], reply: m[3] }
        : { fellBack: false, reasons: [], modelReply: e.content, reply: e.content });
    }
  }
  if (turns.length !== c.turns.length) throw new Error(`${c.id}: kayıtta ${turns.length} tur var, beklenen ${c.turns.length}`);
  return {
    output: turns.map((t) => t.reply).join('\n---\n'), transcript: trace, turns, model: src.model,
    usage: sumUsage([]), sobi_calls: 0, stop_reason: 'end_turn', status: src.status,
  };
}

async function runCase(c, ctx) {
  if (argExtra.kayittan) return runFromTrace(c, argExtra.kayittan);
  const calls = [];
  const client = recordingClient(calls);
  const sahte = argExtra.sahte ? JSON.parse(readFileSync(argExtra.sahte, 'utf8'))[c.id] : null;
  if (argExtra.sahte && !sahte) throw new Error(`sahte dosyasında ${c.id} yok`);
  const transcript = [{ role: 'system', content: SOBI_SYSTEM }];
  const turns = [];
  let history = [];
  let note = null;
  for (let i = 0; i < c.turns.length; i++) {
    const t = c.turns[i];
    const raw = c.contexts[t.ctx];
    // --tum-adimlar: uygulama son 6 adım yerine bütün önceki adımları gönderiyor (öneri 18).
    const context = chat.sanitizeContext(argExtra.tumAdimlar ? { ...raw, recentSteps: raw.allSteps } : raw);
    let r;
    if (sahte) {
      const text = sahte[i] ?? '';
      const check = guard.checkReply(text, guard.sourceNumbers([{ role: 'user', content: [{ type: 'text', text: chat.contextBlock(context) + t.text }] }]), { explanation: context.target === null });
      r = { reply: check.ok ? text : guard.fallbackReply(context, tools), modelReply: text, fellBack: !check.ok, reasons: check.reasons, actions: [], suggestions: [], history: [], usage: {}, stopReason: 'end_turn', masked: false };
    } else {
      r = await chat.runTurn({ client, system: SOBI_SYSTEM, history, context, childText: t.text, note, model: ctx.model, effort: argExtra.effort });
    }
    const newMsgs = r.history.slice(history.length);
    const toolCalls = toolCallsOf(newMsgs);
    transcript.push({ role: 'user', content: `${chat.contextBlock(context)}\n<cocuk>${guard.cleanChildText(t.text).text}</cocuk>${note ? `\n<not>${note}</not>` : ''}` });
    for (const tc of toolCalls) {
      transcript.push({ role: 'tool_call', name: tc.name, content: JSON.stringify(tc.input, null, 2) });
      transcript.push({ role: 'tool_result', content: tc.result ? tc.result.content : '(sonuç yok)' });
    }
    transcript.push({
      role: 'assistant',
      content: r.fellBack
        ? `[MODELİN CEVABI — denetime takıldı: ${r.reasons.join(', ')}]\n${r.modelReply ?? '(cevap yok)'}\n\n[ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN]\n${r.reply}`
        : r.reply,
    });
    turns.push({
      child: t.text, ctxIndex: t.ctx, context, reply: r.reply, modelReply: r.modelReply, fellBack: r.fellBack,
      reasons: r.reasons, actions: r.actions, stopReason: r.stopReason, masked: r.masked, toolCalls,
    });
    history = r.history;
    note = r.fellBack ? r.reply : null;
  }
  const models = [...new Set(calls.map((x) => x.model))];
  if (models.length > 1) {
    const e = new Error(`bir örnekte birden çok model sundu: ${models.join(', ')}`);
    e.failure_class = 'serving_substitution';
    throw e;
  }
  const stops = turns.map((t) => t.stopReason);
  return {
    output: turns.map((t) => t.reply).join('\n---\n'),
    transcript,
    turns,
    model: sahte ? ctx.model : models[0],
    usage: sumUsage(calls.map((x) => x.usage)),
    sobi_calls: calls.length,
    stop_reason: stops.at(-1),
    status: stops.includes('max_tokens') ? 'truncated' : 'ok',
  };
}

// ── Programlı denetimler ───────────────────────────────────────────────────

const HESAP_ARACLARI = new Set(['hamleleri_hesapla', 'hatayi_bul', 'abakuste_goster']);
// Belirli bir sayıyla hamle: "3 gelir", "2 boncuk gider", "kardeşi 4", "büyük kardeşi 6 gider".
// 5 ve 10 kural adlarında geçtiği için ("5 gelir, kardeş gider") hamle sayılmıyor.
const HAMLE = /(?<!\d)(\d+)\s*(?:tane\s+)?(?:birlik\s+)?(?:boncuk\s+)?(?:gelir|gider|gelecek|gidecek|gelsin|gitsin)|kardeş(?:i|in)?\s+(\d+)/giu;
// Hareket yönü tarifi ("aşağı indir", "yukarı kaydır"); "yukarıdaki/aşağıdaki" (yer) sayılmaz.
const YON = /(?<!\p{L})(?:aşağı(?!dak)\p{L}*|yukarı(?!dak)\p{L}*|indir\p{L}*|kaydır\p{L}*|sürükle\p{L}*|çek(?:in|ip|er|elim|iyoruz)?(?!\p{L}))/iu;
const BICIM = /\*\*|^\s*#|^\s*[-•]\s|^\s*\d+[.)]\s|\p{Extended_Pictographic}/mu;
const trLower = (s) => s.toLocaleLowerCase('tr-TR');

function sentences(t) {
  // "3. sütun" gibi sıra sayıları cümle sonu sayılmasın.
  return t.replace(/(\d)\./g, '$1').split(/[.!?…]+(?:\s|$)/).map((x) => x.trim()).filter(Boolean).length;
}

function programmatic(c, run) {
  const out = {};
  const replies = run.turns.map((t) => t.modelReply).filter((r) => typeof r === 'string' && r.trim());
  out.yedek_yok = run.turns.every((t) => !t.fellBack) ? 1 : 0;
  out.cevap_var = replies.length === run.turns.length ? 1 : 0;
  out.kisa = replies.every((r) => sentences(r) <= 3 && r.length <= guard.MAX_REPLY) ? 1 : 0;
  out.yon_yok = replies.some((r) => YON.test(r)) ? 0 : 1;
  out.bicim = replies.some((r) => BICIM.test(r)) ? 0 : 1;
  out.kisisel_tekrar_yok = (c.gizli || []).some((g) => replies.some((r) => trLower(r).includes(trLower(g)))) ? 0 : 1;
  // Hamle söyleyen bir cevaptan önce (bu turda ya da daha önce) hesap aracı çağrılmış mı. Dersin
  // kendi metnindeki hamleyi tekrarlamak serbest.
  let toolSeen = false;
  let aracOnce = 1;
  for (const t of run.turns) {
    if (t.toolCalls.some((x) => HESAP_ARACLARI.has(x.name))) toolSeen = true;
    if (!t.modelReply || toolSeen) continue;
    const ctxText = [t.context.stepText, t.context.questionText, ...(t.context.recentSteps || [])].join(' ');
    const ctxNums = guard.numbersIn(ctxText);
    for (const m of t.modelReply.matchAll(HAMLE)) {
      const n = Number(m[1] ?? m[2]);
      if (n !== 5 && n !== 10 && !ctxNums.has(n)) { aracOnce = 0; break; }
    }
  }
  out.arac_once = aracOnce;
  const bad = unsourcedMoves(c, run);
  out.hamle_kaynakli = bad.length ? 0 : 1;
  out._hamle_kaynaksiz = bad;
  // Bilgi amaçlı (başarıya girmez): soru adımında ilk turda hatayi_bul; sıkı kaynak denetimi
  // (anlatım adımındaki "10'a kadar serbest" gevşetmesi olmadan kaynaksız sayı var mı).
  const first = run.turns[0];
  out.hatayi_bul_ilk = first.context.target === null ? null : (first.toolCalls.some((x) => x.name === 'hatayi_bul') ? 1 : 0);
  return out;
}

// ── Programlı hamle denetimi ────────────────────────────────────────────────
// Hakem (Sonnet, low) boncuk aritmetiğinde güvenilmezdi; matematiğin ölçülebilen kısmı burada:
// cevaptaki her "N gelir / N gider / kardeşi N gider / beşlik boncuk gelir" ifadesi bir kaynakta
// aynı yönle geçmeli. Kaynak değişkenden bağımsız (baseline ile v1 aynı ölçülsün diye): kural
// motorunun bu adım ve ekrandaki işlem için hamleleri, sohbetteki araç sonuçları, dersin bütün
// metni ve kuralların onaylı açıklamaları. Yanlış bir sayıyı yakalar; "7'nin büyük kardeşi" gibi
// sayısı yanlış yere bağlanmış cümleleri yakalamaz (onu hakem görüyor).
const HAMLE_FIIL = '(gelir|gider|geliyor|gidiyor|gelsin|gitsin|gelecek|gidecek|gelmeli|gitmeli)(?!\\p{L})';
const HAMLE_IFADE = new RegExp(
  `(\\d+)(?:['’]\\p{L}+)?\\s*(?:tane\\s+)?(?:birlik\\s+)?(?:boncuk\\s+)?${HAMLE_FIIL}|beşlik\\s+boncuk(?:\\s+de)?\\s+${HAMLE_FIIL}`,
  'giu',
);
function moveTuples(text) {
  const out = new Set();
  for (const m of String(text || '').matchAll(HAMLE_IFADE)) {
    const n = m[1] ? Number(m[1]) : 5;
    const verb = (m[2] || m[3]).toLocaleLowerCase('tr-TR');
    out.add(`${n}${verb.startsWith('gel') ? '+' : '-'}`);
  }
  return out;
}
// Kaynak dar tutuluyor: dersin bütün metni kaynak olunca neredeyse her "N gelir" bir yerde geçiyordu
// ve denetim hiçbir şey yakalamıyordu. Dersten yalnızca adımın metni ve son 6 adım (iki değişkende
// aynı); 5 ve 10 kural adlarında geçtiği için hep serbest; "sol sütuna 1 gelir / onlardan 1 boncuk
// gider" de "10 gelir / 10 gider"in sütun dilindeki karşılığı (baseline'da işaretlenen 5 cevabın
// 4'ü buydu ve doğruydu).
function referenceMoves(c, run) {
  const parts = ['5 gelir 5 gider 10 gelir 10 gider 1 gelir 1 gider'];
  const engine = (e) => { if (e && e.moves.length) parts.push(JSON.stringify(tools.describe(e))); };
  for (const raw of c.contexts) {
    parts.push(raw.stepText, raw.questionText, ...(raw.recentSteps || []));
    if (Number.isInteger(raw.base) && Number.isInteger(raw.target)) engine(explainer.explainTransition(raw.base, raw.target));
    const q = (raw.questionText || '').match(/^\s*(\d+)\s*([+\-−])\s*(\d+)\s*$/);
    if (q) engine(explainer.explain(Number(q[1]), q[2] === '+' ? 'add' : 'sub', Number(q[3])));
  }
  for (const t of run.turns) for (const tc of t.toolCalls) if (tc.result) parts.push(tc.result.content);
  return moveTuples(parts.join(' \n '));
}
function unsourcedMoves(c, run) {
  const ref = referenceMoves(c, run);
  const bad = new Set();
  for (const t of run.turns) for (const k of moveTuples(t.modelReply)) if (!ref.has(k)) bad.add(k);
  return [...bad];
}

/** Sıkı kaynak denetimi: runTurn'ün kullandığı kaynakla, gevşetme olmadan. */
function strictUnknown(run) {
  const msgs = [];
  const unknown = new Set();
  for (const t of run.turns) {
    msgs.push({ role: 'user', content: [{ type: 'text', text: `${chat.contextBlock(t.context)}${t.child}` }] });
    for (const tc of t.toolCalls) if (tc.result) msgs.push({ role: 'user', content: [{ type: 'tool_result', content: tc.result.content }] });
    if (!t.modelReply) continue;
    const src = guard.sourceNumbers(msgs);
    for (const n of guard.numbersIn(t.modelReply)) if (n !== 5 && n !== 10 && !src.has(n)) unknown.add(n);
  }
  return [...unknown];
}

// ── Hakem ──────────────────────────────────────────────────────────────────

const OLCUTLER = ['soruya_cevap', 'matematik', 'ders_tutarliligi', 'ogretilmemis_yok', 'uslup', 'yardim_sirasi', 'guvenlik', 'sohbet_akisi'];
const HAKEM_SCHEMA = {
  type: 'object',
  properties: {
    ...Object.fromEntries(OLCUTLER.map((k) => [k, {
      type: 'object',
      properties: { puan: { type: 'string', enum: ['1', '0', 'yok'] }, gerekce: { type: 'string' } },
      required: ['puan', 'gerekce'],
      additionalProperties: false,
    }])),
    hata_turu: { type: 'string' },
  },
  required: [...OLCUTLER, 'hata_turu'],
  additionalProperties: false,
};

/** Kural motorunun referans hamleleri (hakem matematiği bununla karşılaştırır). */
function referenceFor(context) {
  const lines = [];
  const add = (e, label) => {
    if (!e || !e.moves.length) return;
    const d = tools.describe(e);
    lines.push(`${label}: ${d.islem} = ${d.sonuc}. ${d.hamleler.map((h) => `${h.basamak} basamağında ${h.kural}: ${h.adimlar.join(', ')}`).join('; ')}`);
  };
  if (Number.isInteger(context.base) && Number.isInteger(context.target)) add(explainer.explainTransition(context.base, context.target), 'Bu adımın doğru hamleleri');
  const q = (context.questionText || '').match(/^\s*(\d+)\s*([+\-−])\s*(\d+)\s*$/);
  if (q) add(explainer.explain(Number(q[1]), q[2] === '+' ? 'add' : 'sub', Number(q[3])), 'Ekrandaki işlemin tamamı');
  return lines;
}

const RULE_TR = chat.RULE_LABELS;
function judgeInput(c, run) {
  const first = run.turns[0].context;
  const out = [
    '<ornek>',
    `Kategori: ${c.tags[0]}`,
    `Beklenen davranış: ${c.expected}`,
    `Adım türü: ${first.target === null ? 'anlatım adımı (çocuk soru cevaplamıyor)' : 'abaküslü soru adımı'}`,
    `Çocuğun öğrendiği kurallar: ${first.taughtRules.length ? first.taughtRules.map((k) => RULE_TR[k]).join(', ') : '(henüz hiçbiri; sayıları abaküse yazmayı öğreniyor)'}`,
    `Çok turlu: ${run.turns.length > 1 ? 'evet' : 'hayır'}`,
  ];
  let lastCtx = null;
  run.turns.forEach((t, i) => {
    out.push(`\n[Tur ${i + 1}]`);
    if (t.ctxIndex !== lastCtx) {
      if (lastCtx !== null) out.push('(Çocuk bu turdan önce dersin sonraki adımına geçti.)');
      out.push(chat.contextBlock(t.context));
      for (const l of referenceFor(t.context)) out.push(`Referans (kural motoru) — ${l}`);
      lastCtx = t.ctxIndex;
    }
    out.push(`Çocuk: ${t.child}`);
    for (const tc of t.toolCalls) out.push(`Araç: ${tc.name}(${JSON.stringify(tc.input)}) → ${(tc.result?.content || '').slice(0, 2500)}`);
    out.push(`Sobi: ${t.modelReply ?? '(cevap yok)'}`);
    if (t.fellBack) out.push(`(Bu cevap otomatik denetime takıldı [${t.reasons.join(', ')}]; çocuk yerine şu yedek metni gördü: "${t.reply}". Sen Sobi'nin kendi cevabını puanla.)`);
  });
  out.push('</ornek>');
  return out.join('\n');
}

/**
 * İlk hakem isteği bitmeden diğerleri başlamıyor: hakem talimatını (~8 bin jeton) önbelleğe tek bir
 * istek yazsın. Pilotta aynı anda başlayan 4 istek önbelleği ayrı ayrı yazmıştı; her yazma ~0,02 $,
 * hakem maliyetinin üçte biri.
 */
let hakemIlk = null;
async function judge(c, run, ctx) {
  if (hakemIlk) await hakemIlk.catch(() => {});
  const p = judgeCall(c, run);
  if (!hakemIlk) hakemIlk = p;
  return p;
}

async function judgeCall(c, run) {
  const model = argExtra.hakemModel || DEFAULT_HAKEM;
  const res = await api().messages.create({
    model,
    max_tokens: 8000,
    system: [{ type: 'text', text: HAKEM_SYSTEM, cache_control: { type: 'ephemeral' } }],
    messages: [{ role: 'user', content: judgeInput(c, run) }],
    output_config: { effort: argExtra.hakemEffort || 'low', format: { type: 'json_schema', schema: HAKEM_SCHEMA } },
  });
  const fail = (msg, cls) => {
    const e = new Error(msg);
    e.failure_class = cls;
    e.judge_model = res.model;
    e.judge_usage = res.usage;
    return e;
  };
  if (res.model !== model) throw fail(`hakem modeli ${res.model} != ${model}`, 'serving_substitution');
  if (res.stop_reason !== 'end_turn') throw fail(`hakem durdu: ${res.stop_reason}`, res.stop_reason === 'refusal' ? 'judge_refusal' : 'judge_error');
  let parsed;
  try { parsed = JSON.parse(res.content.filter((b) => b.type === 'text').map((b) => b.text).join('')); } catch { throw fail('hakem JSON değil', 'judge_error'); }
  return { parsed, judge_model: res.model, judge_usage: res.usage };
}

async function gradeCase(c, run, ref, ctx) {
  const grade = programmatic(c, run);
  const explanation = {};
  if (grade._hamle_kaynaksiz.length) explanation.hamle_kaynakli = `kaynakta olmayan hamle: ${grade._hamle_kaynaksiz.map((k) => k.replace('+', ' gelir').replace('-', ' gider')).join(', ')}`;
  delete grade._hamle_kaynaksiz;
  const unknown = strictUnknown(run);
  grade.kaynaksiz_siki_yok = unknown.length ? 0 : 1;
  if (unknown.length) explanation.kaynaksiz_siki_yok = `kaynakta olmayan sayılar: ${unknown.join(', ')}`;
  const reasons = run.turns.flatMap((t) => t.reasons);
  if (reasons.length) explanation.yedek_yok = reasons.join(', ');
  let judged = null;
  if (grade.cevap_var && !argExtra.hakemsiz) judged = await judge(c, run, ctx);
  for (const k of OLCUTLER) {
    const v = judged?.parsed?.[k];
    grade[k] = argExtra.hakemsiz ? null : !judged ? 0 : v?.puan === '1' ? 1 : v?.puan === '0' ? 0 : null;
    if (v?.gerekce) explanation[k] = v.gerekce;
  }
  if (judged?.parsed?.hata_turu) explanation.hata_turu = judged.parsed.hata_turu;
  // Başarı: çocuk modelin kendi cevabını gördü (yedeğe düşmedi), sert programlı denetimler geçti
  // ve hakemin uygulanabilir ölçütleri 1. Kullanıcı kararı (29.10.2026): uzunluk ve "yukarı
  // kalkmış" gibi yön sözleri tek başına kaldırmaz (kisa, yon_yok yalnızca bilgi; hakemin üslup
  // ölçütü aşırı uzun/karışık cevabı ve hareket talimatını yakalıyor). Hakemin matematik puanı
  // güvenilmez olduğu için başarıya girmiyor. Programlı hamle denetimi (hamle_kaynakli) de yalnızca
  // bilgi: v1'de işaretlediği 4 kötüleşmenin 4'ünde Sobi doğruydu ("20 için 2 birlik boncuk gelir").
  const hard = ['yedek_yok', 'cevap_var', 'bicim', 'kisisel_tekrar_yok', 'arac_once'];
  const judgeOk = OLCUTLER.filter((k) => k !== 'matematik').every((k) => grade[k] === null || grade[k] === 1);
  grade.basari = hard.every((k) => grade[k] === 1) && judgeOk ? 1 : 0;
  // Hakem geçti ama denetim yedeğe düşürdü: denetimin yanlış alarmı olabilir.
  grade.yanlis_alarm = grade.yedek_yok === 0 && judged && judgeOk ? 1 : 0;
  const ordered = { basari: grade.basari, ...grade };
  return { grade: ordered, explanation, judge_model: judged?.judge_model, judge_usage: judged?.judge_usage };
}

function perfFrom(run) {
  return {
    tool_calls: run.turns.reduce((s, t) => s + t.toolCalls.length, 0),
    sobi_calls: run.sobi_calls,
    turns: run.turns.length,
    reply_chars: Math.max(...run.turns.map((t) => (t.modelReply || '').length)),
    tools_used: [...new Set(run.turns.flatMap((t) => t.toolCalls.map((x) => x.name)))],
  };
}

// Sobi'ye özel komut satırı seçenekleri (parseArgs bunları buraya yazar).
const argExtra = { sec: null, hakemModel: null, hakemEffort: null, sahte: null, hakemsiz: false, kayittan: null, tumAdimlar: false, effort: chat.DEFAULT_EFFORT };

// --- harness (you usually won't need to touch below this line) --------------

function parseArgs(argv) {
  const a = { flow: '.claude/hillclimb/flow', variant: 'baseline',
              // Varsayılan Sobi modeli: sunan model bununla karşılaştırılıyor (chat.DEFAULT_MODEL).
              model: chat.DEFAULT_MODEL, reps: 1, concurrency: 4, timeoutS: 300,
              approveHarness: false };
  // A flag at the end of argv would otherwise consume undefined - which for
  // --model equals the default and silently disables the served-model check.
  const val = (i) => { if (argv[i] === undefined) { eprint(`missing value for ${argv[i - 1]}`); usage(); process.exit(2); } return argv[i]; };
  for (let i = 0; i < argv.length; i++) {
    const k = argv[i];
    if (k === '--flow') a.flow = val(++i);
    else if (k === '--variant') a.variant = val(++i);
    else if (k === '--model') a.model = val(++i);
    else if (k === '--reps') a.reps = +val(++i);
    else if (k === '--concurrency') a.concurrency = +val(++i);
    else if (k === '--timeout-s') a.timeoutS = +val(++i);
    else if (k === '--approve-harness') a.approveHarness = true;
    else if (k === '--sec') argExtra.sec = val(++i);
    else if (k === '--hakem-model') argExtra.hakemModel = val(++i);
    else if (k === '--hakem-effort') argExtra.hakemEffort = val(++i);
    else if (k === '--effort') argExtra.effort = val(++i);
    else if (k === '--sahte') argExtra.sahte = val(++i);
    else if (k === '--hakemsiz') argExtra.hakemsiz = true;
    else if (k === '--kayittan') argExtra.kayittan = val(++i);
    else if (k === '--tum-adimlar') argExtra.tumAdimlar = true;
    else if (k === '-h' || k === '--help') { usage(); process.exit(0); }
    else { eprint(`unknown argument: ${k}`); usage(); process.exit(2); }
  }
  if (!/^(baseline|v[1-9]\d*)$/.test(a.variant)) {
    // The report only reads directories named 'baseline' or 'v<N>' - any other
    // name runs to completion but spends the pass into a directory the Summary,
    // trajectory, and budget arithmetic never see.
    eprint(`--variant must be 'baseline' or 'v<N>', got '${a.variant}'`);
    usage(); process.exit(2);
  }
  if (!Number.isFinite(a.timeoutS) || a.timeoutS < 0
      || a.timeoutS * 1000 > 2147483647 // setTimeout clamps >2^31-1 ms to 1 ms - the ceiling would fire instantly
      || !Number.isInteger(a.reps) || a.reps < 1
      || !Number.isInteger(a.concurrency) || a.concurrency < 1) { usage(); process.exit(2); }
  return a;
}
function usage() {
  eprint('usage: node run-eval.mjs --flow DIR --variant ID [--model ID] [--reps N] [--concurrency N] [--timeout-s N (0 = no ceiling)] [--approve-harness]');
}

// Harness integrity gate. The hillclimb loop gets this runner command
// allowlisted for the session and then runs rounds unattended, while the
// per-round change (proposed by an analyzer fed untrusted transcripts) may
// legitimately edit harness code. Without this gate a round that rewrites the
// runner would execute attacker-chosen code on the next unattended run under
// the user's one-time approval. So: sha256 over this file, any lockfile
// beside it or in the directory the runner is invoked from (dependency drift
// is harness drift too; with neither, a note says so), plus
// every path in `_state.json.harness_paths` (relative to the directory the
// runner is invoked from, i.e. the repo root); compare to
// `_state.json.harness_sha`; refuse on absent/mismatch unless a human passes
// --approve-harness, which records the new sha. That write is the one
// sanctioned exception to "never write _state.json".
//
// Scope, stated plainly: this is a change detector, not a security boundary.
// The sha, the path list and this file all live where the loop agent can
// write, so an agent that is already prompt-injected can forge the record or
// pass --approve-harness itself. What it catches is the common case - a
// round's harness edit reaching an unattended run unreviewed - and what
// actually bounds an unattended run is the permission allowlist the user
// grants the runner command (scope it to this exact command line, not a
// prefix). Installed dependencies beyond the lockfile are outside the digest.
function checkHarness(statePath, st, approve) {
  const self = fileURLToPath(import.meta.url);
  const listed = Array.isArray(st.harness_paths) ? st.harness_paths.map(String) : [];
  const lockfiles = [...new Set([dirname(self), process.cwd()].flatMap(d =>
    ['package-lock.json', 'bun.lock', 'bun.lockb', 'yarn.lock', 'pnpm-lock.yaml'].map(f => join(d, f))))]
    .filter(f => existsSync(f));
  const paths = [...new Set([self, ...lockfiles, ...listed.map(p => resolve(p))])].sort();
  const h = createHash('sha256');
  const hashed = [];
  for (const p of paths) {
    let buf;
    try { buf = readFileSync(p); }
    catch (e) {
      if (p === self) throw e;
      eprint(`warning: harness path '${relative(process.cwd(), p)}' not readable (${e?.code || 'error'}) - skipped`);
      continue;
    }
    h.update(relative(process.cwd(), p)).update('\0').update(buf).update('\0');
    hashed.push(relative(process.cwd(), p));
  }
  const sha = h.digest('hex');
  if (st.harness_sha === sha) return;
  // Said only here, where a person is about to approve or is being refused.
  if (!lockfiles.length) eprint('note: no lockfile beside the runner or in the current directory - dependency changes are outside the harness sha');
  if (approve) {
    st.harness_sha = sha;
    writeFileNoFollow(statePath, JSON.stringify(st, null, 2) + '\n');
    eprint(`harness approved: sha256 ${sha.slice(0, 12)} over ${hashed.length} file(s) recorded in ${statePath}`);
    return;
  }
  if (st.harness_sha == null) {
    eprint(`no approved harness sha in ${statePath} (computed ${sha.slice(0, 12)} over: ${hashed.join(', ')}).`);
    eprint('Review the harness, then run once with --approve-harness to record it.');
  } else {
    eprint(`harness changed since last approved run (files: ${hashed.join(', ')}); `
      + `approved ${String(st.harness_sha).slice(0, 12)}, now ${sha.slice(0, 12)}.`);
    eprint('Re-run with --approve-harness after reviewing the diff.');
  }
  process.exit(2);
}

// Transient provider errors (429 / overloaded / 5xx) retry with jittered
// exponential backoff - a zero-delay retry loop multiplies cost invisibly
// under rate limits and can turn one transient 429 into a torn-down batch.
// The attempt count lands in the row's meta (or the errors sidecar) so retry
// churn is visible in the data, not just the bill.
async function withBackoff(fn, retry, deadline = Infinity, tries = 5) {
  for (let attempt = 0; ; attempt++) {
    // Checked before every attempt, not just before sleeps: once the case's
    // ceiling has passed, an abandoned chain must not issue another call
    // (e.g. a judge call after the app call consumed the whole ceiling).
    if (Date.now() >= deadline) {
      const e = new Error('wall-clock ceiling exceeded before attempt');
      e.failure_class = 'timeout';
      throw e;
    }
    try { return await fn(); } catch (e) {
      const status = e?.status ?? e?.response?.status;
      const transient = status === 429 || status === 529 || (status >= 500 && status < 600)
        || /overloaded|rate.?limit/i.test(String(e?.message ?? ''));
      if (!transient || attempt >= tries - 1) throw e;
      const delay = Math.min(60_000, 1000 * 2 ** attempt) * (0.5 + Math.random());
      // Never start a retry that would outlive the case's wall-clock ceiling - 
      // otherwise an abandoned chain keeps issuing API calls after the case failed.
      if (Date.now() + delay >= deadline) throw e;
      retry.count++;
      await new Promise(r => setTimeout(r, delay));
    }
  }
}

// Hard per-case wall-clock ceiling, independent of stream liveness - a hung
// SSE stream can emit keepalives forever, defeating inactivity-based timers.
// The underlying call may keep running; the case fails and the slot is freed.
function withTimeout(promise, seconds, label) {
  if (!(seconds > 0)) return promise;
  let timer;
  const ceiling = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const e = new Error(`${label}: exceeded ${seconds}s wall-clock ceiling`);
      e.failure_class = 'timeout';
      reject(e);
    }, seconds * 1000);
  });
  return Promise.race([promise, ceiling]).finally(() => clearTimeout(timer));
}

// Case ids appear in file paths AND as the row/file join key the report uses,
// so rows, trace filenames, and frozen refs all carry the same path-safe id.
// When sanitization changes the id, a short content hash keeps distinct ids
// distinct ('case/1' vs 'case_1'); the original rides in meta.original_id.
function pathSafeId(id) {
  const raw = String(id);
  const cleaned = raw.replace(/[^\w.-]/g, '_');
  // Idempotent by construction: anything already path-safe and within the
  // length bound - including this function's own truncated+suffixed output - 
  // passes through unchanged. Long ids (URLs, prompt text as id) truncate to
  // 120 chars plus an 8-hex hash of the full original, so they fail here, not
  // at the trace write after the spend, and distinct ids stay distinct.
  if (cleaned === raw && raw.length <= 129) return raw;
  return `${cleaned.slice(0, 120)}-${createHash('sha256').update(raw).digest('hex').slice(0, 8)}`;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  // lstat("link/") follows the final symlink, so a trailing separator on
  // --flow would blind every leaf isSymlink check below - strip it first.
  // Only Windows treats `\` as a separator; on POSIX it is a filename byte, so
  // splitting on it would walk prefixes that are not real path components.
  args.flow = args.flow.replace(WIN ? /(.)[\\/]+$/ : /(.)\/+$/, '$1');
  const flowSegments = args.flow.split(WIN ? /[\\/]/ : '/');
  // A `.`/`..` segment (e.g. a trailing `/.`) makes isSymlink(args.flow) below
  // resolve a different final component than the named dir - following a
  // planted link at the flow root - while join() collapses it and the absolute
  // branch skips the ancestor walk. Refuse dot segments outright
  // (absolute --flow stays supported).
  if (flowSegments.some(seg => seg === '.' || seg === '..')) {
    eprint(`refusing to run: --flow must not contain '.' or '..' segments, got '${args.flow}'`);
    process.exit(2);
  }
  const vdir = join(args.flow, args.variant);
  // Preflight every output path before the first model call: a planted
  // symlink would otherwise fail each case after its (billed) run.
  for (const p of [args.flow, join(args.flow, 'baseline'), vdir, join(vdir, 'traces'),
                   join(vdir, 'results.jsonl'), join(vdir, 'errors.jsonl'),
                   join(vdir, 'progress.txt'), join(args.flow, 'baseline', 'ref'), join(args.flow, '_state.json')])
    if (isSymlink(p)) { eprint(`refusing to run: ${p} is a symlink (the flow dir must hold regular files)`); process.exit(2); }
  // A relative --flow (the documented `.claude/hillclimb/<name>` layout) is
  // also lstat-walked component by component from the cwd: a pre-planted
  // link at an ancestor (`.claude/hillclimb -> elsewhere`) would otherwise
  // relocate the root capture below - the containment anchor itself - to the
  // attacker's target. An absolute --flow is the caller's own trust decision
  // and is not walked (an absolute ancestor link can be legitimate: /tmp on
  // macOS).
  if (!isAbsolute(args.flow)) {
    let walk = '';
    for (const part of flowSegments.filter(Boolean).slice(0, -1)) {
      walk = walk ? join(walk, part) : part;
      if (isSymlink(walk)) { eprint(`refusing to run: ${walk} is a symlink (ancestor of --flow)`); process.exit(2); }
    }
  }
  // Every later open/mkdir is bound to this resolved root (see assertInFlow):
  // create the flow dir when fresh (the preflight above refused a link at it
  // and, for a relative path, at every ancestor), then capture where it
  // really resolves.
  mkdirSync(args.flow, { recursive: true });
  flowRealRoot = realpathSync(args.flow);
  mkdirNoFollow(join(vdir, 'traces'));
  // _state.json is READ-ONLY here. The orchestrator owns it. Absent is fine
  // (a baseline-only run has no loop state yet), but present-and-unparsable
  // must not let the id-space gate below pass vacuously over a corrupt file.
  const statePath = join(args.flow, '_state.json');
  let st = {};
  // Read through the no-follow opener like every other flow-dir file; the
  // parse message is not echoed (it can quote the file's first bytes).
  const stateText = readIfPresent(statePath);
  if (stateText != null) {
    try { st = JSON.parse(stateText) || {}; }
    catch { eprint(`${statePath} exists but is not valid JSON - fix it before spending a pass`); process.exit(2); }
  }
  checkHarness(statePath, st, args.approveHarness);
  const ctx = { ...args, state: st };

  // Resume: which (id, rep) pairs already have a row?
  const resultsPath = join(vdir, 'results.jsonl');
  const done = new Set();
  for (const ln of (readIfPresent(resultsPath) ?? '').split('\n')) {
    if (!ln.trim()) continue;
    try { const r = JSON.parse(ln); done.add(`${r.prompt_id}\0${r.rep}`); } catch {}
  }
  // Rows key on the path-safe id (see pathSafeId), so resume must too.

  const cases = await loadCases();
  // Validate the id space before spending anything: duplicate path-safe ids - 
  // including case-insensitive twins, which macOS/Windows filesystems collapse - 
  // would silently overwrite traces and frozen refs; and a _state.json split id
  // that matches no case would silently shrink the scored denominator.
  const seen = new Map();
  for (const c of cases) {
    const k = pathSafeId(c.id).toLowerCase();
    if (seen.has(k)) {
      eprint(`duplicate case id after sanitization: '${c.id}' collides with '${seen.get(k)}'`);
      process.exit(2);
    }
    seen.set(k, c.id);
  }
  const safeIds = new Set(cases.map(c => pathSafeId(c.id)));
  for (const k of ['train_ids', 'val_ids', 'test_ids'])
    if (st[k] != null && !Array.isArray(st[k])) { eprint(`_state.json ${k} must be a list of ids`); process.exit(2); }
  for (const sid of [...(st.train_ids ?? []), ...(st.val_ids ?? []), ...(st.test_ids ?? [])]) {
    const s = String(sid); // the adapter joins with String() on both sides - numeric ids are fine
    if (safeIds.has(s)) continue; // matches a loaded case - definitionally valid
    if (s !== pathSafeId(s)) {
      // Can never match a row: rows key on path-safe ids. This is the silent
      // shrunken-denominator bug - fail before anything is spent.
      eprint(`_state.json split id '${s}' is not a path-safe id - record split ids exactly as they appear in results.jsonl's prompt_id`);
      process.exit(2);
    }
    // Well-formed but absent is legitimate (a trimmed top-K subset run) - note it, don't fail.
    eprint(`note: split id '${s}' matches no loaded case (expected for a trimmed subset run)`);
  }
  const refDir = join(args.flow, 'baseline', 'ref');
  const tasks = [];
  for (const c of cases) for (let rep = 0; rep < args.reps; rep++) {
    if (done.has(`${pathSafeId(c.id)}\0${rep}`)) continue;
    tasks.push({ c, rep });
  }
  eprint(`[${args.variant}] ${tasks.length} of ${cases.length * args.reps} (id,rep) to run`);

  let i = 0, ok = 0, fail = 0;
  const errorsPath = join(vdir, 'errors.jsonl');
  // A hard crash (power loss, ENOSPC) can leave a torn final line with no
  // trailing newline; the next append would merge two rows into one permanently
  // unparseable line. Isolate any fragment before appending anything.
  for (const p of [resultsPath, errorsPath]) {
    const tail = readIfPresent(p);
    if (tail && !tail.endsWith('\n')) appendFileNoFollow(p, '\n');
  }
  async function worker() {
    while (i < tasks.length) {
      const { c, rep } = tasks[i++];
      const safeId = pathSafeId(c.id);
      const t0 = Date.now();
      let lastRun = null;    // survives into the catch - billed spend on a failed attempt
      let rowWritten = false; // set once the results row lands - the attempt is scored
      const deadline = args.timeoutS > 0 ? t0 + args.timeoutS * 1000 : Infinity;
      const appRetry = { count: 0 }, judgeRetry = { count: 0 };
      try {
        // One ceiling over the whole case - app call, identity check, and grading - 
        // so a hung judge stream can't hold the slot either.
        const { run, g, latency_s } = await withTimeout((async () => {
          let tAttempt = t0;
          const run = await withBackoff(() => { tAttempt = Date.now(); return runCase(c, ctx); },
            appRetry, deadline);
          lastRun = run;
          // latency_s = the final app attempt only; backoff sleeps, failed
          // attempts, and judge time are excluded (retry counts are in meta).
          const latency_s = (Date.now() - tAttempt) / 1000;
          // Serving identity: fail loudly when the response was served by a model
          // other than the one requested. Accept exact match or a documented
          // alias->snapshot resolution - 'foo-latest'/'foo-0'/'foo' served as
          // 'foo-20250101', 'foo@20250101', or 'foo-2025-01-01'. Anything else - 
          // another snapshot of the requested pin, a sibling model, or the bare
          // base id ('foo-latest' served as 'foo', an unversioned echo that can
          // hide snapshot drift across rounds) - fails the attempt. Non-Anthropic
          // id schemes (e.g. Bedrock's 'anthropic.claude-...-v1:0') need their own
          // rule here.
          if (ctx.model && run.model && run.model !== ctx.model) {
            const base = ctx.model.replace(/-latest$|-0$/, '');
            const rest = String(run.model).startsWith(base)
              ? String(run.model).slice(base.length) : null;
            if (!(rest != null && /^[-@](\d{8}|\d{4}-\d{2}-\d{2})$/.test(rest))) {
              const e = new Error(`served model ${run.model} != requested ${ctx.model}`);
              e.failure_class = 'serving_substitution';
              throw e;
            }
          }
          // Frozen pairwise reference (never regenerated): baseline/ref/<id>.*
          let ref = null;
          if (args.variant !== 'baseline') {
            const p = join(refDir, safeId);
            // A planted symlink throws (ELOOP) rather than feeding the judge
            // its target; the case then fails loudly instead of leaking.
            for (const ext of REF_EXTS) {
              try { ref = readFileNoFollow(p + ext); break; }
              catch (e) { if (e?.code !== 'ENOENT') throw e; }
            }
          }
          const g = await withBackoff(() => gradeCase(c, run, ref, ctx), judgeRetry, deadline);
          return { run, g, latency_s };
        })(), args.timeoutS, `${c.id} rep${rep}`);
        const row = {
          prompt_id: safeId, rep, prompt: c.prompt ?? c.input ?? c.id,
          tags: c.tags, attachments: c.attachments,
          meta: safeId !== String(c.id) || appRetry.count || judgeRetry.count
            ? { ...(c.meta ?? {}),
                ...(safeId !== String(c.id) ? { original_id: String(c.id) } : {}),
                ...(appRetry.count ? { retries: appRetry.count } : {}),
                ...(judgeRetry.count ? { judge_retries: judgeRetry.count } : {}) }
            : c.meta,
          model: run.model, usage: run.usage, stop_reason: run.stop_reason,
          // The report keys on `status`, not stop_reason: a clipped answer is
          // counted and shown but kept out of the means. runCase may set
          // run.status to override the max_tokens rule.
          status: run.status ?? (run.stop_reason === 'max_tokens' ? 'truncated' : 'ok'),
          judge_model: g.judge_model ?? run.judge_model,
          judge_usage: g.judge_usage ?? run.judge_usage,
          latency_s, ...perfFrom(run),
          grade: g.grade, explanation: g.explanation,
        };
        appendFileNoFollow(resultsPath, JSON.stringify(row) + '\n');
        rowWritten = true; // past this point the attempt is scored - a later throw (trace write, ref freeze) must not also append an error row
        if (run.transcript)
          writeFileNoFollow(join(vdir, 'traces', `${safeId}_rep${rep}.json`),
            JSON.stringify(run.transcript, null, 2));
        // For pairwise: on the baseline run, freeze the reference output once.
        if (args.variant === 'baseline' && run.output != null
            && !REF_EXTS.some(ext => lexists(join(refDir, safeId) + ext))) {
          mkdirNoFollow(refDir);
          writeFileNoFollow(join(refDir, safeId),
            typeof run.output === 'string' ? run.output : JSON.stringify(run.output));
        }
        ok++;
      } catch (e) {
        fail++;
        if (rowWritten) {
          // The attempt scored; only a post-row write (trace, ref) failed. An error
          // row here would double-count the billed usage under the budget rule.
          eprint(`  [${args.variant}] ${c.id} rep${rep} scored, but a post-row write failed: ${e?.message || e}`);
          continue;
        }
        // Failed attempts are data too - but they must not occupy the (case, rep)
        // slot in results.jsonl, or resume would never re-run them.
        appendFileNoFollow(errorsPath, JSON.stringify({
          prompt_id: safeId, rep,
          ...(safeId !== String(c.id) ? { original_id: String(c.id) } : {}),
          failure_class: e?.failure_class ?? 'error',
          error: String(e?.message || e),
          retries: appRetry.count, judge_retries: judgeRetry.count,
          // Billed-but-failed spend stays countable: when the app call completed
          // before the failure (e.g. a served-model mismatch, a judge-stage
          // ceiling), carry its identity and usage on the error row.
          model: lastRun?.model, usage: lastRun?.usage,
          judge_model: e?.judge_model ?? lastRun?.judge_model,
          judge_usage: e?.judge_usage ?? lastRun?.judge_usage,
          latency_s: (Date.now() - t0) / 1000,
        }) + '\n');
        eprint(`  [${args.variant}] ${c.id} rep${rep} FAILED: ${e?.message || e}`);
      }
    }
  }
  // One progress line every 30s (and to <vdir>/progress.txt) so "how far along
  // is it?" is answerable from the background shell's output or one file read,
  // without the orchestrator parsing results.jsonl mid-write. ETA is a plain
  // rate extrapolation from this pass.
  const t0 = Date.now();
  const progress = () => {
    const done = ok + fail, total = tasks.length;
    const el = (Date.now() - t0) / 1000;
    const eta = done ? Math.round((el / done) * (total - done)) : null;
    const line = `[${args.variant}] ${done}/${total} done (${ok} ok, ${fail} failed), `
      + `${Math.round(el)}s elapsed` + (eta != null ? `, ~${eta}s left` : '');
    eprint(line);
    try { writeFileNoFollow(join(vdir, 'progress.txt'), line + '\n'); } catch {}
  };
  const tick = setInterval(progress, 30_000);
  workersStarted = true;
  await Promise.all(Array.from({ length: Math.max(1, args.concurrency) }, worker));
  clearInterval(tick); progress();
  eprint(`[${args.variant}] done - ${ok} ok, ${fail} failed -> ${resultsPath}`);
  process.exit(fail ? 1 : 0);
}

// Anything main() throws prints as one sanitized line, not a raw stack. Before
// the workers start it is a refusal (a planted link at _state.json or
// results.jsonl, an lstat that fails, an error from loadCases) and exits 2 like
// the preflight refusals. After they start, only a failed errors.jsonl append
// gets here; rows may already be on disk, so say that and exit 1.
let workersStarted = false;
// test-eval.mjs bu dosyayı içe aktarıp parçalarını API'siz sınıyor; o zaman main() çalışmaz.
export { loadCases, runCase, gradeCase, programmatic, strictUnknown, judgeInput, argExtra, moveTuples };
if (!process.env.SOBI_EVAL_IMPORT) main().catch(e => {
  const m = String(e?.message || e);
  if (workersStarted) { eprint('stopped mid-run (rows already written are kept; re-run to resume): ' + m); process.exit(1); }
  eprint(m.startsWith('refusing to ') ? m : 'refusing to run: ' + m);
  process.exit(2);
});
