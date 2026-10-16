/**
 * Akşam seri hatırlatmasının KARAR mantığı.
 *
 * Firestore'a ve FCM'e dokunan tarama index.js'te (`sendStreakReminders`); burada yalnızca
 * "bu kullanıcıya bugün hatırlatma gider mi, giderse ne yazar" sorusu var.
 *
 * Ayrı modül olmasının sebebi test: kararın tamamı yan etkisiz olduğu için emülatör
 * kurmadan, saniyeler içinde çalıştırılabiliyor (bkz. scripts/test-streak-reminder.js).
 */

/**
 * Kullanıcının yerel saatiyle hatırlatmanın gönderileceği saat — kullanıcı kendi saatini
 * seçmediyse. Seçim kayıtta ve seri ekranında yapılıyor (`reminderLocalHour`).
 */
const STREAK_REMINDER_LOCAL_HOUR = 19;

/**
 * İstemcinin gönderdiği hatırlatma saati; tam saat 0–23 değilse null.
 *
 * Tarama saatte bir çalıştığı için dakika kabul edilmiyor. Hangi saatlerin SUNULACAĞI
 * istemcinin işi (çocuklara okul saati ve gece önerilmiyor); burada yalnızca geçerlilik.
 */
function normalizeReminderHour(raw) {
  // null/boş Number()'da 0'a dönüyor; "seçim yok" gece yarısı sanılmasın.
  if (raw === null || raw === undefined || raw === '') return null;
  const hour = Number(raw);
  return Number.isInteger(hour) && hour >= 0 && hour <= 23 ? hour : null;
}

/** Kaç gündür görülmeyene hatırlatma gönderilmesin. */
const STREAK_REMINDER_MAX_IDLE_DAYS = 7;

/** Kullanıcının yerel takvim günü (`yyyy-MM-dd`), saat dilimi farkından. */
function localDayId(nowMs, utcOffsetMinutes) {
  return new Date(nowMs + utcOffsetMinutes * 60000).toISOString().slice(0, 10);
}

/**
 * Kullanıcının yerel saatiyle [localHour]'a (verilmezse [STREAK_REMINDER_LOCAL_HOUR]) denk
 * gelen UTC saati.
 *
 * Saatlik tarama "şu anda yerel saati hatırlatma saati olanlar" sorgusunu bu alan üzerinden,
 * indeksli tek bir eşitlikle yapıyor — tüm kullanıcıları taramak yerine.
 */
function reminderHourUtc(utcOffsetMinutes, localHour = STREAK_REMINDER_LOCAL_HOUR) {
  const localMinutes = localHour * 60;
  const utcMinutes = (((localMinutes - utcOffsetMinutes) % 1440) + 1440) % 1440;
  return Math.floor(utcMinutes / 60);
}

/** `yyyy-MM-dd` → epoch gün sayısı; geçersizse null. */
function dayNumber(dayId) {
  if (typeof dayId !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(dayId)) return null;
  const ms = Date.parse(`${dayId}T00:00:00Z`);
  return Number.isFinite(ms) ? Math.floor(ms / 86400000) : null;
}

/**
 * Serinin BUGÜN gerçekte ne durumda olduğu: `{ current, freezes }`.
 *
 * Sunucu kırılmayı yazmıyor — `current` yeni bir gün bildirilene kadar eski değerinde
 * duruyor (bkz. index.js → settleStreakFreeze). İstemci ise kırılmayı okurken hesaplıyor
 * ve 0 gösteriyor (StreakRepository.refresh). Bu yüzden saklanan `current`'ı olduğu gibi
 * kullanmak, ekranda serisi 0 olan çocuğa "1 günlük serin devam etsin" yazdırıyordu.
 *
 * Kurallar istemciyle ve settleStreakFreeze ile aynı:
 *   • lastDay bugün ya da dün → seri canlı.
 *   • lastDay evvelsi gün ve dondurma var (kaçan günden önce alınmamış değil) → dün
 *     dondurmayla kapanacak; seri canlı ama dondurma artık harcanmış sayılır.
 *   • aksi halde seri kırık → 0.
 */
