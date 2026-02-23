# Eğitmen–Öğrenci Görüşme Sistemi – Mimari Öneri

Bu dokümanda, NumiGoo uygulamasına eklenecek **eğitmen–öğrenci canlı görüşmesi** (ses + ekran görüntüsü + öğretmen çizimi) için önerilen yapı anlatılmaktadır.

---

## 1. Genel Akış Özeti

1. **Öğrenci** uygulama içinde “Öğretmene danış” benzeri bir butona tıklar.
2. Sistem, Firestore’da **müsait öğretmenleri** bulur ve birine **görüşme teklifi** oluşturur (veya sıradaki öğretmene atar).
3. **Öğretmen** teklifi görür; kabul/red eder.
4. Kabul edilirse **görüşme başlar**:
   - **Ses:** Öğretmen ↔ öğrenci sesli konuşma (WebRTC ses).
   - **Ekran:** Öğrenci ekranı öğretmene gider (ekran paylaşımı).
   - **Çizim:** Öğretmen kendi ekranında (öğrencinin ekran görüntüsü üzerinde) kalemle çizer; çizim verisi öğrenciye gider ve öğrenci ekranında **overlay** olarak gösterilir.

Öneri: **Tıklama/etkileşim** (öğretmenin öğrenci uygulamasında doğrudan tıklaması) başta **yapılmamalı**; sadece **çizim overlay** ile başlamak daha güvenli ve uygulanabilir.

---

## 2. Bileşenler ve Teknolojiler

| Bileşen | Önerilen çözüm | Alternatif / not |
|--------|----------------|-------------------|
| **Signaling** (teklif, kabul, WebRTC SDP/ICE) | Firestore (realtime listener) | Realtime Database de kullanılabilir |
| **Ses (voice)** | WebRTC (PeerConnection, audio only veya audio+video) | Agora, Twilio gibi 3. parti de kullanılabilir |
| **Öğrenci ekranı → öğretmen** | WebRTC ekran capture (MediaProjection) | Aynı WebRTC bağlantısında video track |
| **Öğretmen çizimi → öğrenci** | WebRTC DataChannel veya Firestore | DataChannel düşük gecikme için daha uygun |
| **Öğretmen “müsait” durumu** | Firestore `users/{uid}` veya `teachers/availability` | Realtime güncelleme için listener |

---

## 3. Veri Modeli (Firestore)

### 3.1 Öğretmen müsaitliği

**Seçenek A – `users` içinde alan:**

```
users / {userId}
  role: "teacher" | "student"
  isAvailable: boolean      // öğretmen müsait mi
  lastSeen: Timestamp       // opsiyonel
```

**Seçenek B – Ayrı koleksiyon:**

```
teacherAvailability / {teacherId}
  available: boolean
  updatedAt: Timestamp
```

Öğretmen uygulama açıkken “Müsaitim” toggle’ı ile `isAvailable: true`, görüşme kabul edince veya uygulama kapanınca `false` yapılır.

### 3.2 Görüşme teklifi

```
meetingRequests / {requestId}
  studentId: string
  studentName: string       // gösterim için
  teacherId: string | null  // atanınca doldurulur (veya boş bırakıp “ilk müsait”e gidebilir)
  status: "pending" | "accepted" | "rejected" | "cancelled" | "completed"
  createdAt: Timestamp
  respondedAt: Timestamp | null
  lessonId: string | null   // hangi ders/ekranda takıldığı (opsiyonel)
```

- Öğrenci butona basınca `status: "pending"` ile doküman oluşturulur.
- Backend (Cloud Function) veya client tarafı “müsait öğretmen”e bu teklifi atayabilir (`teacherId` set edilir).
- Öğretmen kabul ederse `status: "accepted"`, reddederse `"rejected"` yapılır.

### 3.3 WebRTC signaling (aynı görüşme için)

WebRTC için SDP ve ICE alışverişi Firestore’da yapılabilir:

