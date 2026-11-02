'use strict';

/**
 * sobiSessions callable — Sobi sekmesindeki genel sohbetlerin listesi (kullanıcı kararı 02.11.2026).
 * Sohbetler ders sohbetleriyle aynı yerde (sobiChats/{uid}/sessions), `kind: 'genel'` alanıyla
 * ayrılıyor (callable.js yazıyor); ders sohbetleri bu listede görünmez, buradan silinemez.
 *
 *   { action: 'list' }                → { sessions: [{ id, title, updatedAt (ms), turns }] } (yeniden eskiye)
 *   { action: 'get', sessionId }      → { id, title, messages: [{ from, text, index, rating, reported, buttons }] }
 *   { action: 'delete', sessionId }   → { deleted: true }
 *
 * Liste yalnızca eşitlikle süzülüyor, sıralama burada: `where kind == 'genel'` + `orderBy updatedAt`
 * bileşik dizin isterdi (firestore.indexes.json, ayrı deploy). Sohbetler 30 günde siliniyor ve
 * bir sohbet en çok 20 mesaj, yani liste kısa kalıyor. `select` modelin geçmişini (historyJson,
 * en büyük alan) getirmiyor.
 *
 * Silinen sohbetin beğeni/bildirim kayıtları (sobiFeedback, sobiReports) kalır: kendi süreleriyle
 * (30 / 90 gün) siliniyorlar ve bildirilen bir cevabın incelemesi sohbet silinince kaybolmamalı.
 */

const SESSION_ID = /^[A-Za-z0-9_-]{8,64}$/;
const MAX_LIST = 50;

const toMs = (t) => (t && typeof t.toMillis === 'function' ? t.toMillis() : (t && Number.isFinite(t.ms) ? t.ms : 0));

module.exports = function createSobiSessions({ functions, db }) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    const uid = context.auth.uid;
    const p = data || {};
    const sessions = db.collection('sobiChats').doc(uid).collection('sessions');

    if (p.action === 'list') {
      const snap = await sessions.where('kind', '==', 'genel').select('title', 'updatedAt', 'turns').get();
      const list = snap.docs.map((d) => ({
        id: d.id,
        title: typeof d.get('title') === 'string' ? d.get('title') : '',
        updatedAt: toMs(d.get('updatedAt')),
        turns: Number(d.get('turns')) || 0,
      }));
      list.sort((a, b) => b.updatedAt - a.updatedAt);
      return { sessions: list.slice(0, MAX_LIST) };
    }

    const sessionId = SESSION_ID.test(p.sessionId || '') ? p.sessionId : null;
    if (!sessionId || (p.action !== 'get' && p.action !== 'delete')) {
      throw new functions.https.HttpsError('invalid-argument', 'Geçersiz istek.');
    }
    const ref = sessions.doc(sessionId);
    const snap = await ref.get();
    // Ders sohbeti de "yok" sayılır: bu ekran yalnızca genel sohbetleri gösterir ve siler.
    if (!snap.exists || snap.get('kind') !== 'genel') {
      throw new functions.https.HttpsError('not-found', 'Sohbet bulunamadı.');
    }
    if (p.action === 'delete') {
      await ref.delete();
      return { deleted: true };
    }
    const transcript = Array.isArray(snap.get('transcript')) ? snap.get('transcript') : [];
    return {
      id: sessionId,
      title: snap.get('title') || '',
      turns: Number(snap.get('turns')) || 0,
      // index: sohbet kaydındaki sıra; beğen / beğenme buna bağlanıyor (sobiFeedback).
      messages: transcript.map((m, index) => ({
        from: m.from === 'sobi' ? 'sobi' : 'cocuk',
        text: typeof m.text === 'string' ? m.text : '',
        index,
        rating: m.rating === 1 || m.rating === -1 ? m.rating : 0,
        reported: m.reported === true,
        buttons: Array.isArray(m.buttons) ? m.buttons : [],
      })),
    };
  });
};

module.exports.MAX_LIST = MAX_LIST;
