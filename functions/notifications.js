'use strict';

/**
 * BİLDİRİM ÇEKİRDEĞİ — "bu bildirim gönderilir mi, gönderilirse nasıl" kararı.
 *
 * NEDEN AYRI BİR MODÜL
 *   Uygulamada iki bildirim vardı (sohbet mesajı ve akşam seri hatırlatması) ve ikisi de
 *   kararını kendi içinde veriyordu. Tür sayısı artarken bu dağınıklığın iki bedeli var:
 *
 *     • Kullanıcının elinde TEK bir açma/kapama anahtarı vardı (`notificationsEnabled`).
 *       Oyun bildiriminden rahatsız olan kullanıcının kapatacağı şey sohbet bildirimi de
 *       dahil her şeydi — oysa sohbet bildirimi ürünün çalışması için zorunlu olan tek
 *       bildirim: öğretmenin cevabını duymayan öğrenci sorusunun cevaplandığını bilmiyor.
 *     • Toplam sayıya kimse bakmıyordu. Her tür kendi başına makulken toplamı rahatsız
 *       edici olabilir, ve kullanıcı rahatsız olduğunda tek tek türleri değil uygulamayı
 *       kapatıyor.
 *
 *   Bu yüzden karar tek yerden veriliyor ve her yeni tür buraya bir satır olarak ekleniyor.
 *
 * NEDEN YAN ETKİSİZ
 *   Firestore'a ve FCM'e dokunan taraf index.js (`sendUserNotification`); burada yalnızca
 *   saf karar var. Sebep test: tercih/sessiz saat/tavan kombinasyonlarını emülatör kurmadan,
 *   saniyeler içinde koşturabiliyoruz (bkz. scripts/test-notifications.js). Aynı gerekçeyle
 *   ayrılmış ikizi: streakReminder.js.
 */

/**
 * TÜR KATALOĞU — yeni bir bildirim türü buraya bir satır olarak eklenir.
 *
 * Alanlar:
 *   pref    Kullanıcının uygulama içinden kapatabileceği tercih anahtarı. `null` ise tür
 *           uygulama içinden kapatılamaz; yalnızca Android'in kendi kanal ayarından
 *           susturulabilir. Bu yalnızca İŞLEMSEL türler için geçerli (bkz. aşağıda).
 *   capped  Günlük/haftalık tavana tabi mi.
 *   mutable Üst üste açılmazsa kendiliğinden susabilir mi (bkz. [UNOPENED_MUTE_AFTER]).
 *   quiet   Sessiz saatlere (bkz. [QUIET_START_HOUR]) denk gelirse ne olacak.
 *   channel İstemcinin hangi Android kanalını kullanacağı. Türlerin ayrı kanalları olması
 *           şart: kullanıcı seri hatırlatmasını Android ayarlarından kapatırken öğretmen
 *           mesajlarını kapatmak zorunda kalmamalı.
 *
 * TAVAN NEDEN HERKESE UYGULANMIYOR
 *   İki grup var ve ayrımı şu: kullanıcı bu bildirimi kaçırırsa ZARAR GÖRÜR MÜ?
 *
 *     • İşlemsel (`capped: false`) — sohbet mesajı, hesap/ödeme, sorusunun durumu. Kaçırmak
 *       kullanıcının aleyhine: cevabı görmez, kredisinin iade edildiğini bilmez, denemesinin
 *       bittiğini fark etmez. Bunlar tavana takılıp düşmemeli.
 *     • Etkileşim (`capped: true`) — seri hatırlatması, ödül ve etkinlikler. Kaçırmak bir
 *       şey kaybettirmiyor, yalnızca bizim istediğimiz dönüşü geciktiriyor. Tavan bunlara.
 *
 * SUSTURMA NEDEN SADECE `reward`'DA
 *   Susturmanın amacı, kullanıcının ilgilenmediği bir ETKİLEŞİM konusunu kesmek. Ötekilerde
 *   aynı sayaç ters çalışıyor, çünkü "dokunulmadı" ile "işe yaramadı" aynı şey değil:
 *   bildirimi görüp uygulamayı kendi açan kullanıcı hiç dokunmamış sayılıyor. Seri
 *   hatırlatması tam böyle çalışıyor — susturulabilir olsaydı ürünün en değerli bildirimi,
 *   işini düzgün yaptığı için beş günde kendini kapatırdı.
 */
