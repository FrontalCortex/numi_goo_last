import SwiftUI

// Alt gezinme çubuğunun sekmeleri. Sıra Android res/menu/bottom_menu.xml ile aynıdır;
// hangi ekranın açıldığı MainActivity.setupClickListeners'taki eşlemeyle aynıdır.
enum Sekme: Int, CaseIterable, Identifiable {
    case harita      // R.id.map -> PartSelectionFragment / MapFragment
    case profil      // R.id.profile -> ProfileFragment
    case gorevler    // R.id.tasks -> TasksFragment(MODE_MISSIONS): görevler + günlük soru + Abaküs kartı
    case magaza      // R.id.shop -> AbacusCustomizationFragment
    case bildirim    // R.id.notification -> öğrencide SobiChatFragment, öğretmende TeacherClassesFragment
    case kesfet      // R.id.explore -> TasksFragment(MODE_CUP): yalnızca Kupa Yolu kartı

    var id: Int { rawValue }

    /// Asset catalog'daki görsel adı (Android drawable adıyla aynı).
    /// Android: profil sekmesinin ikonu kullanıcının avatarı (refreshProfileNavIcon), öğrencide
    /// sohbet sekmesinin ikonu Sobi'nin kafası (SobiHeadView); ikisi de kodla çizildiği için
    /// burada menüdeki varsayılan görseller kullanılır. Öğretmende sohbet sekmesi `class_ic`.
    func simgeAdi(ogretmen: Bool) -> String {
        switch self {
        case .harita: return "home_ic1"
        case .profil: return "profile_ic2"
        case .gorevler: return "missions_ic3"
        case .magaza: return "shop_ic1"
        case .bildirim: return ogretmen ? "class_ic" : "chat_pair_ic"
        case .kesfet: return "explore_ic1"
        }
    }

    /// Android menüsünde sekme başlığı yoktur (yalnızca simge); bu adlar yer tutucu ekranda
    /// ve erişilebilirlik etiketinde kullanılır.
    func ad(ogretmen: Bool) -> String {
        switch self {
        case .harita: return Metin.al("ios_sekme_harita")
        case .profil: return Metin.al("ios_sekme_profil")
        case .gorevler: return Metin.al("ios_sekme_gorevler")
        case .magaza: return Metin.al("ios_sekme_abakus")
        // Android: öğrencide contentDescription sobi_tab_title, öğretmende class_list_title.
        case .bildirim: return ogretmen ? Metin.al("class_list_title") : Metin.al("sobi_tab_title")
        case .kesfet: return Metin.al("ui_kupa_yolu")
        }
    }

    /// Üst çubuk (cüzdan/enerji paneli) bu sekmede görünür mü.
    /// Android: MainActivity.updateCurrencyPanelVisibility - yalnızca MapFragment,
    /// PartSelectionFragment ve TasksFragment (iki modu da) açıkken görünür.
    var ustCubukGorunur: Bool {
        switch self {
        case .harita, .gorevler, .kesfet: return true
        case .profil, .magaza, .bildirim: return false
        }
    }
}
