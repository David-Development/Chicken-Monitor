package de.luhmer.chickenmonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.luhmer.chickenmonitor.events.StartRecordingEvent
import de.luhmer.chickenmonitor.events.StopRecordingEvent
import org.greenrobot.eventbus.EventBus

class StartStopReceiver : BroadcastReceiver() {

    private val TAG = "[StartStopReceiver]"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action === "start") {
            Log.d(TAG, "start")
            EventBus.getDefault().post(StartRecordingEvent())
        } else if (intent.action === "stop") {
            Log.d(TAG, "stop")
            EventBus.getDefault().post(StopRecordingEvent())
        }
    }
}