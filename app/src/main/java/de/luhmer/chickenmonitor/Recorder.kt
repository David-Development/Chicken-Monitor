package de.luhmer.chickenmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaRecorder
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.luhmer.chickenmonitor.call.CallHandler
import de.luhmer.chickenmonitor.db.InfluxDB
import de.luhmer.chickenmonitor.events.RecordingStateEvent
import de.luhmer.chickenmonitor.events.UpdateUIEvent
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.roundToInt


class Recorder(
    private val context: Context,
    private val threshold: Int,
    private val location: String
) {

    val TAG = "[Recorder]"
    private var mRecorder: MediaRecorder? = null
    private var fileCounter: Long = System.currentTimeMillis() / 1000L

    private val EMA_FILTER = 0.6
    private var mEMA = 0.0
    val mHandler: Handler = Handler()
    val updater: Runnable = Runnable { updateTv() }
    var runner: Thread? = null
    private var valuesAvg: MutableList<Double> = ArrayList<Double>()
    private var timestamp: Long = System.currentTimeMillis() / 1000L
    private var lastTimestamp: Long = System.currentTimeMillis() / 1000L

    private var placedCallForCurrentAlarm = false


    private var keepRecording = false


    fun startRecording() {
        stopRecorder()

        Log.d(TAG, "startRecording - threshold is %d".format(threshold))

        mRecorder = MediaRecorder()
        startRecorder()

        runner = object : java.lang.Thread() {
            override fun run() {
                while (runner != null) {
                    EventBus.getDefault().post(RecordingStateEvent(true))
                    mHandler.post(updater)
                    try {
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        Log.w(TAG, "received InterruptedException")
                    }
                }
                Log.w(TAG, "exit recording..")
            }
        }
        (runner as Thread).start()
        Log.d(TAG, "start runner()")
    }



    private fun getOutputFile(): File {
        if(keepRecording) {
            fileCounter = fileCounter++
            Log.d(TAG, "Increased counter - keep file!!")

            keepRecording = false
        }


        val sdCard: File = this.context.externalCacheDir!!
        val dir = File(sdCard.absolutePath.toString() + "/ChickenMonitor")
        dir.mkdirs()
        val file = File(dir, "$fileCounter.aac")
        Log.d(TAG, "Output$file")
        return file

    }

    private fun startRecorder() {
        val rec = mRecorder!!
        rec.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        //rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        //rec.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        //rec.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        rec.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        //rec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //rec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        /*
        rec.setOnInfoListener { _, what, _ ->
            if (what == MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING) {
            //if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val newFilename = getOutputFile().absoluteFile
                    Log.d(TAG, "MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING switching files now. New file: $newFilename")
                    rec.setNextOutputFile(newFilename)
                }
            }
        }
        rec.setMaxFileSize(100000L)

        val initialFileName = getOutputFile().absolutePath
        rec.setOutputFile(initialFileName)
        Log.d(TAG, "Initial-Filename: $initialFileName")
         */
        rec.setOutputFile("/dev/null")


        try {
            rec.prepare()
        } catch (ioe: IOException) {
            Log.e(
                "[Monkey]", "IOException: " +
                        Log.getStackTraceString(ioe)
            )
        } catch (e: SecurityException) {
            Log.e(
                "[Monkey]", "SecurityException: " +
                        Log.getStackTraceString(e)
            )
        }
        try {
            rec.start()
        } catch (e: SecurityException) {
            Log.e(
                "[Monkey]", "SecurityException: " +
                        Log.getStackTraceString(e)
            )
        }
    }


    fun stopRecorder() {
        Log.d(TAG, "stopRecorder")
        mRecorder?.stop()
        mRecorder?.release()
        mRecorder = null
        runner?.interrupt()
        runner = null

        EventBus.getDefault().post(RecordingStateEvent(false))
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateTv() {
        val raw = getAmplitudeEMA().roundToInt().toString() + " dB";
        val output = UpdateUIEvent(raw)

        val amplitude: Int = getAmplitude()

        if (amplitude in 1..999999) {
            val dbl = convertdDb(amplitude)
            val dblString = String.format("%.2f", dbl) + "dB"
            output.status = dblString

            Log.i(TAG, dblString)

            valuesAvg.add(dbl)
            lastTimestamp = System.currentTimeMillis() / 1000L
            if (lastTimestamp - timestamp > 60) {
                var sum = 0.0
                var count = 0
                for (value in valuesAvg) {
                    count++
                    sum += value
                }
                valuesAvg = ArrayList<Double>()
                timestamp = lastTimestamp
                val average = sum.toFloat() / count
                output.avg = String.format("%.2f", average) + "dB"






                if(average > threshold) {
                    Log.w(TAG, "Threshold breached!")
                    keepRecording = true

                    if(!placedCallForCurrentAlarm) {
                        placedCallForCurrentAlarm = true

                        val timePattern = "HH:mm:ss"
                        val formatter = SimpleDateFormat(timePattern)
                        val dateNow = formatter.parse(formatter.format(Calendar.getInstance().time))
                        // Silent hours
                        val dateFrom: Date = formatter.parse("00:00:00")
                        val dateTo: Date = formatter.parse("11:00:00")

                        Log.d(TAG, dateNow.toString())
                        Log.d(TAG, dateFrom.toString())
                        Log.d(TAG, dateTo.toString())

                        if (dateFrom.before(dateNow) && dateTo.after(dateNow)) {
                            Log.d(TAG, "Not calling - silent time!")
                        } else {
                            Log.d(TAG, "Making video call now")
                            CallHandler().startCall(context, null)
                        }
                    }
                } else {
                    // reset call flag once the volume is below threshold again
                    placedCallForCurrentAlarm = false
                }

                Thread {
                    Log.d(TAG, "Upload data to InfluxDB")
                    InfluxDB().uploadDataPoint(location, average)
                }.start()
            }
        } /* else {
            Log.i(TAG, String.format("Noise out of range %s dB", amplitude))
        } */

        // Log.d(TAG, output.toString())
        EventBus.getDefault().post(output)
    }

    // https://github.com/linuxluigi/OpenDecibelMeter/blob/master/app/src/main/java/com/linuxluigi/opendecibelmeter/MeasureActivity.java

    fun soundDb(ampl: Double): Double {
        return (20 * log10(getAmplitudeEMA() / ampl).toFloat()).toDouble()
    }

    private fun convertdDb(amplitude: Int): Double {
        // Cellphones can catch up to 90 db + -
        // getMaxAmplitude returns a value between 0-32767 (in most phones). that means that if the maximum db is 90, the pressure
        // at the microphone is 0.6325 Pascal.
        // it does a comparison with the previous value of getMaxAmplitude.
        // we need to divide maxAmplitude with (32767/0.6325)
        //51805.5336 or if 100db so 46676.6381
        val EMA_FILTER = 0.6
        val sp: SharedPreferences = this.context.getSharedPreferences(
            "device-base",
            AppCompatActivity.MODE_PRIVATE
        )
        val amp = sp.getFloat("amplitude", 0F).toDouble()
        val mEMAValue = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA
        // Log.d(TAG, amp.toString())
        //Assuming that the minimum reference pressure is 0.000085 Pascal (on most phones) is equal to 0 db
        // samsung S9 0.000028251
        return (20 * log10(mEMAValue / 51805.5336 / 0.000028251).toFloat()).toDouble()
    }


    private fun getAmplitude(): Int {
        if(mRecorder == null) {
            Log.w(TAG, "mRecorder is null - this should not happen!!!")
            return 0
        } else {
            val maxAmplitude = mRecorder!!.maxAmplitude
            // Log.d(TAG, "maxAmplitude: $maxAmplitude")
            return maxAmplitude
        }
    }

    private fun getAmplitudeEMA(): Double {
        val amp = getAmplitude()
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA
    }

}