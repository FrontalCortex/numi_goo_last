# Custom Token INTERNAL Hatası – IAM İzni Ekleme

`verifyLoginCode` Cloud Function çalışırken **Permission 'iam.serviceAccounts.signBlob' denied** hatası alıyorsanız, aşağıdaki izni eklemeniz gerekir.

## Neden?

Custom token üretmek (`admin.auth().createCustomToken(uid)`) için çalışan service account’un **Service Account Token Creator** rolüne sahip olması gerekir. Cloud Functions (1st gen) varsayılan olarak **App Engine default service account** kullanır.

## Adımlar (Google Cloud Console)

1. Tarayıcıda açın:  
   **https://console.cloud.google.com/iam-admin/iam?project=numigo-new**

2. Sayfada **Service account’lar** listesinde şunu bulun:  
   **numigo-new@appspot.gserviceaccount.com**  
   (App Engine default service account – “Editor” veya benzeri rolü olabilir.)

3. Bu satırın en sağındaki **kalem (düzenle)** ikonuna tıklayın.

4. **“Başka rol ekle”** / **“Add another role”** deyin.

5. Rol arama kutusuna yazın: **Service Account Token Creator**

6. **Service Account Token Creator** rolünü seçin.

7. **Kaydet** / **Save** deyin.

Birkaç dakika içinde izin yayılır. Sonra uygulamada tekrar OTP ile giriş deneyin; INTERNAL hatası düzelmiş olmalı.

## Alternatif: gcloud (kuruluysa)

```bash
gcloud iam service-accounts add-iam-policy-binding numigo-new@appspot.gserviceaccount.com \
  --member="serviceAccount:numigo-new@appspot.gserviceaccount.com" \
  --role="roles/iam.serviceAccountTokenCreator" \
  --project=numigo-new
```

## Kontrol

- Firebase Console → Functions → `verifyLoginCode` fonksiyonunu açıp son çağrıları / logları kontrol edebilirsiniz.
- Uygulamada kayıtlı e-posta ile OTP girişi yapıp tekrar deneyin.
