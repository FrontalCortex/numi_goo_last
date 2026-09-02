package com.example.app.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.app.AppStatisticsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.example.app.UserWalletFirestore

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_TEACHER = "TEACHER"
        private const val PREFS_NAME = "auth_prefs"

        /** OTP gönderim amacı — hedef uid'i sunucu bu bilgiye göre kendisi çözer. */
        const val OTP_PURPOSE_REGISTER = "register"
        const val OTP_PURPOSE_LOGIN = "login"
        const val OTP_PURPOSE_TEACHER_RESET = "teacher_reset"

        /**
         * Kayıt sırasında girilen ad/şifre/rol bilgisini, kod doğrulama adımına kadar
         * SADECE BELLEKTE taşır.
         *
         * Eskiden bu bilgiler `pendingRegistrations/{email}` dokümanına düz metin şifreyle
         * yazılıyordu ve o koleksiyona kimliksiz yazma açıktı. Artık hiçbir yere yazılmıyor;
         * doğrulama adımında `verifyRegistrationCode` çağrısının gövdesinde gidiyor.
         * Process ölürse kayıt yarım kalır — kullanıcı yeni kod isteyip baştan girer.
         */
        private val pendingRegistrations = mutableMapOf<String, PendingRegistration>()

        fun rememberPendingRegistration(
            email: String,
            name: String,
            password: String,
            role: String,
        ) {
            pendingRegistrations[email.trim().lowercase()] =
                PendingRegistration(name = name, password = password, role = role)
        }

        private fun consumePendingRegistration(email: String): PendingRegistration? =
            pendingRegistrations.remove(email.trim().lowercase())
    }

    private data class PendingRegistration(
        val name: String,
        val password: String,
        val role: String,
    )
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Google Sign-In yapılandırması
        val webClientId = context.getString(com.example.app.R.string.default_web_client_id)
        
        // Web Client ID kontrolü
        if (webClientId.isEmpty() || webClientId == "YOUR_WEB_CLIENT_ID_HERE") {
            android.util.Log.e(
                "AuthManager",
                "Web Client ID ayarlanmamış! Firebase Console'dan Web Client ID'yi alıp strings.xml'e ekleyin."
            )
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    /** Öğretmen şifre sıfırlama: Firebase Auth ile e-posta gönderir. */
    fun sendPasswordResetEmail(email: String, callback: (Boolean, String?) -> Unit) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            callback(false, "E-posta adresi girin")
            return
        }
        auth.sendPasswordResetEmail(trimmed)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e ->
                val msg = e.localizedMessage ?: e.message ?: "E-posta gönderilemedi"
                callback(false, msg)
            }
    }

    /**
     * Öğretmen şifre sıfırlama: OTP gönderir.
     *
     * E-postanın TEACHER rolüne ait olduğu kontrolü artık sunucuda da yapılıyor
     * (purpose = teacher_reset); buradaki ön kontrol yalnızca hata mesajını
     * kullanıcıya daha hızlı göstermek için duruyor.
     */
    fun sendTeacherPasswordResetCode(email: String, callback: (Boolean, String?) -> Unit) {
        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty()) {
            callback(false, "E-posta adresi girin")
            return
        }
        sendStudentVerificationCode(trimmed, OTP_PURPOSE_TEACHER_RESET) { success, error ->
            callback(success, error)
        }
    }

    /** Öğretmen şifre sıfırlama: kodu sadece doğrular, used işaretlemez. */
    fun verifyTeacherPasswordResetCode(email: String, code: String, callback: (Boolean, String?) -> Unit) {
        val payload = hashMapOf<String, Any>(
            "email" to email.trim().lowercase(),
            "code" to code
        )
        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyTeacherPasswordResetCode")
            .call(payload)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                val msg = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.message
                    ?: e.localizedMessage
                callback(false, msg ?: "Kod doğrulanamadı")
            }
    }

    /** Öğretmen şifre sıfırlama: kodu doğrular, şifreyi günceller. */
    fun resetTeacherPassword(email: String, code: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        val payload = hashMapOf<String, Any>(
            "email" to email.trim().lowercase(),
            "code" to code,
            "newPassword" to newPassword
        )
        FirebaseFunctions.getInstance()
            .getHttpsCallable("resetTeacherPassword")
            .call(payload)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                val msg = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.message
                    ?: e.localizedMessage
                callback(false, msg ?: "Şifre güncellenemedi")
            }
    }

    internal fun cacheBasicUser(email: String, role: String, name: String, userId: String = "") {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_type", role)
            .putString("user_name", name)
            .putString("user_id", userId)
            .apply()
    }

    private fun currentLocalFirstTutorialShown(): Boolean {
        if (!::appContext.isInitialized) return false
        return appContext
            .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getBoolean("first_tutorial_shown", false)
    }

    fun checkSkipStepTutorialShown(callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            callback(true) // Kullanıcı yoksa gösterilmiş say
            return
        }
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.contains("skip_step_tutorial_shown")) {
                    callback(doc.getBoolean("skip_step_tutorial_shown") ?: false)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(true)
            }
    }

    fun setSkipStepTutorialShown() {
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .update("skip_step_tutorial_shown", true)
        }
    }

    /**
     * Benzersiz kullanıcı ID'si oluşturur (örn: ogrenci_12345, ogretmen_67890)
     * Firestore'da benzersizlik kontrolü yapar
     */
    private fun generateUniqueUserId(role: String, callback: (String) -> Unit) {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var attempts = 0
        val maxAttempts = 15

        fun tryGenerate() {
            attempts++
            val randomNum = (100000..999999).random()
            val suffix = "${letters.random()}${letters.random()}"
            val userId = "${randomNum}${suffix}"

            // Firestore'da bu userId'nin kullanılıp kullanılmadığını kontrol et
            firestore.collection("users")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // Benzersiz ID bulundu
                        callback(userId)
                    } else {
                        // ID kullanılıyor, tekrar dene
                        if (attempts < maxAttempts) {
                            tryGenerate()
                        } else {
                            // Max deneme sayısına ulaşıldı, timestamp + random suffix ile benzersiz yap
                            val timestamp = System.currentTimeMillis() % 1000000
                            val fallbackSuffix = "${letters.random()}${letters.random()}"
                            callback("${timestamp}${fallbackSuffix}")
                        }
                    }
                }
                .addOnFailureListener {
                    // Hata durumunda timestamp + random suffix ile benzersiz ID oluştur
                    val timestamp = System.currentTimeMillis() % 1000000
                    val fallbackSuffix = "${letters.random()}${letters.random()}"
                    callback("${timestamp}${fallbackSuffix}")
                }
        }

        tryGenerate()
    }

    /** E-posta kayıtlı mı Cloud Function ile güvenli kontrol eder; kayıtlıysa uid ve role döner. callback(registered, uid, role). */
    fun isEmailRegistered(email: String, callback: (Boolean, String?, String?) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        android.util.Log.d("AuthManager", "isEmailRegistered Cloud Function çağrıldı - email: $normalizedEmail")

        val payload = hashMapOf<String, Any>("email" to normalizedEmail)
        FirebaseFunctions.getInstance()
            .getHttpsCallable("checkEmailRegistered")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val registered = data?.get("registered") as? Boolean ?: false
                val uid = data?.get("uid") as? String
                val role = data?.get("role") as? String
                android.util.Log.d("AuthManager", "isEmailRegistered sonuç: registered=$registered, uid=$uid, role=$role")
                callback(registered, uid, role)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthManager", "isEmailRegistered Cloud Function hatası: ${e.message}", e)
                callback(false, null, null)
            }
    }

    /**
     * Giriş kodu gönderir.
     *
     * [uid] parametresi artık kullanılmıyor — sunucu hedef hesabı e-postadan kendisi
     * buluyor. Çağrı yerlerini bozmamak için imza korundu.
     */
    @Suppress("UNUSED_PARAMETER")
    fun sendLoginCode(email: String, uid: String, callback: (Boolean, String?) -> Unit) {
        sendStudentVerificationCode(email.trim().lowercase(), OTP_PURPOSE_LOGIN, callback)
    }

    /**
     * Sadece öğrenci girişi için kod gönderir. Öğretmen e-postasına kod gönderilmez;
     * bu kural sunucuda da uygulanıyor (purpose = login → rol STUDENT olmalı).
     */
    fun sendLoginCodeOnly(email: String, callback: (Boolean, String?) -> Unit) {
        sendStudentVerificationCode(email.trim().lowercase(), OTP_PURPOSE_LOGIN) { success, error ->
            callback(success, error)
        }
    }

    /**
     * OTP ile öğrenci kaydını hazırlar.
     *
     * Artık Firestore'a HİÇBİR ŞEY YAZMIYOR. Eskiden `pendingRegistrations/{email}`
     * dokümanına düz metin şifre yazılıyordu ve o koleksiyona kimliksiz yazma açıktı;
     * saldırgan kurbanın dokümanını kendi şifresiyle ezip hesabı devralabiliyordu.
     * Bilgi yalnızca bellekte tutulur, `verifyStudentCode` sırasında sunucuya gider.
     *
     * Şifre gönderilmiyor: OTP ile giren öğrenci hesabı için şifreyi sunucu üretir.
     */
    fun createPendingRegistrationForOTP(email: String, callback: (Boolean, String?) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isEmpty()) {
            callback(false, "Geçersiz bilgiler")
            return
        }
        rememberPendingRegistration(normalizedEmail, name = "", password = "", role = ROLE_STUDENT)
        callback(true, null)
    }

    /**
     * OTP ile öğretmen kaydını hazırlar (kullanıcı adı + şifre, role = TEACHER).
     * Bkz. [createPendingRegistrationForOTP] — bilgi Firestore'a yazılmaz, bellekte tutulur.
     */
    fun createPendingTeacherRegistrationForOTP(
        email: String,
        name: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isEmpty() || name.isBlank() || password.length < 6) {
            callback(false, "Geçersiz bilgiler")
            return
        }
        rememberPendingRegistration(normalizedEmail, name.trim(), password, ROLE_TEACHER)
        callback(true, null)
    }

    /** userId ile öğretmen e-postasını bul (role = TEACHER) - Cloud Function ile güvenli sorgu */
    fun findTeacherEmailByUserId(userId: String, callback: (String?) -> Unit) {
        val trimmedId = userId.trim()
        if (trimmedId.isEmpty()) {
            callback(null)
            return
        }
        val payload = hashMapOf<String, Any>("userId" to trimmedId)
        FirebaseFunctions.getInstance()
            .getHttpsCallable("findTeacherEmailByUserId")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val email = data?.get("email") as? String
                callback(email)
            }
            .addOnFailureListener { callback(null) }
    }

    /** OTP ile yalnızca öğrenci girişi. Öğretmen hesabıyla giriş yapılamaz. */
    fun verifyLoginWithOTP(email: String, code: String, callback: (Boolean, String?) -> Unit) {
        val payload = hashMapOf<String, Any>("email" to email, "code" to code)
        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyLoginCode")
            .call(payload)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any>
                val token = data?.get("token") as? String
                if (token.isNullOrBlank()) {
                    callback(false, "Token alınamadı")
                    return@addOnSuccessListener
                }
                auth.signInWithCustomToken(token)
                    .addOnSuccessListener {
                        val user = auth.currentUser ?: run {
                            callback(false, "Oturum açılamadı")
                            return@addOnSuccessListener
                        }
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role") ?: ROLE_STUDENT
                                if (role != ROLE_STUDENT) {
                                    auth.signOut()
                                    callback(false, "Bu hesap öğretmen hesabı. Öğrenci giriş ekranından giriş yapılamaz.")
                                    return@addOnSuccessListener
                                }
                                val name = doc.getString("name") ?: ""
                                val userId = doc.getString("userId") ?: ""
                                cacheBasicUser(email, role, name, userId)
                                callback(true, null)
                            }
                            .addOnFailureListener { callback(true, null) }
                    }
                    .addOnFailureListener { e -> callback(false, e.localizedMessage) }
            }
            .addOnFailureListener { e ->
                val msg = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.message
                    ?: e.localizedMessage
                callback(false, msg ?: "Kod doğrulanamadı")
            }
    }

    fun signInWithGoogle(
        account: GoogleSignInAccount,
        autoRegister: Boolean = false,
        birthYear: Int? = null,
        acquisitionSource: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        if (account.idToken == null) {
            android.util.Log.e("AuthManager", "Google Sign-In account idToken is null")
            callback(false, "Google hesap bilgisi alınamadı. Lütfen tekrar deneyin.")
            return
        }
        
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        
        android.util.Log.d("AuthManager", "Firebase Auth ile Google credential kullanılıyor")
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    
                    // Detaylı log
                    val errorCode =
                        (exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    val errorMessageFull = exception?.message ?: ""
                    val errorClass = exception?.javaClass?.simpleName ?: "Unknown"
                    
                    android.util.Log.e("AuthManager", "=== Firebase Auth Error ===")
                    android.util.Log.e("AuthManager", "Error Code: $errorCode")
                    android.util.Log.e("AuthManager", "Error Message: $errorMessageFull")
                    android.util.Log.e("AuthManager", "Error Class: $errorClass")
                    android.util.Log.e("AuthManager", "Full Exception:", exception)
                    
                    val errorMessage = when {
                        errorCode == "ERROR_NETWORK_REQUEST_FAILED" || 
                        errorMessageFull.contains("network", ignoreCase = true) -> 
                            "Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin."
                        
                        errorCode == "ERROR_INVALID_CREDENTIAL" || 
                        errorMessageFull.contains("invalid", ignoreCase = true) -> 
                            "Geçersiz kimlik bilgisi. Web Client ID doğru mu kontrol edin."
                        
                        errorCode == "ERROR_OPERATION_NOT_ALLOWED" || 
                        errorMessageFull.contains("permission", ignoreCase = true) || 
                        errorMessageFull.contains("PERMISSION_DENIED", ignoreCase = true) ||
                                errorMessageFull.contains(
                                    "OPERATION_NOT_ALLOWED",
                                    ignoreCase = true
                                ) -> {
                            val detailedError = """
                            İzin hatası (Error Code: $errorCode)
                            
                            Kontrol edin:
                            1. Firebase Console > Authentication > Sign-in method > Google ENABLED mi?
                            2. Google Cloud Console > OAuth consent screen yapılandırıldı mı?
                            3. Test users eklendi mi? (OAuth consent screen'de)
                            4. SHA-1 fingerprint eklendi mi? (71:43:C7:24:86:97:58:FA:59:EB:3D:F2:BC:E0:77:77:43:5D:B4:4E)
                            5. Identity Toolkit API etkin mi?
                            
                            Logcat'te detaylı hata: $errorMessageFull
                            """.trimIndent()
                            detailedError
                        }
                        
                        errorCode == "ERROR_DEVELOPER_ERROR" ||
                        errorMessageFull.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                            "Yapılandırma hatası:\n\n1. Web Client ID doğru mu? (strings.xml)\n2. SHA-1 fingerprint Firebase Console'a eklendi mi?\n3. Google Sign-In Firebase Console'da etkin mi?\n\nHata: $errorMessageFull"
                        
                        else -> {
                            "Giriş başarısız (Code: $errorCode)\n\n$errorMessageFull\n\nLogcat'te detaylı bilgi için 'AuthManager' tag'ini kontrol edin."
                        }
                    }
                    
                    callback(false, errorMessage)
                    return@addOnCompleteListener
                }
                
                val user = auth.currentUser
                if (user == null) {
                    callback(false, "Kullanıcı bulunamadı")
                    return@addOnCompleteListener
                }
                
                // Kullanıcının daha önce kayıt olup olmadığını kontrol et
                firestore.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            // Mevcut kullanıcı - rol kontrolü yap
                            val role = doc.getString("role") ?: ROLE_STUDENT
                            
                            // Öğretmen hesabı ise Google ile giriş yapılmasına izin verme
                            if (role == ROLE_TEACHER) {
                                auth.signOut()
                                callback(false, "Giriş bilgileri doğrulanamadı.")
                                return@addOnSuccessListener
                            }

                            var userId = doc.getString("userId") ?: ""

                            // Eğer userId yoksa oluştur (eski kullanıcılar için)
                            if (userId.isEmpty()) {
                                generateUniqueUserId(role) { newUserId ->
                                    userId = newUserId
                                    firestore.collection("users").document(user.uid)
                                        .update("userId", userId)
                                        .addOnSuccessListener {
                                            firestore.collection("users").document(user.uid)
                                                .update("verified", true)
                                                .addOnSuccessListener {
                                cacheBasicUser(
                                    user.email ?: "",
                                                        ROLE_STUDENT,
                                                        user.displayName ?: "",
                                                        userId
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    cacheBasicUser(
                                                        user.email ?: "",
                                                        ROLE_STUDENT,
                                                        user.displayName ?: "",
                                                        userId
                                                    )
                                                }
                                callback(true, null)
                                        }
                                        .addOnFailureListener {
                                            cacheBasicUser(
                                                user.email ?: "",
                                                role,
                                                user.displayName ?: "",
                                                userId
                                            )
                                            callback(true, null)
                                        }
                                }
                                return@addOnSuccessListener
                            }

                                // Öğrenci girişi - verified kontrolü
                                val verified = doc.getBoolean("verified") ?: false
                                if (!verified) {
                                    // E-posta doğrulaması gerekli değil Google Sign-In için
                                    // Ama yine de verified olarak işaretle
                                    firestore.collection("users").document(user.uid)
                                        .update("verified", true)
                                        .addOnSuccessListener {
                                            cacheBasicUser(
                                                user.email ?: "",
                                                ROLE_STUDENT,
                                            user.displayName ?: "",
                                            userId
                                        )
                                        callback(true, null)
                                    }
                                    .addOnFailureListener {
                                        cacheBasicUser(
                                            user.email ?: "",
                                            ROLE_STUDENT,
                                            user.displayName ?: "",
                                            userId
                                            )
                                            callback(true, null)
                                        }
                                } else {
                                    cacheBasicUser(
                                        user.email ?: "",
                                        ROLE_STUDENT,
                                    user.displayName ?: "",
                                    userId
                                    )
                                    callback(true, null)
                            }
                        } else {
                            // Firestore'da kullanıcı yok
                            if (autoRegister) {
                                // Kayıt ekranından çağrıldıysa otomatik kayıt yap
                                android.util.Log.d(
                                    "AuthManager",
                                    "Kayıt ekranından çağrıldı, otomatik kayıt yapılıyor"
                                )
                                generateUniqueUserId(ROLE_STUDENT) { userId ->
                                    val userData = mutableMapOf<String, Any>(
                                        "uid" to user.uid,
                                        "userId" to userId,
                                        "email" to (user.email ?: ""),
                                        "name" to (user.displayName ?: ""),
                                        "role" to ROLE_STUDENT,
                                        "verified" to true, // Google Sign-In ile gelen kullanıcılar otomatik verified
                                        "first_tutorial_shown" to currentLocalFirstTutorialShown(),
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )
                                    birthYear?.let { userData["birthYear"] = it }
                                    acquisitionSource?.let { src ->
                                        userData["acquisitionSource"] = src
                                        AppStatisticsManager.incrementAcquisitionSource(src)
                                    }
                                    userData.putAll(UserWalletFirestore.registrationWalletFields())
                                    
                                    firestore.collection("users").document(user.uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            cacheBasicUser(
                                                user.email ?: "",
                                                ROLE_STUDENT,
                                                user.displayName ?: "",
                                                userId
                                            )
                                            callback(true, null)
                                        }
                                        .addOnFailureListener { e ->
                                            callback(false, e.localizedMessage)
                                        }
                                }
                            } else {
                                // Giriş ekranından çağrıldıysa kayıt ekranına yönlendir
                                android.util.Log.w(
                                    "AuthManager",
                                    "Firestore'da kullanıcı bulunamadı ama Firebase Auth'da var. Kullanıcı çıkış yaptırılıyor."
                                )
                                
                                // Firebase Auth'dan çıkış yap
                                auth.signOut()
                                
                                // Özel bir hata kodu döndür ki UI'da kayıt ekranına yönlendirilebilsin
                                callback(false, "ACCOUNT_NOT_REGISTERED")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.localizedMessage)
                    }
            }
    }
    
    fun handleGoogleSignInResult(
        task: Task<GoogleSignInAccount>,
        autoRegister: Boolean = false,
        birthYear: Int? = null,
        acquisitionSource: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                android.util.Log.d(
                    "AuthManager",
                    "Google hesabı seçildi: ${account.email}, autoRegister: $autoRegister"
                )
                signInWithGoogle(account, autoRegister, birthYear, acquisitionSource, callback)
            } else {
                android.util.Log.e("AuthManager", "Google hesabı null")
                callback(false, "Google hesabı seçilemedi")
            }
        } catch (e: ApiException) {
            android.util.Log.e(
                "AuthManager",
                "Google sign in failed - Status: ${e.statusCode}, Message: ${e.message}",
                e
            )
            
            // Kullanıcı iptal ettiyse (12501) sessizce dön
            if (e.statusCode == 12501) {
                android.util.Log.d("AuthManager", "Kullanıcı Google Sign-In'i iptal etti")
                callback(false, "Kullanıcı girişi iptal edildi")
                return
            }
            
            val errorMessage = when (e.statusCode) {
                10 -> "Yapılandırma hatası: Web Client ID eksik veya yanlış. Firebase Console'dan Web Client ID'yi alıp strings.xml'e ekleyin."
                7 -> "Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin."
                8 -> "İstek limiti aşıldı. Lütfen daha sonra tekrar deneyin."
                else -> "Google girişi başarısız (Hata: ${e.statusCode}): ${e.localizedMessage ?: e.message}"
            }
            
            callback(false, errorMessage)
        }
    }
    
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun loginStudent(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(false, task.exception?.localizedMessage)
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    callback(false, "Kullanıcı bulunamadı")
                    return@addOnCompleteListener
                }

                fetchUserRole(user.uid) { role ->
                    if (role != ROLE_STUDENT) {
                        callback(false, "Bu hesap öğrenci değil")
                        return@fetchUserRole
                    }
                    
                    // Firestore'dan verified kontrolü yap
                    firestore.collection("users").document(user.uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val verified = doc.getBoolean("verified") ?: false
                            val userId = doc.getString("userId") ?: ""
                            if (!verified) {
                                callback(
                                    false,
                                    "E-posta doğrulaması gerekli. Lütfen e-postanızı doğrulayın."
                                )
        } else {
                                cacheBasicUser(email, ROLE_STUDENT, user.displayName ?: "", userId)
                                callback(true, null)
                            }
                        }
                        .addOnFailureListener {
                            callback(false, "Kullanıcı bilgileri alınamadı")
                        }
                }
            }
    }

    fun loginTeacher(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(false, "Şifre veya e-posta hatalı")
                    return@addOnCompleteListener
                }
                val user = auth.currentUser
                if (user == null) {
                    callback(false, "Şifre veya e-posta hatalı")
                    return@addOnCompleteListener
                }
                firestore.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val role = doc.getString("role") ?: ""
                        val userId = doc.getString("userId") ?: ""
                    if (role == ROLE_TEACHER) {
                            cacheBasicUser(email, role, user.displayName ?: "", userId)
                        callback(true, null)
        } else {
                        callback(false, "Bu hesap öğretmen değil")
                    }
                }
                    .addOnFailureListener {
                        callback(false, "Şifre veya e-posta hatalı")
                    }
            }
    }

    fun registerStudent(
        email: String,
        name: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        android.util.Log.d("AuthManager", "registerStudent çağrıldı - email: $email, name: $name")
        
        // Direkt Firebase Auth'da hesap oluştur
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    val errorMessage = when {
                        exception?.message?.contains(
                            "email address is already in use",
                            ignoreCase = true
                        ) == true ->
                            "Bu e-posta adresi zaten kayıtlı. Lütfen giriş yapın."

                        exception?.message?.contains("weak password", ignoreCase = true) == true -> 
                            "Şifre çok zayıf. Daha güçlü bir şifre seçin."

                        exception?.message?.contains("invalid email", ignoreCase = true) == true -> 
                            "Geçersiz e-posta adresi."

                        else -> exception?.localizedMessage
                            ?: "Kayıt başarısız. Lütfen tekrar deneyin."
                    }
                    android.util.Log.e("AuthManager", "Kayıt başarısız: $errorMessage")
                    callback(false, errorMessage)
                    return@addOnCompleteListener
                }
                
                val user = auth.currentUser
                if (user == null) {
                    android.util.Log.e("AuthManager", "Kullanıcı oluşturulamadı")
                    callback(false, "Kullanıcı oluşturulamadı")
                    return@addOnCompleteListener
                }
                
                // Display name ayarla
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(profileUpdates)
                
                // Firestore'a kullanıcı dokümanı yaz (verified: false - e-posta doğrulanmamış)
                generateUniqueUserId(ROLE_STUDENT) { userId ->
                val userData = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                        "userId" to userId,
                    "email" to email,
                    "name" to name,
                    "role" to ROLE_STUDENT,
                    "verified" to false, // E-posta doğrulanmamış - profil ekranında doğrulanacak
                    "first_tutorial_shown" to currentLocalFirstTutorialShown(),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                userData.putAll(UserWalletFirestore.registrationWalletFields())
                
                firestore.collection("users").document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                            android.util.Log.d(
                                "AuthManager",
                                "Kullanıcı başarıyla kaydedildi - verified: false"
                            )
                            cacheBasicUser(email, ROLE_STUDENT, name, userId)
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                            android.util.Log.e(
                                "AuthManager",
                                "Firestore'a kayıt hatası: ${e.message}",
                                e
                            )
                        // Firebase Auth'da hesap oluşturuldu ama Firestore'a yazılamadı
                        // Kullanıcıyı sil
                        user.delete()
                        callback(false, "Kayıt başarısız: ${e.localizedMessage}")
                        }
                }
            }
    }

    fun registerTeacher(
        email: String,
        name: String,
        password: String,
        approvalCode: String,
        callback: (Boolean, String?) -> Unit
    ) {
        // Firestore: teacherInvites/{code} doğrulaması
        firestore.collection("teacherInvites").document(approvalCode)
            .get()
            .addOnSuccessListener { doc ->
                val exists = doc.exists()
                val used = doc.getBoolean("used") ?: true
                val expiresAt = doc.getTimestamp("expiresAt")
                val now = com.google.firebase.Timestamp.now()

                if (!exists || used || (expiresAt != null && expiresAt < now)) {
                    callback(false, "Geçersiz veya süresi dolmuş kod")
                    return@addOnSuccessListener
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            callback(false, task.exception?.localizedMessage)
                            return@addOnCompleteListener
                        }
                        val user = auth.currentUser
                        if (user == null) {
                            callback(false, "Kullanıcı oluşturulamadı")
                            return@addOnCompleteListener
                        }

                        val profileUpdates =
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        user.updateProfile(profileUpdates)

                        generateUniqueUserId(ROLE_TEACHER) { userId ->
                        val data = mutableMapOf<String, Any>(
                            "uid" to user.uid,
                                "userId" to userId,
                            "email" to email,
                            "name" to name,
                            "role" to ROLE_TEACHER,
                            "first_tutorial_shown" to currentLocalFirstTutorialShown(),
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                        data.putAll(UserWalletFirestore.registrationWalletFields())
                        firestore.collection("users").document(user.uid)
                            .set(data)
                            .addOnSuccessListener {
                                // Kodu kullanıldı işaretle
                                firestore.collection("teacherInvites").document(approvalCode)
                                        .update(
                                            mapOf(
                                        "used" to true,
                                        "usedByUid" to user.uid,
                                        "usedAt" to com.google.firebase.Timestamp.now()
                                            )
                                        )
                                    cacheBasicUser(email, ROLE_TEACHER, name, userId)
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                callback(false, e.localizedMessage)
                            }
                    }
            }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
            }
    }

    private fun fetchUserRole(uid: String, callback: (String) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                callback(doc.getString("role") ?: "")
            }
            .addOnFailureListener { callback("") }
    }

    fun requestTeacherInviteCode(
        candidateEmail: String?,
        callback: (Boolean, String?) -> Unit
    ) {
        val payload = hashMapOf<String, Any>(
            "recipient" to "basartumturk2@gmail.com",
            "candidateEmail" to (candidateEmail ?: "")
        )
        
        FirebaseFunctions.getInstance()
            .getHttpsCallable("createTeacherInvite")
            .call(payload)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
    }

    /**
     * OTP gönderir.
     *
     * Not: Hedef `uid` ARTIK GÖNDERİLMİYOR. Sunucu, [purpose] ve e-postaya bakarak uid'i
     * kendisi çözüyor — eskiden istemcinin gönderdiği uid doğrulanmadan kabul ediliyordu
     * ve başka bir kullanıcının uid'i verilerek o hesap için token alınabiliyordu.
     */
    private fun sendStudentVerificationCode(
        email: String,
        purpose: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val payload = hashMapOf<String, Any>(
            "email" to email,
            "purpose" to purpose
        )

        android.util.Log.d(
            "AuthManager",
            "sendStudentVerificationCode çağrıldı - email: $email, purpose: $purpose"
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("sendStudentVerificationCode")
            .call(payload)
            .addOnSuccessListener { result ->
                android.util.Log.d("AuthManager", "sendStudentVerificationCode başarılı")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                android.util.Log.e(
                    "AuthManager",
                    "sendStudentVerificationCode hatası: ${e.message}",
                    e
                )
                val msg = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.message
                    ?: e.localizedMessage
                callback(false, msg ?: "Kod gönderilemedi")
            }
    }

    fun resendStudentVerificationCode(
        email: String,
        isRegistration: Boolean = false,
        callback: (Boolean, String?) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()
        val purpose = if (isRegistration) OTP_PURPOSE_REGISTER else OTP_PURPOSE_LOGIN
        sendStudentVerificationCode(normalizedEmail, purpose) { success, error ->
            callback(success, error)
        }
    }

    fun verifyStudentCode(email: String, code: String, callback: (Boolean, String?) -> Unit) {
        verifyStudentCode(email, code, autoLogin = false, callback = callback)
    }

    /**
     * Kayıt kodunu doğrular ve hesabı oluşturur.
     *
     * Ad / şifre / rol bilgisi bu çağrının gövdesinde gider ([rememberPendingRegistration]
     * ile bellekte tutulan değerler). Eskiden bunlar Firestore'a düz metin yazılıyordu.
     * [birthYear] ve [acquisitionSource] da sunucuda yazılır — `birthYear` reklam yaş
     * korumasını belirlediği için istemci yazımına kapalıdır (bkz. firestore.rules).
     */
    fun verifyStudentCode(
        email: String,
        code: String,
        autoLogin: Boolean,
        birthYear: Int? = null,
        acquisitionSource: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        android.util.Log.d("AuthManager", "verifyStudentCode Cloud Function çağrılıyor - email: $email, autoLogin: $autoLogin")
        val normalizedEmail = email.trim().lowercase()
        val pending = consumePendingRegistration(normalizedEmail)
        val payload = hashMapOf<String, Any>("email" to normalizedEmail, "code" to code)
        pending?.let {
            if (it.name.isNotBlank()) payload["name"] = it.name
            if (it.password.isNotEmpty()) payload["password"] = it.password
            payload["role"] = it.role
        }
        birthYear?.let { payload["birthYear"] = it }
        acquisitionSource?.let { payload["acquisitionSource"] = it }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyRegistrationCode")
            .call(payload)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any>
                val token = data?.get("token") as? String
                if (token.isNullOrBlank()) {
                    callback(false, "Token alınamadı")
                    return@addOnSuccessListener
                }
                
                auth.signInWithCustomToken(token)
                    .addOnSuccessListener {
                        if (!autoLogin) {
                            auth.signOut()
                            callback(true, null)
                            return@addOnSuccessListener
                        }
                        
                        val user = auth.currentUser ?: run {
                            callback(false, "Oturum açılamadı")
                            return@addOnSuccessListener
                        }
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role") ?: ROLE_STUDENT
                                val name = doc.getString("name") ?: ""
                                val userId = doc.getString("userId") ?: ""
                                cacheBasicUser(email, role, name, userId)
                                callback(true, null)
                            }
                            .addOnFailureListener { callback(true, null) }
                    }
                    .addOnFailureListener { e -> callback(false, e.localizedMessage) }
            }
            .addOnFailureListener { e ->
                val msg = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.message
                    ?: e.localizedMessage
                callback(false, msg ?: "Kod doğrulanamadı")
            }
    }
    
    fun logout() {
        auth.signOut()
        prefs.edit().clear().apply()
    }

    fun getCurrentUserId(): String {
        return prefs.getString("user_id", "") ?: ""
    }
    
    fun getCurrentUserType(): String {
        return prefs.getString("user_type", "") ?: ""
    }
    
    fun getCurrentUserEmail(): String {
        return prefs.getString("user_email", "") ?: ""
    }
    
    fun getCurrentUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }
}
