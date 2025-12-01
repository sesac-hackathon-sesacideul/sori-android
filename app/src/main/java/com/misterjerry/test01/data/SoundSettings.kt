package com.misterjerry.test01.data

data class SoundSettings(
    val highUrgency: UrgencySetting = UrgencySetting(),
    val mediumUrgency: UrgencySetting = UrgencySetting(),
    val lowUrgency: UrgencySetting = UrgencySetting()
)

data class UrgencySetting(
    val isEnabled: Boolean = true, // 목록 표시 여부
    val vibrationPattern: VibrationPattern = VibrationPattern.DEFAULT
)

enum class VibrationPattern(val label: String) {
    DEFAULT("기본"),
    FAST("빠르게"),
    NONE("진동 없음")
}