function liveStreak(state, today) {
  const current = Math.max(0, Math.trunc(Number(state.current) || 0));
  const freezes = Math.max(0, Math.trunc(Number(state.freezes) || 0));
  const todayNo = dayNumber(today);
  const lastNo = dayNumber(state.lastDay);
  if (current <= 0 || todayNo === null || lastNo === null) return { current: 0, freezes };

  const gap = todayNo - lastNo;
  if (gap <= 1) return { current, freezes };
  if (gap === 2 && freezes > 0) {
    // Dondurma, satın alındığı günden öncesini kurtarmaz (settleStreakFreeze ile aynı şart).
    const boughtNo = dayNumber(state.freezeDay);
    if (boughtNo === null || lastNo + 1 >= boughtNo) return { current, freezes: freezes - 1 };
  }
  return { current: 0, freezes };
}

/**
 * Bildirim metni.
 *
 * Ton davet, korku değil: "SERİN BİTMEK ÜZERE!" değil "bugün beş dakikan var mı". Kullanıcı
 * kitlesi 7–10 yaş; kaygı bu yaşta motivasyondan çok bırakma üretiyor.
 */
function streakReminderText(current, goalMinutes, freezes = 0) {
  const dk = goalMinutes > 0 ? goalMinutes : 5;
  if (current > 0) {
    // Dondurması olan kullanıcının serisi bugün kırılmıyor, ama dondurma 4000 altın ve
    // yarın harcanacak. Bilgi aynı, ton davet: "harcanacak" demek yerine "sakla".
    if (freezes > 0) {
      return {
        title: 'Serin seni bekliyor 🔥',
        body: `Bugün ${dk} dakika çalış, dondurmanı yarına sakla.`,
      };
    }
    return {
      title: 'Serin seni bekliyor 🔥',
      body: `Bugün ${dk} dakika çalış, ${current} günlük serin devam etsin.`,
    };
  }
  return {
    title: 'Bugün başlamaya ne dersin?',
    body: `${dk} dakikalık küçük bir hedefle yeni serini başlat.`,
  };
}

// ─── KIRILMA ÖNCESİ İKİNCİ ŞANS ─────────────────────────────────────────────
//
// NEDEN
//   Akşam hatırlatması günde bir kez gidiyor ve kullanıcının seçtiği saatte (varsayılan
//   19:00). O saatte telefona bakmayan kullanıcı için gün kapanıyor ve seri kırılıyor —
//   kırılmanın ardından geri dönüş oranı düşük, yani önlemek onarmaktan hem ucuz hem etkili.
//
// KİME
//   Yalnızca KAYBEDECEK ŞEYİ OLANA: serisi [SECOND_CHANCE_MIN_STREAK] günü geçmiş,
//   dondurması olmayan, ilk hatırlatmayı bugün almış ve hedefi hâlâ tutturmamış kullanıcı.
//   Bu dört şart olmadan ikinci bildirim, günde iki hatırlatma demek olurdu; 7–10 yaş
//   kitlede bu rahatsızlık üretir ve bildirimlerin tamamen kapatılmasıyla sonuçlanır.

/** İkinci hatırlatmanın yerel saati. Sessiz saatlerin (21:00) bir saat öncesi. */
const SECOND_CHANCE_LOCAL_HOUR = 20;

/** Bu seriden kısa seriler için ikinci hatırlatma gönderilmiyor. */
const SECOND_CHANCE_MIN_STREAK = 3;

/**
 * İkinci hatırlatmanın metni.
 *
 * Birinci hatırlatmadan farklı olmak zorunda: aynı metni ikinci kez göndermek kullanıcıya
 * bir şey söylemiyor, yalnızca tekrar ediyor. Burada kaybın ne olduğu ve çıkışın ne kadar
 * kolay olduğu birlikte söyleniyor.
 */
function secondChanceText(current, goalMinutes) {
  const dk = goalMinutes > 0 ? goalMinutes : 5;
  return {
    title: `${current} günlük serin bugün kırılabilir`,
    body: `${dk} dakikan var mı? Günü kapatmak için yeter.`,
  };
}

