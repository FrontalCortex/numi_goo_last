# Firestore Test Mode Bitti – Erişimi Açma

Test Mode 30 gün sonra kapandığı için tüm client istekleri reddediliyor.  
Aşağıdaki **güvenlik kurallarını** Firestore’a yayınlamanız yeterli.

---

## Yöntem 1: Firebase Console (en kolay)

1. **Firebase Console** açın: https://console.firebase.google.com  
2. Projenizi seçin (**numigo-new**).  
3. Sol menüden **Build** → **Firestore Database**’e girin.  
4. Üstte **Rules** sekmesine tıklayın.  
5. Şu anki kuralların tamamını **silin** ve aşağıdaki kuralları **olduğu gibi** yapıştırın.  
6. **Publish** (Yayınla) butonuna basın.

Bitti. Uygulama artık giriş yapmış kullanıcılar için Firestore’a yazıp okuyabilir.

---

## Yöntem 2: Terminal (Firebase CLI)

Proje klasöründe (numi_goo_last) terminal açıp:

```bash
firebase deploy --only firestore:rules
```

(`firebase login` ve `firebase use numigo-new` gerekebilir.)

---

## Kullanılacak kurallar (kopyala-yapıştır)

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    // Kullanıcılar: giriş yapmış kullanıcı sadece kendi dokümanını okuyup yazabilir
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Öğretmen davet kodları
    match /teacherInvites/{code} {
      allow read, write: if request.auth != null;
    }

    // Öğrenci doğrulama kodları
    match /studentVerificationCodes/{code} {
      allow read, write: if request.auth != null;
    }

    // Bekleyen kayıtlar
    match /pendingRegistrations/{docId} {
      allow read, write: if request.auth != null;
    }

    // Diğer tüm path'ler kapalı
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## Ne değişti?

| Önce (Test Mode) | Sonra (bu kurallar) |
|-------------------|----------------------|
| Herkes her şeyi okuyup yazabiliyordu | Sadece **giriş yapmış** kullanıcı erişebilir |
| 30 gün sonra erişim kesildi | Süre sınırı yok, kurallar geçerli olduğu sürece çalışır |
| PERMISSION_DENIED | Giriş yapan kullanıcı kendi verisine erişir, kayıt/giriş çalışır |

Kuralları yayınladıktan sonra uygulamada **kayıt ol** ve **giriş yap** tekrar deneyin.