```
meetingSignals / {meetingId}   // meetingId = accepted requestId veya yeni ID
  offer: string | null         // SDP offer
  answer: string | null       // SDP answer
  iceCandidates: array        // veya subcollection iceCandidates/{id}
  createdBy: "student" | "teacher"
  updatedAt: Timestamp
```

Her iki taraf bu dokümana listener koyar; offer/answer/ICE güncellendikçe WebRTC bağlantısı tamamlanır.

---

## 4. Ses ve Ekran Paylaşımı (WebRTC)

- **Tek PeerConnection** kullanılabilir:
  - 1 audio track (mikrofon).
  - 1 video track: öğrenci tarafında **ekran capture** (MediaProjection), öğretmen tarafında isteğe bağlı kameranız (opsiyonel).
- **Signaling:** Firestore’daki `meetingSignals/{meetingId}` ile SDP/ICE exchange.
- **STUN/TURN:** Google STUN ile başlanır; mobil ağlarda TURN sunucusu (coturn vb.) eklemek bağlantı başarısını artırır.

Öğrenci ekranı öğretmene gider; öğretmen ekranında bu video bir `SurfaceView` / `TextureView` veya ExoPlayer ile gösterilir. Üzerine çizim katmanı (Canvas) eklenir (aşağıda).

---

## 5. Çizim Sistemi (Sadece Overlay – Önerilen)

Hedef: Öğretmen öğrencinin ekran görüntüsünü görür, kendi ekranında kalemle çizer; **sadece çizim verisi** öğrenciye gider, öğrenci kendi ekranında bu çizimleri overlay olarak çizer. Öğrenci uygulamasında **tıklama/remote control yok**.

### 5.1 Veri formatı (çizim komutları)

Her çizim “stroke” (tek kalem hareketi) olarak gönderilebilir:

```json
{
  "type": "stroke",
  "points": [ {"x": 0.25, "y": 0.4}, {"x": 0.26, "y": 0.41}, ... ],
  "color": "#FF0000",
  "strokeWidth": 4,
  "timestamp": 1234567890
}
```

`x`, `y` **normalize (0–1)** olursa farklı çözünürlüklerde de oran korunur. Öğretmen ekranı ile öğrenci ekranı farklı boyutta olsa bile çizim orantılı görünür.

İsteğe eklenebilir:

- `"type": "clear"` → Öğrenci tarafta overlay temizlenir.
- `"type": "undo"` → Son stroke silinir (her iki tarafta stroke listesi tutulur).

### 5.2 İletim yöntemleri

| Yöntem | Artı | Eksi |
|--------|------|------|
| **WebRTC DataChannel** | Düşük gecikme, P2P, sürekli bağlantı | WebRTC zaten kullanılıyorsa ek iş yok |
| **Firestore** | Zaten kullanıyorsunuz, kolay | Gecikme ve maliyet (her stroke = yazma) |
| **Realtime Database** | Realtime, tek bağlantı | Yeni bağımlılık, yapı tasarımı |

**Öneri:** Ses ve ekran için WebRTC kullanıyorsanız, çizim için de **DataChannel** kullanın. Tek DataChannel’da JSON mesajları (yukarıdaki stroke formatında) gönderilir.

### 5.3 Öğretmen uygulaması (çizim UI)

- Öğrencinin ekranı bir view’da gösterilir (WebRTC remote video).
- Üzerine şeffaf bir **çizim view** (custom View veya Canvas) konur.
- Öğretmen parmakla/stylus ile çizer; her hareket `MotionEvent` ile alınır, koordinatlar normalize edilip (ör. view genişliği/yüksekliğine göre 0–1) stroke olarak toplanır ve DataChannel (veya seçilen kanal) ile gönderilir.
- İsteğe bağlı: renk seçici, kalem kalınlığı, “Temizle” butonu.

### 5.4 Öğrenci uygulaması (overlay)

- Normal uygulama ekranı (Abacus vb.) çalışmaya devam eder.
- Üstte şeffaf bir **overlay view** (full screen, touch’u geçirir; sadece çizim çizer) olur.
- DataChannel’dan (veya Firestore/Realtime’dan) gelen stroke’lar bu overlay’de çizilir. Koordinatlar 0–1 ise, overlay’in width/height ile çarpılarak çizilir.
- Görüşme bitince overlay kaldırılır veya “Çizimleri temizle” ile temizlenir.

