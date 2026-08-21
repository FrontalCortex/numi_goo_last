const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/example/app/auth/AuthManager.kt', 'utf8');

const startMarker = '    fun verifyStudentCode(\r\n        email: String,\r\n        code: String,\r\n        autoLogin: Boolean,\r\n        callback: (Boolean, String?) -> Unit\r\n    ) {';
const altStartMarker = '    fun verifyStudentCode(\n        email: String,\n        code: String,\n        autoLogin: Boolean,\n        callback: (Boolean, String?) -> Unit\n    ) {';

let startIndex = content.indexOf(startMarker);
if (startIndex === -1) startIndex = content.indexOf(altStartMarker);

if (startIndex === -1) {
  console.log('Start marker not found');
  process.exit(1);
}

let braces = 0;
let endIndex = -1;
let started = false;

for (let i = startIndex; i < content.length; i++) {
  if (content[i] === '{') {
    braces++;
    started = true;
  } else if (content[i] === '}') {
    braces--;
  }
  
  if (started && braces === 0) {
    endIndex = i;
    break;
  }
}

const newCode =     fun verifyStudentCode(
        email: String,
        code: String,
        autoLogin: Boolean,
        callback: (Boolean, String?) -> Unit
    ) {
        android.util.Log.d("AuthManager", "verifyStudentCode Cloud Function çağrılıyor - email: , code: , autoLogin: ")
        val payload = hashMapOf<String, Any>("email" to email, "code" to code)
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
    };

content = content.substring(0, startIndex) + newCode + content.substring(endIndex + 1);
fs.writeFileSync('app/src/main/java/com/example/app/auth/AuthManager.kt', content, 'utf8');
console.log('Successfully updated verifyStudentCode.');
