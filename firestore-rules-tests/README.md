# firestore.rules regresyon testleri

`../firestore.rules` dosyasini Firestore emulatorunde calistirip izinleri dogrular.

## Neden var

Ders basari oranlari, edinim kaynagi sayaci ve anket sik sayaclari Firestore'dan Firebase
Analytics'e tasindi; guvenlik kurallari da buna gore daraltildi. Kurallar sessizce bozulan
turden bir seydir: fazla acik birakilirsa kimse fark etmez, fazla kapatilirsa hata yalnizca
uretimde bir kullanicinin ekraninda gorunur. Bu testler iki yonu birden tutar:

- **Kapatilanlar** gercekten kapali mi (`successRate/lessonSuccessRate/**`, `appStatistics`,
  anket sik sayaclari).
- **Korunanlar** hala calisiyor mu (gunluk soru basari orani, anket serbest metni,
  `lessonSuccessRateState` cift sayim korumasi).

## Calistirma

```bash
cd firestore-rules-tests
npm install      # yalnizca ilk kez
npm test
```

Emulator jar'i ilk calistirmada indirilir, internet gerekir. Java 11+ kurulu olmalidir.

Kurallar `firebase.json` uzerinden degil, `rules.test.mjs` icindeki
`readFileSync('../firestore.rules')` ile yuklenir — tek kaynak repo kokundeki
`firestore.rules` dosyasidir.

## Yeni kural eklerken

`firestore.rules` dosyasina her dokunusta buraya da bir vaka ekleyin: hem izin verilen
hem reddedilen bir ornek. Deploy oncesi `npm test` yesil olmalidir.
