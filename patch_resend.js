const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/example/app/auth/AuthManager.kt', 'utf8');

const startMarker = '    fun resendStudentVerificationCode(email: String, callback: (Boolean, String?) -> Unit) {';
const altStartMarker = startMarker;

let startIndex = content.indexOf(startMarker);
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

const newCode =     fun resendStudentVerificationCode(email: String, isRegistration: Boolean = false, callback: (Boolean, String?) -> Unit) {
        if (isRegistration) {
            val tempUid = "pending_\"
            sendStudentVerificationCode(email, tempUid) { success, error ->
                callback(success, error)
            }
        } else {
            firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents[0]
                        val role = doc.getString("role") ?: ""
                        if (role == ROLE_TEACHER) {
                            callback(false, "E-posta hatalı.")
                            return@addOnSuccessListener
                        }
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
    };

content = content.substring(0, startIndex) + newCode + content.substring(endIndex + 1);
fs.writeFileSync('app/src/main/java/com/example/app/auth/AuthManager.kt', content, 'utf8');
console.log('Successfully updated resendStudentVerificationCode.');
