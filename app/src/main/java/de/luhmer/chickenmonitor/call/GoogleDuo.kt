package de.luhmer.chickenmonitor.call

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class GoogleDuo {

    val TAG = "[GoogleDuo]"

    // https://stackoverflow.com/a/60392824/13370504
    fun startCall(context: Context, phoneNumber: String){
        val intent = Intent()
        intent.setPackage("com.google.android.apps.tachyon")
        intent.action = "com.google.android.apps.tachyon.action.CALL"
        intent.data = Uri.parse("tel:$phoneNumber")


        Log.d(TAG, "start call!")
        context.startActivity(intent)
    }
}