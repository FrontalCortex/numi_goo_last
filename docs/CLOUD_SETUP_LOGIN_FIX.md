# Google Giriş (Login) Hatası – Cloud Tarafında Yapılacaklar

"PERMISSION_DENIED" veya giriş hatası alıyorsanız ve test süresi bitmiş / ön ödeme iade edilmişse aşağıdakileri sırayla yapın.

---

## 1. OAuth Consent Screen (Test Modu / Yayın)

Google ile giriş **OAuth Consent Screen** ayarlarına bağlıdır. Test süresi bitince sadece “test kullanıcıları” giriş yapabilir; sizin hesabınız listede değilse hata alırsınız.

### Adımlar

1. **Google Cloud Console** açın: https://console.cloud.google.com  
2. Üstten **projenizi** seçin (Firebase ile aynı proje).  
3. Sol menü: **APIs & Services** → **OAuth consent screen**.  
4. **Publishing status** kısmına bakın:
   - **"Testing"** yazıyorsa:
     - **Test users** bölümüne gidin.
     - **+ ADD USERS** ile **giriş yapmak istediğiniz Gmail adresini** ekleyin.
     - Kaydedin.  
     - Bu sayede test modunda bile sadece bu hesapla giriş yapabilirsiniz.
   - **Production’a almak istiyorsanız** (herkes giriş yapsın):
     - **PUBLISH APP** ile uygulamayı “In production” yapın.
     - İlk seferde “Verification” istenebilir; sadece kendi kullanımınız için “Internal” veya “Testing” + test kullanıcı ekleyerek de devam edebilirsiniz.

**Özet:** Test süresi bitti diyorsanız önce **Test users**’a kendi e-postanızı ekleyin; çoğu “izin hatası” bununla çözülür.

---

## 2. Firebase Authentication – Google Açık mı?

1. **Firebase Console**: https://console.firebase.google.com  
2. Projenizi seçin.  
3. Sol menü: **Build** → **Authentication** → **Sign-in method**.  
4. **Google** satırına tıklayın.  
5. **Enable** açık olmalı.  
6. **Web SDK configuration** kısmındaki **Web client ID** ve **Web client secret** dolu olmalı (genelde otomatik).  
7. **Save** deyin.

Bunlar kapalıysa veya yanlışsa giriş reddedilir.

---

## 3. Firestore Kurallarını Güncellemek / Yayınlamak

"Missing or insufficient permissions" bazen **Firestore** kurallarından gelir. Kurallar projede güncellendi ama Cloud’a deploy edilmediyse hata devam eder.

### Seçenek A: Firebase CLI ile

Proje kökünde (terminal):

```bash
firebase deploy --only firestore:rules
```

### Seçenek B: Firebase Console ile

1. **Firebase Console** → projeniz → **Firestore Database**.  
2. **Rules** sekmesi.  
3. Aşağıdaki kuralları yapıştırıp **Publish** edin:

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /teacherInvites/{code} {
      allow read, write: if request.auth != null;
    }
    match /studentVerificationCodes/{code} {
      allow read, write: if request.auth != null;
    }
    match /pendingRegistrations/{docId} {
      allow read, write: if request.auth != null;
    }
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

Böylece giriş yapan kullanıcı kendi `users/{uid}` dokümanına erişebilir; PERMISSION_DENIED bu path için azalır.

---

## 4. Ön Ödeme / Billing (500 TL iadesi)

- **Firebase Auth** ve **Firestore** belirli kotaya kadar **ücretsiz** çalışır.  
- Ön ödeme iade edilmiş olsa bile, **Authentication** ve **Firestore (free tier)** normalde çalışmaya devam eder.  
- Eğer proje **Blaze (pay as you go)** plana geçirilmiş ve sonra billing kapatıldıysa proje tekrar **Spark** plana döner; yine de giriş ve temel Firestore çalışır.

**Yapmanız gereken:**  
- Sadece giriş ve ders verileri için: **Billing eklemeden** önce 1–2–3’ü yapın.  
- İleride kota / ek servis isterseniz: **Google Cloud Console** → **Billing** → projeye tekrar ödeme yöntemi ekleyip Blaze’e geçebilirsiniz.

---

## 5. Kontrol Listesi (Sırayla)

| # | Ne yapılacak | Nerede |
|---|----------------|--------|
| 1 | OAuth Consent Screen → **Test users**’a kendi Gmail’inizi ekleyin | Google Cloud Console → APIs & Services → OAuth consent screen |
| 2 | Google Sign-in **Enabled** olsun | Firebase Console → Authentication → Sign-in method |
| 3 | Firestore kurallarını yukarıdaki gibi yapıp **Publish** edin | Firebase Console → Firestore → Rules veya `firebase deploy --only firestore:rules` |
| 4 | Uygulamada **giriş yap** tekrar deneyin | Telefon/emülatör |

Önce **1** ve **2**’yi yapın; hata sürerse **3**’ü mutlaka kontrol edin. Ön ödeme iadesi için ekstra bir şey yapmanız gerekmez; test kullanıcısı + Firestore kuralları çoğu zaman yeterli olur.