const NOTIFICATION_TYPES = {
  /** Soru sohbetine gelen mesaj. Ürünün çalışması için zorunlu; kapatılabilir ama tavansız. */
  chat: { pref: 'chat', capped: false, mutable: false, quiet: 'silence', channel: 'messages' },

  /**
   * Akşam seri hatırlatması. Saati kullanıcı seçiyor, o yüzden sessiz saat uygulanmıyor.
   *
   * `capped: false` — kullanıcının AÇIKÇA İSTEDİĞİ bildirim: saatini kendisi seçti ve günde
   * bir taneyle sınırlı (`reminderSentDay`, bkz. streakReminder.js). Bir pazarlama bildirimi
   * değil, kurduğu alarm. Tavana tabi olsaydı o gün gönderilmiş bir ödül bildirimi yüzünden
   * düşebilirdi — kullanıcının istediği şey, istemediği şey uğruna.
   */
  streak: { pref: 'streak', capped: false, mutable: false, quiet: 'send', channel: 'streak_reminder' },

  /** Sandık, sezon, enerji — oyun tarafı. Geceye denk gelirse düşer; sabahı beklemez. */
  reward: { pref: 'reward', capped: true, mutable: true, quiet: 'drop', channel: 'rewards' },

  /**
   * Hesap ve ödeme: deneme süresi bitişi, kredi iadesi, sorusunun durumu.
   *
   * `pref: null` — uygulama içinden kapatılamıyor. Gerekçe: kullanıcının parasıyla ve
   * verdiğimiz sözle ilgili bilgi, bir pazarlama bildirimi gibi kapatılabilir olmamalı; bir
   * kez kaçırıldığında yerine geçecek başka bir kanal yok. Yine de kullanıcı çaresiz değil:
   * Android'in kanal ayarından (`account`) susturabiliyor. Bu bilinçli bir tercih, ihmal
   * değil.
   *
   * `quiet: 'send'` — zamanlanmış türler kullanıcının yerel saatine göre SEÇİLDİĞİ için
   * (bkz. streakReminder.reminderHourUtc deseni) buraya gelen çağrı zaten uygun saatte.
   */
  account: { pref: null, capped: false, mutable: false, quiet: 'send', channel: 'account' },

  /**
   * Öğretmene: havuza yeni soru düştü.
   *
   * Öğretmen tarafı bu bildirime kadar tamamen sessizdi — soruyu SAHİPLENDİKTEN sonraki
   * mesajları duyuyor, havuza düşen soruyu duymuyordu. Bu boşluk doğrudan paraya dokunuyor:
   * 48 saat içinde cevaplanmayan soru öğrencinin kredisini iade ediyor
   * (`QUESTION_REFUND_AFTER_MS`), yani her kaçan soru hem gelir hem güven kaybı.
   *
   * `pref: null` — öğretmenin işi, uygulama içindeki öğrenciye yönelik tercih listesinde yeri
   * yok; gerekirse Android'in `pool` kanalından susturulabiliyor.
   * `capped: false` — iş bildirimi, pazarlama değil.
   * `quiet: 'silence'` — gece gelen soru görünsün ama öğretmeni uyandırmasın. Düşürmek
   * olmazdı: 48 saatlik pencerede geçen her saat önemli.
   */
  pool: { pref: null, capped: false, mutable: false, quiet: 'silence', channel: 'pool' },
};

/** Uygulama içinden kapatılabilen tercihler — istemcideki anahtarların kaynağı. */
const NOTIFICATION_PREFS = ['chat', 'streak', 'reward'];

/** Sessiz saatler: yerel [QUIET_START_HOUR]:00 — [QUIET_END_HOUR]:00. */
const QUIET_START_HOUR = 21;
const QUIET_END_HOUR = 8;

