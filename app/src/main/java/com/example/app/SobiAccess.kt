package com.example.app

import android.content.Context
import com.example.app.auth.AuthManager

/**
 * Sobi'nin açılıp açılamayacağı. Öğretmen hesabında Sobi yok (kullanıcı kararı 19.11.2026): öğretmen
 * derslere kursta göstermek için giriyor, Sobi'ye soru sormuyor, Sobi'nin günlük testini çözmüyor.
 *
 * Kapıdan geçenler: öğreticideki "Sobi'ye sor" düğmeleri ve dürtme balonları
 * ([TutorialFragment.isSobiChatAvailable]), derslerin yanlış panelindeki "Sobi ile analiz et"
 * ([AbacusFragment], [BlindingLessonFragment]) ve haritadaki Sobi testi düğmesi ([MapSobiTestButton]).
 * Sobi sekmesi öğretmende zaten yok ([MainActivity.applySobiTab]); ödev oynatıcısındaki Sobi düğmesi
 * yalnızca Sobi testinde çıkıyor, test de harita düğmesinden açılıyor.
 *
 * Rol cihazdaki son bilinen rolden okunuyor (giriş ve kullanıcı belgesi okunduğunda yazılıyor).
 */
object SobiAccess {
    fun isAllowed(context: Context): Boolean =
        AuthManager().also { it.initialize(context) }.getCurrentUserType() != AuthManager.ROLE_TEACHER
}
