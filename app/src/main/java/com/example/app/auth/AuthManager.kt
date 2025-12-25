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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
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
            android.util.Log.e("AuthManager", "Web Client ID ayarlanmamış! Firebase Console'dan Web Client ID'yi alıp strings.xml'e ekleyin.")
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
    
    fun signInWithGoogle(account: GoogleSignInAccount, autoRegister: Boolean = false, callback: (Boolean, String?) -> Unit) {
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
                    val errorCode = (exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
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
                        errorMessageFull.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) -> {
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
                            
                            if (role == ROLE_TEACHER) {
                                // Öğretmen girişi
                                cacheBasicUser(
                                    user.email ?: "",
                                    ROLE_TEACHER,
                                    user.displayName ?: ""
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
                                                user.displayName ?: ""
                                            )
                                            callback(true, null)
                                        }
                                } else {
                                    cacheBasicUser(
                                        user.email ?: "",
                                        ROLE_STUDENT,
                                        user.displayName ?: ""
                                    )
                                    callback(true, null)
                                }
                            }
                        } else {
                            // Firestore'da kullanıcı yok
                            if (autoRegister) {
                                // Kayıt ekranından çağrıldıysa otomatik kayıt yap
                                android.util.Log.d("AuthManager", "Kayıt ekranından çağrıldı, otomatik kayıt yapılıyor")
                                val userData = mapOf(
                                    "uid" to user.uid,
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
                                            user.displayName ?: ""
                                        )
                                        callback(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        callback(false, e.localizedMessage)
                                    }
                            } else {
                                // Giriş ekranından çağrıldıysa kayıt ekranına yönlendir
                                android.util.Log.w("AuthManager", "Firestore'da kullanıcı bulunamadı ama Firebase Auth'da var. Kullanıcı çıkış yaptırılıyor.")
                                
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
    
    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>, autoRegister: Boolean = false, callback: (Boolean, String?) -> Unit) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                android.util.Log.d("AuthManager", "Google hesabı seçildi: ${account.email}, autoRegister: $autoRegister")
                signInWithGoogle(account, autoRegister, callback)
            } else {
                android.util.Log.e("AuthManager", "Google hesabı null")
                callback(false, "Google hesabı seçilemedi")
            }
        } catch (e: ApiException) {
            android.util.Log.e("AuthManager", "Google sign in failed - Status: ${e.statusCode}, Message: ${e.message}", e)
            
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
                            if (!verified) {
                                callback(false, "E-posta doğrulaması gerekli. Lütfen e-postanızı doğrulayın.")
        } else {
                                cacheBasicUser(email, ROLE_STUDENT, user.displayName ?: "")
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
                fetchUserRole(user.uid) { role ->
                    if (role == ROLE_TEACHER) {
                        cacheBasicUser(email, role, user.displayName ?: "")
                        callback(true, null)
        } else {
                        callback(false, "Bu hesap öğretmen değil")
                    }
                }
            }
    }

    fun registerStudent(email: String, name: String, password: String, callback: (Boolean, String?) -> Unit) {
        android.util.Log.d("AuthManager", "registerStudent çağrıldı - email: $email, name: $name")
        
        // Direkt Firebase Auth'da hesap oluştur
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    val errorMessage = when {
                        exception?.message?.contains("email address is already in use", ignoreCase = true) == true -> 
                            "Bu e-posta adresi zaten kayıtlı. Lütfen giriş yapın."
                        exception?.message?.contains("weak password", ignoreCase = true) == true -> 
                            "Şifre çok zayıf. Daha güçlü bir şifre seçin."
                        exception?.message?.contains("invalid email", ignoreCase = true) == true -> 
                            "Geçersiz e-posta adresi."
                        else -> exception?.localizedMessage ?: "Kayıt başarısız. Lütfen tekrar deneyin."
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
                user.updateProfile(userProfileChangeRequest { displayName = name })
                
                // Firestore'a kullanıcı dokümanı yaz (verified: false - e-posta doğrulanmamış)
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "name" to name,
                    "role" to ROLE_STUDENT,
                    "verified" to false, // E-posta doğrulanmamış - profil ekranında doğrulanacak
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("users").document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        android.util.Log.d("AuthManager", "Kullanıcı başarıyla kaydedildi - verified: false")
                        cacheBasicUser(email, ROLE_STUDENT, name)
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("AuthManager", "Firestore'a kayıt hatası: ${e.message}", e)
                        // Firebase Auth'da hesap oluşturuldu ama Firestore'a yazılamadı
                        // Kullanıcıyı sil
                        user.delete()
                        callback(false, "Kayıt başarısız: ${e.localizedMessage}")
                    }
            }
    }

    fun registerTeacher(email: String, name: String, password: String, approvalCode: String, callback: (Boolean, String?) -> Unit) {
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

                        user.updateProfile(userProfileChangeRequest { displayName = name })

                        val data = mapOf(
                            "uid" to user.uid,
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
                                    .update(mapOf(
                                        "used" to true,
                                        "usedByUid" to user.uid,
                                        "usedAt" to com.google.firebase.Timestamp.now()
                                    ))
                                cacheBasicUser(email, ROLE_TEACHER, name)
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                callback(false, e.localizedMessage)
                            }
                    }
            }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
    }

    private fun fetchUserRole(uid: String, callback: (String) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                callback(doc.getString("role") ?: "")
            }
            .addOnFailureListener { callback("") }
    }

    fun requestTeacherInviteCode(candidateEmail: String?, callback: (Boolean, String?) -> Unit) {
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

    private fun sendStudentVerificationCode(email: String, uid: String, callback: (Boolean, String?) -> Unit) {
        val payload = hashMapOf<String, Any>(
            "email" to email,
            "uid" to uid
        )
        
        android.util.Log.d("AuthManager", "sendStudentVerificationCode çağrıldı - email: $email, uid: $uid")
        
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sendStudentVerificationCode")
            .call(payload)
            .addOnSuccessListener { result ->
                android.util.Log.d("AuthManager", "sendStudentVerificationCode başarılı")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuthManager", "sendStudentVerificationCode hatası: ${e.message}", e)
                callback(false, e.localizedMessage ?: "Kod gönderilemedi: ${e.message}")
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
        // Firestore'da kod kontrolü
        firestore.collection("studentVerificationCodes").document(code)
                .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    callback(false, "Geçersiz kod")
                    return@addOnSuccessListener
                }
                
                val docEmail = doc.getString("email") ?: ""
                val docUid = doc.getString("uid") ?: ""
                val used = doc.getBoolean("used") ?: false
                val expiresAt = doc.getTimestamp("expiresAt")
                val now = com.google.firebase.Timestamp.now()
                
                if (used || (expiresAt != null && expiresAt < now) || docEmail != email) {
                    callback(false, "Geçersiz veya süresi dolmuş kod")
                    return@addOnSuccessListener
                }
                
                // Kodu kullanıldı işaretle
                firestore.collection("studentVerificationCodes").document(code)
                    .update(mapOf("used" to true, "verifiedAt" to now))
                    .addOnSuccessListener {
                        // Eğer pending registration varsa (yeni kayıt), Firebase'e kayıt yap
                        if (docUid.startsWith("pending_")) {
                            // Pending registration'dan bilgileri al
                            firestore.collection("pendingRegistrations").document(email)
                                .get()
                                .addOnSuccessListener { pendingDoc ->
                                    if (!pendingDoc.exists()) {
                                        callback(false, "Kayıt bilgileri bulunamadı")
                                        return@addOnSuccessListener
                                    }
                                    
                                    val name = pendingDoc.getString("name") ?: ""
                                    val password = pendingDoc.getString("password") ?: ""
                                    
                                    // Firebase'e kayıt yap
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
                                            
                                            // Display name ayarla
                                            user.updateProfile(userProfileChangeRequest { displayName = name })
                                            
                                            // Firestore'a kullanıcı dokümanı yaz
                                            val userData = mapOf(
                                                "uid" to user.uid,
                                                "email" to email,
                                                "name" to name,
                                                "role" to ROLE_STUDENT,
                                                "verified" to true,
                                                "createdAt" to com.google.firebase.Timestamp.now()
                                            )
                                            
                                            firestore.collection("users").document(user.uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    // Pending registration'ı sil
                                                    firestore.collection("pendingRegistrations").document(email).delete()
                                                    // Oturumu kapat (kullanıcı login ekranından giriş yapacak)
                                                    auth.signOut()
                                                    callback(true, null)
                                                }
                                                .addOnFailureListener { e ->
                                                    callback(false, e.localizedMessage)
                                                }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.localizedMessage)
                }
        } else {
                            // Mevcut kullanıcı için sadece verified durumunu güncelle
                            firestore.collection("users").document(docUid)
                                .update(mapOf("verified" to true))
                                .addOnSuccessListener {
                                    callback(true, null)
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.localizedMessage)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.localizedMessage)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.localizedMessage)
            }
    }
    
    fun logout() {
        auth.signOut()
        prefs.edit().clear().apply()
    }

    private fun cacheBasicUser(email: String, role: String, name: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_type", role)
            .putString("user_name", name)
            .apply()
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