/**
 * Tavana tabi türlerden (pratikte `reward`) günde / son yedi günde en fazla kaç bildirim.
 *
 * Sayılar kitleye göre seçildi: kullanıcılar 7–10 yaş. Seri hatırlatması tavandan muaf
 * olduğu için kullanıcının gördüğü toplam şu: günde en fazla 1 hatırlatma + 1 ödül
 * bildirimi. Hedef buydu — sohbet dışı günde ortalama 1, en fazla 2.
 *
 * Tavanın SON slotu öncelikli konulara ayrılmış ([PRIORITY_TOPICS]): normal bir ödül
 * bildirimi günde 1'den fazlasını alamıyor, günün ikinci slotunu bekleyen öncelikli bir
 * konuya bırakıyor.
 */
const DAILY_CAP = 2;
const WEEKLY_CAP = 5;

/**
 * Tavanın öncelikli konulara ayrılmış slot sayısı.
 *
 * NEDEN REZERVASYON, NEDEN SIRALAMA DEĞİL
 *   Bildirimler gün içinde ayrı ayrı tetikleniyor, hepsini görüp aralarından seçebileceğimiz
 *   bir an yok. Rezervasyon olmadan tavan "ilk gelen geçer" demek olurdu: sezonun son günü
 *   sabah giden bir sandık hatırlatması, akşamki "sezon bitiyor"u düşürürdü.
 */
const RESERVED_FOR_PRIORITY = 1;

/**
 * Tavanın son slotunu kullanabilen konular — kullanıcı kararı (05.10.2026).
 *
 * `season_ending` burada çünkü kaçırıldığında telafisi yok: sezon kapanıyor ve sıralama
 * kesinleşiyor. Sezon sonu madalya bildirimi (`season_medals`) bilerek DIŞINDA: ödül zaten
 * hesaba geçmiş, kullanıcı uygulamayı açtığında orada duruyor — yani bildirim kaçsa bile
 * ödül kaçmıyor.
 */
const PRIORITY_TOPICS = ['season_ending'];

/**
 * Bir konu üst üste bu kadar kez gönderilip hiç açılmazsa o konu susuyor.
 *
 * NEDEN KONU BAZINDA
 *   Tür değil konu: "sezon bitiyor" ile "sandığın bekliyor" ikisi de `reward` türünde, ama
 *   biriyle ilgilenmeyen kullanıcı ötekiyle ilgilenebilir. Türü susturmak ikisini birden
 *   keserdi.
 *
 * NEDEN SONSUZA KADAR DEĞİL
 *   İlgisizlik kalıcı bir durum değil; kullanıcı aradan sonra rekabete dönebilir. Sayaç
 *   [resetUnopened] ile sıfırlanıyor — ilgili bildirimi açtığında ya da çağıran taraf
 *   kullanıcının o konuya geri döndüğünü gördüğünde (örn. sıralamaya yeni skor girmesi).
 */
const UNOPENED_MUTE_AFTER = 5;

/** UTC gün anahtarı (`yyyy-MM-dd`). */
function dayKey(nowMs) {
  return new Date(nowMs).toISOString().slice(0, 10);
}

/** Kullanıcının yerel saati (0–23), saat dilimi farkından. */
function localHourOf(nowMs, utcOffsetMinutes) {
  return new Date(nowMs + utcOffsetMinutes * 60000).getUTCHours();
}

/**
 * Saat dilimi farkı — bilinmiyorsa null.
 *
 * DİKKAT: `Number(null)` ve `Number('')` sıfıra dönüyor, yani düz bir `Number()` çevirimi
 * "farkı bilmiyorum" ile "kullanıcı UTC'de" durumlarını ayırt edemiyor. İkisi burada zıt
 * sonuç veriyor: UTC kullanıcısına sessiz saat uygulanırken, farkı bilinmeyene
 * uygulanmıyor. Aynı tuzak için bkz. streakReminder.normalizeReminderHour.
 */
