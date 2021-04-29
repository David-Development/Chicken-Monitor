package de.luhmer.chickenmonitor

import android.Manifest.permission
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.luhmer.chickenmonitor.call.CallHandler
import de.luhmer.chickenmonitor.databinding.ActivityMainBinding
import de.luhmer.chickenmonitor.events.RecordingStateEvent
import de.luhmer.chickenmonitor.events.StartRecordingEvent
import de.luhmer.chickenmonitor.events.StopRecordingEvent
import de.luhmer.chickenmonitor.events.UpdateUIEvent
import de.luhmer.chickenmonitor.services.RecordingService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val TAG = "[MainActivity]"

    private var PERMISSIONS = arrayOf(
        permission.WRITE_EXTERNAL_STORAGE,
        permission.RECORD_AUDIO,
        permission.READ_CONTACTS,
        permission.CALL_PHONE,
        permission.FOREGROUND_SERVICE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        var permissionMissing = false
        PERMISSIONS.forEach { perm ->
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED -> {
                   permissionMissing = true
                }
            }
        }

        if(permissionMissing) {
            // You can directly ask for the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, 123)
            }
        } else {
            // ...
        }

        // Check if the notification lister is enabled
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        if(packages.contains("de.luhmer.chickenmonitor")){
            Log.d(TAG, "package listener already enabled - nothing to do!")
        } else {
            Log.d(TAG, "ask for permission to listen to notifiactions")
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        binding.startService.setOnClickListener { EventBus.getDefault().post(StartRecordingEvent()) }
        binding.stopService.setOnClickListener { EventBus.getDefault().post(StopRecordingEvent()) }

        binding.startTestCall.setOnClickListener { CallHandler().startCall(this, null) }

        binding.downloadUpdate.setOnClickListener {
            val url = BuildConfig.DOWNLOAD_URL
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }



        /*
        binding.sendTestData.setOnClickListener {
            (Thread() {
                InfluxDB().uploadDataPoint(60F)
            }).start()
        }
        */

        updateLocationAndThreshold()


        binding.updateLocation.setOnClickListener { openDialog("Location", InputType.TYPE_CLASS_TEXT) {
                val sp = getSharedPrefs()
                val loc = it.replace(" ", "") // remove all white-spaces
                sp.edit().putString("LOCATION", loc).commit()
                 updateLocationAndThreshold()
            }
        }

        binding.updateThreshold.setOnClickListener { openDialog("Threshold", InputType.TYPE_CLASS_NUMBER) {
                val sp = getSharedPrefs()
                sp.edit().putInt("THRESHOLD", it.toInt()).commit()
                updateLocationAndThreshold()
            }
        }
    }

    fun openDialog(title: String, inputType: Int, callback: (String) -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val input = EditText(this)
        input.inputType = inputType
        builder.setView(input)
        builder.setPositiveButton("OK"
        ) { dialog, which ->
            val loc = input.text.toString()
            callback(loc)
        }
        builder.setNegativeButton("Cancel"
        ) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun getSharedPrefs(): SharedPreferences {
        return getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
    }

    private fun updateLocationAndThreshold() {
        val sp = getSharedPrefs()
        val location = sp.getString("LOCATION", "test")
        binding.location.text = location

        val threshold = sp.getInt("THRESHOLD", 85)
        binding.threshold.text = threshold.toString()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            123 -> {
                //startRecording()
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: StartRecordingEvent) {

        // Start service now
        val serviceIntent = Intent(this, RecordingService::class.java)
        // serviceIntent.putExtra("THRESHOLD", threshold)
        ContextCompat.startForegroundService(this, serviceIntent)

        /*

        // Update threshold first
        val queue = Volley.newRequestQueue(this)
        val url = BuildConfig.CONFIG_URL
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                threshold = response.get("threshold") as Int
                binding.threshold.text = "Threshold: %s db".format(threshold)
                Log.d(TAG, response.toString())



            },
            { error ->
                binding.threshold.text = "Error: %s".format(error.toString())
                Log.e(TAG, error.toString())
            }
        )
        // Access the RequestQueue through your singleton class.
        queue.add(jsonObjectRequest)
        */
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: UpdateUIEvent) {
        this.binding.raw.text = "Raw: " + event.raw

        if(event.status != null) {
            this.binding.status.text = "Status: " + event.status
        }

        if(event.avg != null) {
            this.binding.avg.text = "Average: " + event.avg
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: RecordingStateEvent) {
        // Log.d(TAG, "RecordingStateEvent - recording: ${event.recording}")
        if (event.recording) {
            this.binding.recordingState.text = "RECORDING"
            this.binding.recordingState.setTextColor(Color.parseColor("#00FF00"));
        } else {
            this.binding.recordingState.text = "NOT RECORDING"
            this.binding.recordingState.setTextColor(Color.parseColor("#FF0000"));

            this.binding.raw.text = "-"
            this.binding.status.text = "-"
            this.binding.avg.text = "-"
            this.binding.status.text = "-"
            // this.binding.threshold.text = "-"
        }
    }
}