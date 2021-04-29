package de.luhmer.chickenmonitor.db

import android.util.Log
import de.luhmer.chickenmonitor.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt


class InfluxDB {

    private val token = BuildConfig.INFLUX_DB_TOKEN
    private val org = BuildConfig.INFLUX_DB_ORG
    private val bucket = BuildConfig.INFLUX_DB_BUCKET
    private val TAG = "[InfluxDB]"

    fun uploadDataPoint(location: String, loudness: Float) {
        sendPost(location, loudness.roundToInt())

        /*
        // code below is not working on older Android Phones...

        val client = InfluxDBClientFactory.create(
            "https://eu-central-1-1.aws.cloud2.influxdata.com",
            token.toCharArray(),
        )

        val point: Point = Point.measurement("decibel")
            .addTag("location", location)
            .addField("value", loudness)
            .time(System.currentTimeMillis(), WritePrecision.MS)


        Log.d(TAG, "write data point to influxdb")
        client.writeApi.writePoint(bucket, org, point)
        client.close()
        */
    }

    fun sendPost(location: String, loudness: Int) {
        val reqParam = "rp=one_week&org=%s&bucket=%s&prescision=ms".format(
            URLEncoder.encode(
                org,
                "UTF-8"
            ), URLEncoder.encode(bucket, "UTF-8")
        )
        val mURL = URL(
            "https://eu-central-1-1.aws.cloud2.influxdata.com/api/v2/write?%s".format(
                reqParam
            )
        )

        // To ensure a data point includes the time a metric is observed (not received by InfluxDB), include the timestamp.
        val tx = "decibel,location=$location value=%d".format(loudness) // System.currentTimeMillis()

        Log.d(TAG, tx)

        val auth = "Token %s".format(token)
        Log.d(TAG, auth)

        val txByteArray = tx.toByteArray()


        with(mURL.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true

            setRequestProperty("Authorization", auth)
            setRequestProperty("Content-Type", "text/plain; utf-8")
            setRequestProperty("Content-Length", txByteArray.size.toString())
            setRequestProperty("Accept", "application/json")

            Log.d(TAG, requestProperties.toString())


            outputStream.write(txByteArray)
            outputStream.close()

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response : $response")

            }
        }
    }
}