function normalizeOffset(raw) {
  if (raw === null || raw === undefined || raw === '') return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

/** Yerel saat sessiz saatlere denk geliyor mu (gece yarısını aşan aralık). */
function isQuietHour(localHour) {
  return localHour >= QUIET_START_HOUR || localHour < QUIET_END_HOUR;
}

/**
 * Kullanıcının bildirim tercihleri — eksik alanlar açık sayılır.
 *
 * GERİYE DÖNÜK UYUM
 *   Tercihler eskiden tek bir alandı: `notificationsEnabled`. Eski istemci sürümleri hâlâ
 *   onu yazıyor ve kullanıcıların çoğunda yalnızca o var. Sıra şu:
 *
 *     1. `notificationPrefs.<tür>` varsa o — kullanıcının o tür için verdiği açık karar.
 *     2. Yoksa `notificationsEnabled` — eski tek anahtar; `false` ise hepsi kapalı.
 *     3. O da yoksa açık.
 *
 *   Daha özel olanın kazanması önemli: yeni istemcide sohbet bildirimini kapatmış kullanıcı,
 *   evdeki eski sürümlü tablete girip ana anahtarı açtığında kapattığı şey geri gelmemeli.
 */
function notificationPrefsFor(userData) {
  const d = userData || {};
  const raw = d.notificationPrefs && typeof d.notificationPrefs === 'object' ? d.notificationPrefs : {};
  const legacy = d.notificationsEnabled === false ? false : true;
  const out = {};
  for (const key of NOTIFICATION_PREFS) {
    out[key] = typeof raw[key] === 'boolean' ? raw[key] : legacy;
  }
  return out;
}

/**
 * Defterden son yedi günün gönderim sayıları — tavan hesabının girdisi.
 *
 * Defter `{ '2026-10-05': 2, ... }` biçiminde bir harita (bkz. [ledgerPatch]). Kayan pencere
 * tercih edildi: "hafta başı" diye sabit bir sınır olsaydı pazartesi sabahı tavan sıfırlanıp
 * kullanıcı pazar akşamıyla üst üste iki dolu gün yaşayabilirdi.
 */
function recentCounts(history, nowMs) {
  const h = history && typeof history === 'object' ? history : {};
  const today = dayKey(nowMs);
  let day = 0;
  let week = 0;
  for (let i = 0; i < 7; i++) {
    const key = dayKey(nowMs - i * 86400000);
    const n = Math.max(0, Math.trunc(Number(h[key]) || 0));
    week += n;
    if (key === today) day = n;
  }
  return { day, week };
}

/**
 * Bu bildirim gönderilecek mi; gönderilecekse nasıl.
 *
 * Dönen `send: false` sebepleri — hepsi kasıtlı ve ayrı ayrı sayılabilir olsun diye isimli:
 *   • `unknown_type`  — katalogda olmayan tür. Çağıran taraftaki yazım hatası.
 *   • `pref_off`      — kullanıcı bu türü kapatmış.
 *   • `quiet_hours`   — sessiz saate denk geldi ve tür geceyi beklemiyor (`quiet: 'drop'`).
 *   • `daily_cap`     — bugünün tavanı dolu.
 *   • `weekly_cap`    — son yedi günün tavanı dolu.
 *   • `topic_muted`   — bu konu üst üste [UNOPENED_MUTE_AFTER] kez açılmadı.
 *
 * @param type   Katalog anahtarı (`chat` | `streak` | `reward` | `account`).
 * @param topic  Sayaçların tutulacağı konu etiketi (örn. `season_ending`). Verilmezse tür.
 * @param state  { prefs, history, unopened, utcOffsetMinutes }
 */
function notificationDecision({ type, topic, state }, nowMs) {
  const spec = NOTIFICATION_TYPES[type];
  if (!spec) return { send: false, reason: 'unknown_type' };

  const key = topic || type;
  const s = state || {};

  if (spec.pref !== null) {
    const prefs = s.prefs || {};
    if (prefs[spec.pref] === false) return { send: false, reason: 'pref_off' };
  }

  // Saat dilimi bilinmiyorsa sessiz saat uygulanamıyor. Bildirimi düşürmek yerine
  // göndermeyi seçiyoruz: alan `submitStreakDay` ile doluyor, yani hiç seri çalışmamış yeni
  // kullanıcıda eksik — ve ona sohbet bildirimi gitmemesi, geceye denk gelme riskinden
  // daha büyük bir sorun.
  const offset = normalizeOffset(s.utcOffsetMinutes);
  let silent = false;
  if (offset !== null && isQuietHour(localHourOf(nowMs, offset))) {
    if (spec.quiet === 'drop') return { send: false, reason: 'quiet_hours' };
    if (spec.quiet === 'silence') silent = true;
  }

  if (spec.mutable) {
    const unopened = s.unopened && typeof s.unopened === 'object' ? s.unopened : {};
    if (Math.trunc(Number(unopened[key]) || 0) >= UNOPENED_MUTE_AFTER) {
      return { send: false, reason: 'topic_muted' };
    }
  }

  if (spec.capped) {
    // Öncelikli konular tavanın tamamını, geri kalanı rezerve slot hariçini kullanıyor.
    const reserve = PRIORITY_TOPICS.includes(key) ? 0 : RESERVED_FOR_PRIORITY;
    const counts = recentCounts(s.history, nowMs);
    if (counts.day >= DAILY_CAP - reserve) return { send: false, reason: 'daily_cap' };
    if (counts.week >= WEEKLY_CAP - reserve) return { send: false, reason: 'weekly_cap' };
  }

  return {
    send: true,
    topic: key,
    // İstemci bildirimi hangi kanalda göstereceğini buradan öğreniyor. Sessiz varyant ayrı
    // bir kanal olmak zorunda: Android'de bir kanalın önem derecesi oluşturulduktan sonra
    // uygulama tarafından değiştirilemiyor.
    channel: silent ? `${spec.channel}_quiet` : spec.channel,
    silent,
    counted: spec.capped,
  };
}

/**
 * Gönderim sonrası deftere yazılacak değişiklik.
 *
 * Yedi günden eski gün anahtarları düşürülüyor — defter süresiz büyümesin ve zaten kayan
 * pencere yalnızca son yediye bakıyor. Haritanın tamamı yeniden yazılıyor (`merge` ile
 * birleştirilmiyor): eski anahtarları silmenin tek yolu bu.
 */
function ledgerPatch(ledger, decision, nowMs) {
  const prev = ledger && typeof ledger === 'object' ? ledger : {};
  const history = {};
  if (prev.history && typeof prev.history === 'object') {
    for (let i = 0; i < 7; i++) {
      const key = dayKey(nowMs - i * 86400000);
      const n = Math.max(0, Math.trunc(Number(prev.history[key]) || 0));
      if (n > 0) history[key] = n;
    }
  }
  if (decision.counted) {
    const today = dayKey(nowMs);
    history[today] = (history[today] || 0) + 1;
  }

  const sent = { ...(prev.sent && typeof prev.sent === 'object' ? prev.sent : {}) };
  sent[decision.topic] = Math.max(0, Math.trunc(Number(sent[decision.topic]) || 0)) + 1;

  const unopened = { ...(prev.unopened && typeof prev.unopened === 'object' ? prev.unopened : {}) };
  unopened[decision.topic] = Math.max(0, Math.trunc(Number(unopened[decision.topic]) || 0)) + 1;

  return { history, sent, unopened };
}

/**
 * Kullanıcı bildirimi açtı: o konunun "açılmadı" sayacı sıfırlanır, açılma sayısı artar.
 *
 * Sayacın sıfırlanması susturmayı da kaldırıyor — konu yeniden gönderilebilir hale geliyor.
 */
function openPatch(ledger, topic) {
  const prev = ledger && typeof ledger === 'object' ? ledger : {};
  const opened = { ...(prev.opened && typeof prev.opened === 'object' ? prev.opened : {}) };
  opened[topic] = Math.max(0, Math.trunc(Number(opened[topic]) || 0)) + 1;
  const unopened = { ...(prev.unopened && typeof prev.unopened === 'object' ? prev.unopened : {}) };
  unopened[topic] = 0;
  return { opened, unopened };
}

module.exports = {
  NOTIFICATION_TYPES,
  NOTIFICATION_PREFS,
  QUIET_START_HOUR,
  QUIET_END_HOUR,
  DAILY_CAP,
  WEEKLY_CAP,
  RESERVED_FOR_PRIORITY,
  PRIORITY_TOPICS,
  UNOPENED_MUTE_AFTER,
  dayKey,
  localHourOf,
  normalizeOffset,
  isQuietHour,
  notificationPrefsFor,
  recentCounts,
  notificationDecision,
  ledgerPatch,
  openPatch,
};
