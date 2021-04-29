package de.luhmer.chickenmonitor.services

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import de.luhmer.chickenmonitor.MainActivity
import de.luhmer.chickenmonitor.call.CallHandler
import de.luhmer.chickenmonitor.events.StartRecordingEvent
import org.greenrobot.eventbus.EventBus


class NotificationListener : NotificationListenerService() {

    private val TAG = "[NotificationListener]"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        parseNotification(sbn, "Notification Posted")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        parseNotification(sbn, "Notification Removed")
    }

    fun parseNotification(sbn: StatusBarNotification, action: String) {
        val pack = sbn.packageName

        if(pack != "com.whatsapp") {
            Log.d(TAG, "Notification from package $pack")
            return
        }

        var ticker = ""
        if (sbn.notification.tickerText != null) {
            ticker = sbn.notification.tickerText.toString()
        }
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text").toString()
        //val id1 = extras.getInt(Notification.EXTRA_SMALL_ICON)
        //val id = sbn.notification.largeIcon
        Log.i(TAG, "############")
        Log.i(TAG, action)
        Log.i(TAG, "Package: $pack")
        Log.i(TAG, "Ticker: $ticker")
        Log.i(TAG, "Title: $title")
        Log.i(TAG, "Text: $text")

        if (text == "Call me" && action == "Notification Posted") {
            val phoneNumber = title!!.replace(" ", "").replace("+", "")
            Log.i(TAG, "call command detected")
            CallHandler().markMessageAsRead(this, phoneNumber)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    CallHandler().startCall(this, phoneNumber)
                },
                5000 // value in milliseconds
            )

        } else if((text == "Ongoing video call" || text == "Ringing…") && action == "Notification Removed") {
            // Ringing -> call not answered
            // Ongoing video call -> call ended

            Handler(Looper.getMainLooper()).postDelayed(
            {
                val i = Intent(this, MainActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                i.action = Intent.ACTION_MAIN
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                startActivity(i) // start app again

                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        // start recording again
                        EventBus.getDefault().post(StartRecordingEvent())
                    }, 3000)
            }, 2000)
        }
    }
}