package de.luhmer.chickenmonitor.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import de.luhmer.chickenmonitor.MainActivity
import de.luhmer.chickenmonitor.R
import de.luhmer.chickenmonitor.Recorder
import de.luhmer.chickenmonitor.events.StopRecordingEvent
import de.luhmer.chickenmonitor.receiver.StartStopReceiver
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class RecordingService : Service() {

    lateinit var recorder: Recorder


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        // val startIntent = Intent(this, StartStopReceiver::class.java).apply { action = "start" }
        val stopIntent = Intent(this, StartStopReceiver::class.java).apply { action = "stop" }

        // val startPendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, startIntent, 0)
        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("Chicken Monitor")
            .setContentText("Recoding..")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            // .addAction(R.drawable.ic_launcher_foreground, "Start", startPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()

        startForeground(1, notification)


        val sp = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        val location = sp.getString("LOCATION", "test")
        val threshold = sp.getInt("THRESHOLD", 85)

        recorder = Recorder(this, threshold, location!!)
        recorder.startRecording()


        return START_NOT_STICKY
        //return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    /*
    @Subscribe()
    fun onMessageEvent(event: StartRecordingEvent) {
        this.recorder.startRecording()
    }
    */

    @Subscribe()
    fun onMessageEvent(event: StopRecordingEvent) {
        this.recorder.stopRecorder()

        stopSelf()
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val CHANNEL_ID = "RecordingServiceChannel"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }
}