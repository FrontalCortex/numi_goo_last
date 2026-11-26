import Foundation
import SwiftUI

// Derse dokununca alttan açılan panel (Android: lesson_bottom_sheet.xml +
// LessonAdapter.showLessonBottomSheet). Metinler ve düğme durumu ortak koddan
// (LessonAdapterLogic.lessonBottomSheetState) gelir; burada yalnızca yerleşim ve renkler var.

struct HaritaDersAltPaneli: View {
    let durum: HaritaAltPanelDurumu
    @ObservedObject var model: HaritaModeli

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ustSatir
            ortaSatir
                .padding(.bottom, 14)
            eylemDugmesi
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(
            HaritaUstYuvarlakSekil(yaricap: 16)
                .fill(zemin)
                .ignoresSafeArea(edges: .bottom)
        )
        .gesture(asagiCekme)
    }

    // MARK: Renkler (LessonAdapter.showLessonBottomSheet)

    /// Kilitli: background_color; başlamış ama bitmemiş: panel_background; diğerleri: #222.
    private var zemin: Color {
        if durum.kilitli {
            return Color("background_color")
        }
        if durum.basladi {
            return Color("panel_background")
        }
        return HaritaStil.altPanelZemini
    }

    private var yaziRengi: Color {
        return durum.kilitli ? Color("lesson_locked") : Color("lesson_completed")
    }

    private var dugmeZemini: Color {
        return durum.kilitli ? Color("circleBackground_color") : Color("lesson_completed")
    }

    private var dugmeYaziRengi: Color {
        if durum.kilitli {
            return Color("lesson_locked")
        }
        if durum.basladi {
            return Color("panel_background")
        }
        return Color("background_color")
    }

    // MARK: Üst satır: başlık + rekor

    private var ustSatir: some View {
        HStack(spacing: 6) {
            Text(durum.baslik)
                .font(HaritaStil.yazi(21))
                .foregroundColor(yaziRengi)
                .lineLimit(1)
                .padding(.top, 4)
                .padding(.bottom, 6)
                .padding(.leading, 8)
            Spacer(minLength: 0)
            rekor
        }
        .frame(height: 38)
    }

    /// recordLayout: yalnızca bitmiş sandıkta ve rekor satırı gizli değilse görünür.
    @ViewBuilder
    private var rekor: some View {
        if let metin = durum.rekorMetni, !durum.rekorGizli {
            Button(action: { model.rekoraDokun(durum) }) {
                // Android'de metnin solunda ateş Lottie'si (fire_anim.json) oynar; lottie-ios bağlanınca eklenecek.
                Text(metin)
                    .font(HaritaStil.yazi(14))
                    .foregroundColor(Color("background_color"))
                    .lineLimit(1)
                    .padding(.leading, 10)
                    .padding(.trailing, 10)
                    .frame(height: 30)
                    .background(RoundedRectangle(cornerRadius: 12).fill(HaritaStil.rekorZemini))
            }
            .buttonStyle(.plain)
            .padding(.trailing, 8)
        }
    }

    // MARK: Orta satır: açıklama + "Eğitimi tekrarla"

    private var ortaSatir: some View {
        HStack(alignment: .top, spacing: 0) {
            Text(durum.aciklama)
                .font(HaritaStil.yazi(17))
                .foregroundColor(yaziRengi)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 8)
            egitimTekrar
        }
    }

    /// againTutorial: görünmezken de yer kaplar (Android: INVISIBLE).
    private var egitimTekrar: some View {
        Button(action: { model.egitimiTekrarla(durum) }) {
            Text("Eğitimi tekrarla")
                .font(HaritaStil.yazi(16))
                .foregroundColor(Color("background_color"))
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 8).fill(Color("lesson_completed")))
        }
        .buttonStyle(.plain)
        .padding(.trailing, 8)
        .opacity(durum.egitimTekrarGorunur ? 1.0 : 0.0)
        .disabled(!durum.egitimTekrarGorunur)
    }

    // MARK: Eylem düğmesi

    private var eylemDugmesi: some View {
        Button(action: { model.eylemDugmesi(durum) }) {
            // Android: textAllCaps=true (Türkçe büyük harf kuralıyla: i → İ).
            Text(durum.dugmeMetni.uppercased(with: Locale(identifier: "tr_TR")))
                .font(HaritaStil.yazi(14))
                .foregroundColor(dugmeYaziRengi)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 40)
                .background(RoundedRectangle(cornerRadius: 4).fill(dugmeZemini))
        }
        .buttonStyle(.plain)
        .disabled(!durum.dugmeEtkin)
        .padding(.horizontal, 8)
    }

    // MARK: Kapatma hareketi

    /// Paneli aşağı çekince kapat (Android: BottomSheetBehavior STATE_HIDDEN).
    private var asagiCekme: some Gesture {
        DragGesture(minimumDistance: 20)
            .onEnded { deger in
                if deger.translation.height > 60 {
                    model.altPaneliKapat()
                }
            }
    }
}
