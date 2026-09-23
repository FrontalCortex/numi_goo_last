/**
 * Akşam seri hatırlatmasının KARAR mantığı.
 *
 * Firestore'a ve FCM'e dokunan tarama index.js'te (`sendStreakReminders`); burada yalnızca
 * "bu kullanıcıya bugün hatırlatma gider mi, giderse ne yazar" sorusu var.
 *
 * Ayrı modül olmasının sebebi test: kararın tamamı yan etkisiz olduğu için emülatör
 * kurmadan, saniyeler içinde çalıştırılabiliyor (bkz. scripts/test-streak-reminder.js).
 */

/** Kullanıcının yerel saatiyle hatırlatmanın gönderileceği saat. */
const STREAK_REMINDER_LOCAL_HOUR = 19;

/** Kaç gündür görülmeyene hatırlatma gönderilmesin. */
const STREAK_REMINDER_MAX_IDLE_DAYS = 7;

/** Kullanıcının yerel takvim günü (`yyyy-MM-dd`), saat dilimi farkından. */
function localDayId(nowMs, utcOffsetMinutes) {
  return new Date(nowMs + utcOffsetMinutes * 60000).toISOString().slice(0, 10);
}

/**
 * Kullanıcının yerel saatiyle [STREAK_REMINDER_LOCAL_HOUR]'a denk gelen UTC saati.
 *
 * Saatlik tarama "şu anda yerel saati akşam olanlar" sorgusunu bu alan üzerinden, indeksli
 * tek bir eşitlikle yapıyor — tüm kullanıcıları taramak yerine.
 */
function reminderHourUtc(utcOffsetMinutes) {
  const localMinutes = STREAK_REMINDER_LOCAL_HOUR * 60;
  const utcMinutes = (((localMinutes - utcOffsetMinutes) % 1440) + 1440) % 1440;
  return Math.floor(utcMinutes / 60);
}

/**
 * Bildirim metni.
 *
 * Ton davet, korku değil: "SERİN BİTMEK ÜZERE!" değil "bugün beş dakikan var mı". Kullanıcı
 * kitlesi 7–10 yaş; kaygı bu yaşta motivasyondan çok bırakma üretiyor.
 */
function streakReminderText(current, goalMinutes) {
  const dk = goalMinutes > 0 ? goalMinutes : 5;
  if (current > 0) {
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

  return {
    send: true,
    today,
    text: streakReminderText(state.current, state.goalMinutes),
  };
}

module.exports = {
  STREAK_REMINDER_LOCAL_HOUR,
  STREAK_REMINDER_MAX_IDLE_DAYS,
  localDayId,
  reminderHourUtc,
  streakReminderText,
  streakReminderDecision,
};
