package de.luhmer.chickenmonitor.call

import android.content.Context
import de.luhmer.chickenmonitor.BuildConfig
import de.luhmer.chickenmonitor.events.StopRecordingEvent
import org.greenrobot.eventbus.EventBus

class CallHandler {

    fun markMessageAsRead(context: Context, phoneNumber: String) {
        WhatsApp().markChatAsRead(context, phoneNumber)
    }

    fun startCall(context: Context, phoneNumber: String?) {
        // GoogleDuo().startCall(context,  "+491234567890")

        // stop recording
        EventBus.getDefault().post(StopRecordingEvent())


        if(phoneNumber != null) {
            WhatsApp().makeVideoCall(context, phoneNumber)
        } else {
            WhatsApp().makeVideoCall(context, BuildConfig.PHONE_NUMBER)
        }

    }
}