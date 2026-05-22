package com.example.receiver

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import android.util.Log

object AlarmSoundManager {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun startRinging(context: Context, vibrate: Boolean) {
        stopRinging()
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            if (vibrate) {
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = longArrayOf(0, 800, 800)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0) // Looping vibration at index 0
            }
            Log.d("AlarmSoundManager", "Ringing started successfully")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error starting sound alarm", e)
        }
    }

    fun stopRinging() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            
            vibrator?.cancel()
            vibrator = null
            Log.d("AlarmSoundManager", "Ringing stopped successfully")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error stopping alert sound", e)
        }
    }
}
