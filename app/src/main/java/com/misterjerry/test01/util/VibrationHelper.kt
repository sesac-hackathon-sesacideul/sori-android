package com.misterjerry.test01.util

import com.misterjerry.test01.data.Urgency
import com.misterjerry.test01.data.VibrationPattern
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate(pattern: VibrationPattern) {
        if (pattern == VibrationPattern.NONE) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (pattern) {
                VibrationPattern.DEFAULT -> {
                    // 기본: 0.2초 진동, 0.1초 대기, 0.2초 진동 (다급함/강조) - 기존 HIGH 패턴 유사
                    val timings = longArrayOf(0, 200, 100, 200)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                }
                VibrationPattern.FAST -> {
                    // 빠르게: 0.1초 진동, 0.1초 대기, 0.1초 진동
                    val timings = longArrayOf(0, 100, 100, 100)
                    val amplitudes = intArrayOf(0, 200, 0, 200)
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                }
                VibrationPattern.NONE -> null // Should not happen due to early return
            }
            effect?.let { vibrator.vibrate(it) }
        } else {
            @Suppress("DEPRECATION")
            val wavePattern = when (pattern) {
                VibrationPattern.DEFAULT -> longArrayOf(0, 200, 100, 200)
                VibrationPattern.FAST -> longArrayOf(0, 100, 100, 100)
                VibrationPattern.NONE -> null
            }
            // createWaveform이 아닌 구형 API에서는 pattern 사용 시 repeat index 필요 (-1: 반복 없음)
            wavePattern?.let { vibrator.vibrate(it, -1) }
        }
    }
}
