package com.example.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

fun Context.isOnline(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo
        @Suppress("DEPRECATION")
        info != null && info.isConnected
    }
}

fun AppCompatActivity.requireOnlineOrShowOffline(onOnline: () -> Unit) {
    if (isOnline()) {
        onOnline()
    } else if (this is MainActivity) {
        showOfflineFragment()
    } else {
        Toast.makeText(this, "İnternet bağlantınız yok.", Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.requireOnlineOrShowOffline(onOnline: () -> Unit) {
    val ctx = context ?: return
    if (ctx.isOnline()) {
        onOnline()
    } else {
        (activity as? MainActivity)?.showOfflineFragment()
            ?: Toast.makeText(ctx, "İnternet bağlantınız yok.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Hem internet hem de aktif kullanıcı gerektiren aksiyonlar için:
 * - Offline ise: OfflineFragment gösterilir.
 * - Online ama login değilse: LoginStartActivity açılır.
 * - Online + login varsa: onOnline bloğu çalışır.
 */
fun AppCompatActivity.requireOnlineAndLoggedInOrLogin(onOnline: () -> Unit) {
    if (!isOnline()) {
        if (this is MainActivity) {
            showOfflineFragment()
        } else {
            Toast.makeText(this, "İnternet bağlantınız yok.", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        val intent = Intent(this, LoginStartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        return
    }

    onOnline()
}

fun Fragment.requireOnlineAndLoggedInOrLogin(onOnline: () -> Unit) {
    val ctx = context ?: return
    if (!ctx.isOnline()) {
        (activity as? MainActivity)?.showOfflineFragment()
            ?: Toast.makeText(ctx, "İnternet bağlantınız yok.", Toast.LENGTH_SHORT).show()
        return
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        val act = activity ?: return
        val intent = Intent(act, LoginStartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        act.startActivity(intent)
        return
    }

    onOnline()
}