Bu sayede öğretmen **öğrenci ekranını görür**, **sadece çizimle** anlatır; öğrenci cihazında uzaktan tıklama olmaz, güvenlik ve uygulama karmaşıklığı azalır.

---

## 6. Öğretmenin Öğrenci Ekranında Tıklaması (İsteğe Bağlı – İleri Aşama)

İleride “öğretmen tıklayınca öğrenci ekranında da tıklanmış gibi olsun” derseniz:

- Öğretmen ekranında tıklanan **normalize (x,y)** koordinatı öğrenciye gönderilir.
- Öğrenci tarafta bu koordinata **programatik tıklama** (dispatch touch event) yapılır. Bu:
  - Güvenlik: Sadece görüşme sırasında ve sadece overlay/izin verilen alanlarda yapılmalı.
  - Teknik: Doğru view’ı bulup `dispatchTouchEvent` ile event üretmek zor; farklı cihaz boyutları ve layout’larda hata riski var.

Bu yüzden **ilk sürümde sadece çizim overlay** önerilir; tıklama özelliği v2’de düşünülebilir.

---

## 7. Uygulama Adımları (Özet)

1. **Firestore**
   - `users` (veya ayrı koleksiyon) içinde öğretmen `isAvailable` alanı.
   - `meetingRequests` ve gerekirse `meetingSignals` koleksiyonları.
   - Güvenlik kuralları: sadece ilgili öğrenci/öğretmen okuyabilsin, yazabilsin.

2. **Öğrenci client**
   - “Öğretmene danış” butonu → `meetingRequests`’e yeni doküman.
   - `meetingRequests/{id}` ve `meetingSignals/{meetingId}` listener.
   - Kabul gelince: WebRTC başlat (mikrofon + ekran capture), SDP/ICE’i Firestore’a yaz.
   - Çizim DataChannel’ı dinle → overlay view’da stroke’ları çiz.

3. **Öğretmen client**
   - Müsaitlik toggle → `isAvailable` güncelle.
   - `meetingRequests` (kendine atanmış veya pending) listener → teklif listesi/kabul ekranı.
   - Kabul edince: WebRTC başlat (mikrofon, remote video = öğrenci ekranı), SDP/ICE’i Firestore’a yaz.
   - Öğrenci ekranı view’ı üzerine çizim katmanı; çizimleri DataChannel ile gönder.

4. **Backend (opsiyonel)**
   - Cloud Function: Yeni `meetingRequest` oluşunca müsait bir öğretmen bulup `teacherId` atayabilir; böylece “ilk müsait öğretmene git” mantığı merkezi olur.

5. **WebRTC kütüphanesi**
   - Android için `org.webrtc:google-webrtc` veya daha yüksek seviye bir wrapper (ör. İleride Agora/Twilio SDK) kullanılabilir.

---

## 8. Sorulabilecek Ek Kararlar

- **Öğretmen/öğrenci aynı uygulama mı?** (Tek APK, role’e göre farklı ekranlar mı?) → Evet gibi görünüyor; aynı projede iki farklı “call UI” (öğrenci: ekran paylaş + overlay, öğretmen: remote ekran + çizim) yeterli.
- **Görüşme süresi sınırı var mı?** (Abonelikte “ayda X danışma” var; süre sınırı da olacak mı?)
- **TURN sunucusu:** Şimdilik sadece STUN ile deneyip, bağlantı sorunlarında TURN (coturn + Firebase/Socket sunucu) eklenebilir.
- **Kayıt:** Görüşme kaydı (ses/ekran) istenmiyorsa WebRTC’de kayıt eklenmez; ileride istenirse sunucu tarafında ayrı bir servis gerekir.

Bu yapı ile önce **ses + ekran paylaşımı + sadece çizim overlay** ile güvenli ve sade bir eğitmen–öğrenci görüşmesi kurgulanabilir; tıklama/remote control sonra eklenebilir.