/**
 * Bu kullanıcıya ikinci hatırlatma gönderilmeli mi.
 *
 * Saat kontrolü BURADA YOK: tarama kullanıcıları `secondReminderHourUtc` alanına göre
 * seçiyor (bkz. index.js → reminderPatch), yani buraya gelen kullanıcının yerel saati
 * zaten [SECOND_CHANCE_LOCAL_HOUR].
 *
 * Gönderilmeyen durumlar:
 *   • `goal_done`     — bugün hedefini tutturmuş.
 *   • `already_sent`  — bugün ikinci hatırlatma gönderilmiş.
 *   • `short_streak`  — kaybedecek şeyi yok; kısa seri için ikinci bildirim rahatsızlık.
 *   • `has_freeze`    — dondurması var, seri bugün kırılmıyor. Birinci hatırlatma bunu
 *                       zaten söylüyor (bkz. streakReminderText).
 *   • `no_first`      — birinci hatırlatma bugün gitmediyse ikincisi de gitmesin; aksi
 *                       halde tercihi kapalı ya da hedefi tutturmuş kullanıcıya ulaşırdı.
 *   • `idle`          — bir haftadır uygulamayı açmamış.
 */
function secondChanceDecision(state, nowMs) {
  const offset = Number(state.utcOffsetMinutes);
  if (!Number.isFinite(offset)) return { send: false, reason: 'no_offset' };

  const today = localDayId(nowMs, offset);
  if (state.lastDay === today) return { send: false, reason: 'goal_done' };
  if (state.secondReminderSentDay === today) return { send: false, reason: 'already_sent' };
  // Kırılmış seri "bugün kırılabilir" diye hatırlatılmasın (bkz. liveStreak).
  const live = liveStreak(state, today);
  if (!(live.current >= SECOND_CHANCE_MIN_STREAK)) return { send: false, reason: 'short_streak' };
  if (live.freezes > 0) return { send: false, reason: 'has_freeze' };
  if (state.reminderSentDay !== today) return { send: false, reason: 'no_first' };

  if (state.lastSeenMs) {
    const idleDays = (nowMs - state.lastSeenMs) / 86400000;
    if (idleDays > STREAK_REMINDER_MAX_IDLE_DAYS) return { send: false, reason: 'idle' };
  }

  return {
    send: true,
    today,
    text: secondChanceText(live.current, state.goalMinutes),
  };
}

/**
 * Bu kullanıcıya hatırlatma gönderilmeli mi; gönderilecekse metniyle birlikte döner.
 *
 * Gönderilmeyen üç durum ve gerekçeleri:
 *   • `goal_done`     — bugün hedefini zaten tutturmuş, hatırlatmanın konusu kalmamış.
 *   • `already_sent`  — bugün gönderilmiş; günde bir bildirim.
 *   • `idle`          — bir haftadır uygulamayı açmamış. Giden kullanıcıyı dürtmek geri
 *                       getirmiyor, uygulamayı kaldırtıyor.
 */
function streakReminderDecision(state, nowMs) {
  const offset = Number(state.utcOffsetMinutes);
  if (!Number.isFinite(offset)) return { send: false, reason: 'no_offset' };

  const today = localDayId(nowMs, offset);
  if (state.lastDay === today) return { send: false, reason: 'goal_done' };
  if (state.reminderSentDay === today) return { send: false, reason: 'already_sent' };

  // lastSeenAt hiç yoksa gönderiliyor: alan sonradan eklendi, eksikliği "gitmiş kullanıcı"
  // anlamına gelmiyor.
  if (state.lastSeenMs) {
    const idleDays = (nowMs - state.lastSeenMs) / 86400000;
    if (idleDays > STREAK_REMINDER_MAX_IDLE_DAYS) return { send: false, reason: 'idle' };
  }

  // Saklanan `current` kırılmış seride bayat kalıyor; metin ekranda görünen seriyle
  // aynı sayıyı söylemeli (bkz. liveStreak).
  const live = liveStreak(state, today);
  return {
    send: true,
    today,
    text: streakReminderText(live.current, state.goalMinutes, live.freezes),
  };
}

module.exports = {
  STREAK_REMINDER_LOCAL_HOUR,
  STREAK_REMINDER_MAX_IDLE_DAYS,
  SECOND_CHANCE_LOCAL_HOUR,
  SECOND_CHANCE_MIN_STREAK,
  normalizeReminderHour,
  localDayId,
  liveStreak,
  reminderHourUtc,
  streakReminderText,
  streakReminderDecision,
  secondChanceText,
  secondChanceDecision,
};
