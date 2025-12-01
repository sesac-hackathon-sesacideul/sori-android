package com.misterjerry.test01.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.misterjerry.test01.MainActivity
import com.misterjerry.test01.R
import com.misterjerry.test01.data.AudioClassifierHelper
import com.misterjerry.test01.data.SoundEventBus
import com.misterjerry.test01.data.Urgency
import com.misterjerry.test01.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SoundDetectionService : Service() {

    private var audioClassifierHelper: AudioClassifierHelper? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var vibrationHelper: VibrationHelper

    companion object {
        const val CHANNEL_ID = "SoundDetectionChannel"
        const val NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrationHelper = VibrationHelper(this)
        audioClassifierHelper = AudioClassifierHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        
        startListening()
        
        return START_STICKY
    }

    private fun startListening() {
        audioClassifierHelper?.startAudioClassification()
        
        serviceScope.launch {
            audioClassifierHelper?.classificationFlow?.collect { (label, direction) ->
                // Broadcast to UI via EventBus (Legacy flow)
                SoundEventBus.emitEvent(label, direction)
                
                // Handle Background Alerts & Update State
                handleSoundDetection(label, direction)
            }
        }
    }

    private fun handleSoundDetection(label: String, direction: Float) {
        val result = getUrgency(label) ?: return // Ignore unmapped sounds
        val (koreanLabel, urgency) = result
        
        // Create Event
        val newEvent = com.misterjerry.test01.data.SoundEvent(
            id = System.currentTimeMillis(),
            name = koreanLabel,
            direction = direction,
            distance = (1..10).random().toFloat(), // Random distance for demo
            urgency = urgency
        )
        
        // Update Global State
        SoundEventBus.addEvent(newEvent)

        // Vibration Logic
        if (!SoundEventBus.isForeground) {
            // Background: Only vibrate for High/Medium
            if (urgency == Urgency.HIGH || urgency == Urgency.MEDIUM) {
                vibrationHelper.vibrate(com.misterjerry.test01.data.VibrationPattern.DEFAULT)
            }
        } else {
            // Foreground: Handled by ViewModel/UI based on settings
        }

        if (urgency == Urgency.HIGH || urgency == Urgency.MEDIUM) {
            // Show Notification (Always show for High/Med even in foreground? 
            // Usually notifications are for background, but heads-up might be desired.
            // Let's keep it for now, or restrict to background if user wants.)
            // User said "Background alarm", so maybe only background?
            // But usually services show notifications. 
            // Let's keep notification logic as is for now (it's a foreground service anyway).
            showAlertNotification(koreanLabel, urgency)
        }
    }

    
    private fun getUrgency(label: String): Pair<String, Urgency>? {
         return when (label) {
            // Safety (High Urgency)
            "Siren", "Ambulance (siren)", "Fire engine, fire truck (siren)", "Emergency vehicle" -> "사이렌" to Urgency.HIGH
            "Car horn, honking" -> "자동차 경적" to Urgency.HIGH
            "Baby cry, infant cry" -> "아기 울음소리" to Urgency.HIGH
            "Smoke detector, smoke alarm" -> "화재 경보기" to Urgency.HIGH
            "Glass" -> "유리 깨지는 소리" to Urgency.HIGH
            "Scream" -> "비명 소리" to Urgency.HIGH

            // Alerts / Communication (Medium Urgency)
            "Doorbell" -> "초인종 소리" to Urgency.MEDIUM
            "Telephone", "Ringtone" -> "전화 벨소리" to Urgency.MEDIUM
            "Alarm" -> "알람 소리" to Urgency.MEDIUM
            "Dog", "Bark" -> "개 짖는 소리" to Urgency.MEDIUM
            
            // Daily Life (Low Urgency)
            "Clapping", "Hands" -> "박수 소리" to Urgency.LOW
            "Knock" -> "노크 소리" to Urgency.LOW
            "Finger snapping" -> "핑거 스냅" to Urgency.LOW
            "Speech" -> "말소리" to Urgency.LOW
            "Water tap, faucet" -> "물 틀어놓은 소리" to Urgency.LOW
            "Toilet flush" -> "변기 물 내리는 소리" to Urgency.LOW
            "Microwave oven" -> "전자레인지 소리" to Urgency.LOW
            "Cat", "Meow" -> "고양이 울음소리" to Urgency.LOW

            else -> null // Ignore other sounds
        }
    }

    private fun showAlertNotification(label: String, urgency: Urgency) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with appropriate icon
            .setContentTitle("소리 감지됨: $label")
            .setContentText("${if(urgency == Urgency.HIGH) "위험" else "알림"}: 주변에서 $label 소리가 감지되었습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("환경 소리 감지 중")
            .setContentText("백그라운드에서 소리를 듣고 있습니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sound Detection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioClassifierHelper?.stopAudioClassification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
