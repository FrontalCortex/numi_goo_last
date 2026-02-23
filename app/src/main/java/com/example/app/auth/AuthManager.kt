package com.example.app.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.firebase.functions.FirebaseFunctions

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var prefs: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_TEACHER = "TEACHER"
        private const val PREFS_NAME = "auth_prefs"
    }

    fun initialize(context: Context) {
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

    internal fun cacheBasicUser(email: String, role: String, name: String, userId: String = "") {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_type", role)
            .putString("user_name", name)
            .putString("user_id", userId)
            .apply()
    }

    /**
     * Benzersiz kullanıcı ID'si oluşturur (örn: ogrenci_12345, ogretmen_67890)
     * Firestore'da benzersizlik kontrolü yapar
     */
    private fun generateUniqueUserId(role: String, callback: (String) -> Unit) {
        val prefix = if (role == ROLE_TEACHER) "ogretmen" else "ogrenci"
        var attempts = 0
        val maxAttempts = 10

        fun tryGenerate() {
            attempts++
            val randomNum = (10000..99999).random()
            val userId = "${prefix}_${randomNum}"

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
                            // Max deneme sayısına ulaşıldı, timestamp ekleyerek benzersiz yap
                            val timestamp = System.currentTimeMillis() % 100000
                            callback("${prefix}_${timestamp}")
                        }
                    }
                }
                .addOnFailureListener {
                    // Hata durumunda timestamp ile benzersiz ID oluştur
                    val timestamp = System.currentTimeMillis() % 100000
                    callback("${prefix}_${timestamp}")
                }
        }

        tryGenerate()
    }

    /** E-posta kayıtlı mı Firestore ile kontrol eder; kayıtlıysa uid döner */
    fun isEmailRegistered(email: String, callback: (Boolean, String?) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        android.util.Log.d("AuthManager", "isEmailRegistered çağrıldı - email: $normalizedEmail")
        // Firestore'da email kontrolü yap (rules güncellendi, unauthenticated kullanıcılar where query yapabilir)
        firestore.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("AuthManager", "isEmailRegistered query sonucu - empty: ${querySnapshot.isEmpty}, size: ${querySnapshot.size()}")
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val uid = doc.getString("uid") ?: doc.id
                    android.util.Log.d("AuthManager", "isEmailRegistered - email kayıtlı, uid: $uid")
                    callback(true, uid)
                } else {
                    // Email lowercase'den farklıysa, original email ile de dene
                    if (email.trim() != normalizedEmail) {
                        android.util.Log.d("AuthManager", "isEmailRegistered - original email ile tekrar deniyor: ${email.trim()}")
                        firestore.collection("users")
                            .whereEqualTo("email", email.trim())
                            .limit(1)
                            .get()
                            .addOnSuccessListener { querySnapshot2 ->
                                if (!querySnapshot2.isEmpty) {
                                    val doc = querySnapshot2.documents[0]
                                    val uid = doc.getString("uid") ?: doc.id
                                    android.util.Log.d("AuthManager", "isEmailRegistered - email kayıtlı (original), uid: $uid")
                                    callback(true, uid)
                                } else {
                                    android.util.Log.d("AuthManager", "isEmailRegistered - email kayıtlı değil")
                                    callback(false, null)
                                }
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("AuthManager", "isEmailRegistered - query hatası (original): ${e.message}", e)
                                callback(false, null)
                            }
                    } else {
                        android.util.Log.d("AuthManager", "isEmailRegistered - email kayıtlı değil")
                        callback(false, null)
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthManager", "isEmailRegistered - query hatası: ${e.message}", e)
                callback(false, null)
            }
    }

    /** Giriş kodu gönderir (uid Cloud Function'dan alındığında Firestore okumaya gerek yok) */
    fun sendLoginCode(email: String, uid: String, callback: (Boolean, String?) -> Unit) {
        sendStudentVerificationCode(email, uid, callback)
    }

    /** Sadece giriş kodu gönderir: sadece users koleksiyonuna bakar, pendingRegistrations'a bakmaz. Giriş ekranı için. */
    fun sendLoginCodeOnly(email: String, callback: (Boolean, String?) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        firestore.collection("users")
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val uid = doc.getString("uid") ?: doc.id
                    sendStudentVerificationCode(normalizedEmail, uid) { success, error ->
                        callback(success, error)
                    }
                } else {
                    if (email.trim() != normalizedEmail) {
                        firestore.collection("users")
                            .whereEqualTo("email", email.trim())
                            .limit(1)
                            .get()
                            .addOnSuccessListener { qs ->
                                if (!qs.isEmpty) {
                                    val doc = qs.documents[0]
                                    val uid = doc.getString("uid") ?: doc.id
                                    sendStudentVerificationCode(email.trim(), uid) { success, error ->
                                        callback(success, error)
                                    }
                                } else {
                                    callback(false, "Kullanıcı bulunamadı")
                                }
                            }
                            .addOnFailureListener {
                                callback(false, "Kullanıcı bulunamadı")
                            }
                    } else {
                        callback(false, "Kullanıcı bulunamadı")
                    }
                }
            }
            .addOnFailureListener {
                callback(false, "Kullanıcı bulunamadı")
            }
    }

    /** OTP ile kayıt için pending registration oluşturur (email + rastgele şifre, isim boş) */
    fun createPendingRegistrationForOTP(email: String, callback: (Boolean, String?) -> Unit) {
        val randomPassword = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
        val pendingData = mapOf(
            "email" to email,
            "name" to "",
            "password" to randomPassword,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("pendingRegistrations").document(email)
            .set(pendingData)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
    }

    /** OTP ile giriş: Cloud Function'dan custom token alıp oturum açar */
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
                                val name = doc.getString("name") ?: ""
                                val userId = doc.getString("userId") ?: ""
                                cacheBasicUser(email, ROLE_STUDENT, name, userId)
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
                            var userId = doc.getString("userId") ?: ""

                            // Eğer userId yoksa oluştur (eski kullanıcılar için)
                            if (userId.isEmpty()) {
                                generateUniqueUserId(role) { newUserId ->
                                    userId = newUserId
                                    firestore.collection("users").document(user.uid)
                                        .update("userId", userId)
                                        .addOnSuccessListener {
                                            if (role == ROLE_TEACHER) {
                                                cacheBasicUser(
                                                    user.email ?: "",
                                                    ROLE_TEACHER,
                                                    user.displayName ?: "",
                                                    userId
                                                )
                                            } else {
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

                            if (role == ROLE_TEACHER) {
                                // Öğretmen girişi
                                cacheBasicUser(
                                    user.email ?: "",
                                    ROLE_TEACHER,
                                    user.displayName ?: "",
                                    userId
                                )
                                callback(true, null)
                            } else {
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
                                    val userData = mapOf(
                                        "uid" to user.uid,
                                        "userId" to userId,
                                        "email" to (user.email ?: ""),
                                        "name" to (user.displayName ?: ""),
                                        "role" to ROLE_STUDENT,
                                        "verified" to true, // Google Sign-In ile gelen kullanıcılar otomatik verified
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )

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
        callback: (Boolean, String?) -> Unit
    ) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                android.util.Log.d(
                    "AuthManager",
                    "Google hesabı seçildi: ${account.email}, autoRegister: $autoRegister"
                )
                signInWithGoogle(account, autoRegister, callback)
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
                    callback(false, task.exception?.localizedMessage)
                    return@addOnCompleteListener
                }
                val user = auth.currentUser
                if (user == null) {
                    callback(false, "Kullanıcı bulunamadı")
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
                        callback(false, "Kullanıcı bilgileri alınamadı")
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
                    val userData = mapOf(
                        "uid" to user.uid,
                        "userId" to userId,
                        "email" to email,
                        "name" to name,
                        "role" to ROLE_STUDENT,
                        "verified" to false, // E-posta doğrulanmamış - profil ekranında doğrulanacak
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

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
                            val data = mapOf(
                                "uid" to user.uid,
                                "userId" to userId,
                                "email" to email,
                                "name" to name,
                                "role" to ROLE_TEACHER,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )
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

    private fun sendStudentVerificationCode(
        email: String,
        uid: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val payload = hashMapOf<String, Any>(
            "email" to email,
            "uid" to uid
        )

        android.util.Log.d(
            "AuthManager",
            "sendStudentVerificationCode çağrıldı - email: $email, uid: $uid"
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

    fun resendStudentVerificationCode(email: String, callback: (Boolean, String?) -> Unit) {
        // Önce pending registration kontrolü yap
        firestore.collection("pendingRegistrations").document(email)
            .get()
            .addOnSuccessListener { pendingDoc ->
                if (pendingDoc.exists()) {
                    // Pending registration var - yeni kayıt için kod gönder
                    val tempUid = "pending_${System.currentTimeMillis()}"
                    sendStudentVerificationCode(email, tempUid) { success, error ->
                        callback(success, error)
                    }
                } else {
                    // Pending registration yok - mevcut kullanıcı için kod gönder
                    firestore.collection("users")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val doc = querySnapshot.documents[0]
                                val uid = doc.getString("uid") ?: doc.id
                                sendStudentVerificationCode(email, uid) { success, error ->
                                    callback(success, error)
                                }
                            } else {
                                callback(false, "Kullanıcı bulunamadı")
                            }
                        }
                        .addOnFailureListener {
                            callback(false, "Kullanıcı bulunamadı")
                        }
                }
            }
            .addOnFailureListener {
                // Pending registration kontrolü başarısız - mevcut kullanıcı için dene
                firestore.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val doc = querySnapshot.documents[0]
                            val uid = doc.getString("uid") ?: doc.id
                            sendStudentVerificationCode(email, uid) { success, error ->
                                callback(success, error)
                            }
                        } else {
                            callback(false, "Kullanıcı bulunamadı")
                        }
                    }
                    .addOnFailureListener {
                        callback(false, "Kod gönderilemedi")
                    }
            }
    }

    fun verifyStudentCode(email: String, code: String, callback: (Boolean, String?) -> Unit) {
        verifyStudentCode(email, code, autoLogin = false, callback = callback)
    }

    fun verifyStudentCode(
        email: String,
        code: String,
        autoLogin: Boolean,
        callback: (Boolean, String?) -> Unit
    ) {
        android.util.Log.d("AuthManager", "verifyStudentCode çağrıldı - email: $email, code: $code, autoLogin: $autoLogin")
        // Firestore'da kod kontrolü
        firestore.collection("studentVerificationCodes").document(code)
            .get()
            .addOnSuccessListener { doc ->
                android.util.Log.d("AuthManager", "verifyStudentCode - kod dokümanı okundu, exists: ${doc.exists()}")
                if (!doc.exists()) {
                    android.util.Log.e("AuthManager", "verifyStudentCode - kod dokümanı bulunamadı")
                    callback(false, "Geçersiz kod")
                    return@addOnSuccessListener
                }

                val docEmail = doc.getString("email") ?: ""
                val docUid = doc.getString("uid") ?: ""
                val used = doc.getBoolean("used") ?: false
                val expiresAt = doc.getTimestamp("expiresAt")
                val now = com.google.firebase.Timestamp.now()

                android.util.Log.d("AuthManager", "verifyStudentCode - kod bilgileri: email=$docEmail, uid=$docUid, used=$used, expiresAt=$expiresAt")

                if (used || (expiresAt != null && expiresAt < now) || docEmail != email) {
                    android.util.Log.e("AuthManager", "verifyStudentCode - kod geçersiz veya süresi dolmuş")
                    callback(false, "Geçersiz veya süresi dolmuş kod")
                    return@addOnSuccessListener
                }

                // Kodu kullanıldı işaretle
                android.util.Log.d("AuthManager", "verifyStudentCode - kodu kullanıldı işaretleniyor")
                firestore.collection("studentVerificationCodes").document(code)
                    .update(mapOf("used" to true, "verifiedAt" to now))
                    .addOnSuccessListener {
                        android.util.Log.d("AuthManager", "verifyStudentCode - kod kullanıldı işaretlendi")
                        // Eğer pending registration varsa (yeni kayıt), Firebase'e kayıt yap
                        if (docUid.startsWith("pending_")) {
                            android.util.Log.d("AuthManager", "verifyStudentCode - pending registration akışı başlatılıyor")
                            // Pending registration'dan bilgileri al
                            firestore.collection("pendingRegistrations").document(email)
                                .get()
                                .addOnSuccessListener { pendingDoc ->
                                    android.util.Log.d("AuthManager", "verifyStudentCode - pending registration okundu, exists: ${pendingDoc.exists()}")
                                    if (!pendingDoc.exists()) {
                                        android.util.Log.e("AuthManager", "verifyStudentCode - pending registration bulunamadı")
                                        callback(false, "Kayıt bilgileri bulunamadı")
                                        return@addOnSuccessListener
                                    }

                                    val name = pendingDoc.getString("name") ?: ""
                                    val password = pendingDoc.getString("password") ?: ""

                                    fun completeRegistration(user: com.google.firebase.auth.FirebaseUser) {
                                        android.util.Log.d("AuthManager", "verifyStudentCode - completeRegistration çağrıldı, uid: ${user.uid}")
                                        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                                        user.updateProfile(profileUpdates)
                                        generateUniqueUserId(ROLE_STUDENT) { userId ->
                                            val userData = mapOf(
                                                "uid" to user.uid,
                                                "userId" to userId,
                                                "email" to email,
                                                "name" to name,
                                                "role" to ROLE_STUDENT,
                                                "verified" to true,
                                                "createdAt" to com.google.firebase.Timestamp.now()
                                            )
                                            android.util.Log.d("AuthManager", "verifyStudentCode - users collection'a yazılıyor, uid: ${user.uid}")
                                            firestore.collection("users").document(user.uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    android.util.Log.d("AuthManager", "verifyStudentCode - users collection'a yazıldı")
                                                    firestore.collection("pendingRegistrations").document(email).delete()
                                                    if (!autoLogin) auth.signOut()
                                                    else cacheBasicUser(email, ROLE_STUDENT, name, userId)
                                                    callback(true, null)
                                                }
                                                .addOnFailureListener { e ->
                                                    android.util.Log.e("AuthManager", "verifyStudentCode - users collection'a yazma hatası: ${e.message}", e)
                                                    callback(false, e.localizedMessage ?: "Kayıt tamamlanamadı: ${e.message}")
                                                }
                                        }
                                    }

                                    // Önce Firebase Auth'da hesap oluşturmayı dene
                                    android.util.Log.d("AuthManager", "verifyStudentCode - Firebase Auth'da hesap oluşturuluyor")
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                android.util.Log.d("AuthManager", "verifyStudentCode - Firebase Auth'da hesap oluşturuldu")
                                                val user = auth.currentUser
                                                if (user != null) completeRegistration(user)
                                                else {
                                                    android.util.Log.e("AuthManager", "verifyStudentCode - auth.currentUser null")
                                                    callback(false, "Kullanıcı oluşturulamadı")
                                                }
                                                return@addOnCompleteListener
                                            }
                                            // "Bu e-posta zaten kullanılıyor" -> önceki denemede Auth oluşmuş olabilir; giriş yapıp kaydı tamamla
                                            val ex = task.exception
                                            android.util.Log.e("AuthManager", "verifyStudentCode - Firebase Auth hesap oluşturma hatası: ${ex?.message}", ex)
                                            val isAlreadyInUse = ex is FirebaseAuthUserCollisionException ||
                                                (ex?.message?.contains("already in use", ignoreCase = true) == true)
                                            if (!isAlreadyInUse) {
                                                callback(false, ex?.localizedMessage ?: "Hesap oluşturulamadı")
                                                return@addOnCompleteListener
                                            }
                                            android.util.Log.d("AuthManager", "verifyStudentCode - email zaten kullanılıyor, giriş yapılıyor")
                                            auth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener { signInTask ->
                                                    if (!signInTask.isSuccessful) {
                                                        android.util.Log.e("AuthManager", "verifyStudentCode - giriş hatası: ${signInTask.exception?.message}")
                                                        callback(false, "Bu e-posta başka bir hesapta kullanılıyor. Giriş ekranından giriş yapın.")
                                                        return@addOnCompleteListener
                                                    }
                                                    val user = auth.currentUser
                                                    if (user == null) {
                                                        android.util.Log.e("AuthManager", "verifyStudentCode - giriş sonrası currentUser null")
                                                        callback(false, "Kullanıcı bulunamadı")
                                                        return@addOnCompleteListener
                                                    }
                                                    // Firestore'da kullanıcı var mı kontrol et; yoksa oluştur
                                                    firestore.collection("users").document(user.uid).get()
                                                        .addOnSuccessListener { userDoc ->
                                                            if (userDoc.exists()) {
                                                                android.util.Log.d("AuthManager", "verifyStudentCode - kullanıcı zaten Firestore'da var")
                                                                firestore.collection("pendingRegistrations").document(email).delete()
                                                                if (!autoLogin) auth.signOut()
                                                                else {
                                                                    val uid = userDoc.getString("userId") ?: user.uid
                                                                    cacheBasicUser(email, ROLE_STUDENT, userDoc.getString("name") ?: "", uid)
                                                                }
                                                                callback(true, null)
                                                            } else {
                                                                android.util.Log.d("AuthManager", "verifyStudentCode - kullanıcı Firestore'da yok, oluşturuluyor")
                                                                completeRegistration(user)
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            android.util.Log.e("AuthManager", "verifyStudentCode - Firestore kullanıcı kontrolü hatası: ${e.message}", e)
                                                            callback(false, e.localizedMessage ?: "Kullanıcı kontrolü başarısız: ${e.message}")
                                                        }
                                                }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("AuthManager", "verifyStudentCode - pending registration okuma hatası: ${e.message}", e)
                                    callback(false, e.localizedMessage ?: "Kayıt bilgileri alınamadı: ${e.message}")
                                }
                        } else {
                            // Mevcut kullanıcı için sadece verified durumunu güncelle
                            android.util.Log.d("AuthManager", "verifyStudentCode - mevcut kullanıcı için verified güncelleniyor, uid: $docUid")
                            firestore.collection("users").document(docUid)
                                .update(mapOf("verified" to true))
                                .addOnSuccessListener {
                                    android.util.Log.d("AuthManager", "verifyStudentCode - verified güncellendi")
                                    callback(true, null)
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("AuthManager", "verifyStudentCode - verified güncelleme hatası: ${e.message}", e)
                                    callback(false, e.localizedMessage ?: "Doğrulama güncellenemedi: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("AuthManager", "verifyStudentCode - kod güncelleme hatası: ${e.message}", e)
                        callback(false, e.localizedMessage ?: "Kod güncellenemedi: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthManager", "verifyStudentCode - kod okuma hatası: ${e.message}", e)
                callback(false, e.localizedMessage ?: "Kod okunamadı: ${e.message}")
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
