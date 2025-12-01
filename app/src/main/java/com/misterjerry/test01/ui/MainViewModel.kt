package com.misterjerry.test01.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misterjerry.test01.data.AudioClassifierHelper
import com.misterjerry.test01.data.ConversationItem
import com.misterjerry.test01.data.SoundEvent
import com.misterjerry.test01.data.SoundRepository
import com.misterjerry.test01.data.Urgency
import com.misterjerry.test01.util.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.misterjerry.test01.data.SoundSettings
import com.misterjerry.test01.data.VibrationPattern

data class MainUiState(
    val soundEvents: List<SoundEvent> = emptyList(),
    val conversationHistory: List<ConversationItem> = emptyList(),
    val isListening: Boolean = false,
    val soundSettings: SoundSettings = SoundSettings()
)


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val soundRepository = SoundRepository()
    // Remove ConversationRepository as we will generate real data
    // private val conversationRepository = ConversationRepository()

    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())
    private val _isListening = MutableStateFlow(false)
    private val _soundSettings = MutableStateFlow(SoundSettings())

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
    // private val audioClassifierHelper = AudioClassifierHelper(application) // Moved to Service
    private val vibrationHelper = VibrationHelper(application)

    init {
        // ... (SpeechRecognizer init code remains same) ...
        
        // Listen to audio classification results
        // Listen to audio classification results from EventBus
        // We now observe SoundEventBus.soundEvents directly in uiState, 
        // but we might still need to handle specific one-off logic if any.
        // For now, the list update is handled by the Bus and Service.
        
        // If we need to trigger vibration for foreground, we can still listen here OR 
        // rely on the Service to vibrate (which it now does for all).
        // However, the Service vibrates for Medium/High. 
        // The original ViewModel logic checked settings. 
        // Since we moved detection to Service, Service handles vibration.
        // We can remove the duplicate vibration logic here to avoid double vibration.

        // Listen to real-time events for Foreground Vibration
        viewModelScope.launch {
            com.misterjerry.test01.data.SoundEventBus.eventFlow.collect { event ->
                // Only handle if we are in foreground (though logic is in VM, VM is active when UI is active usually)
                // But VM can survive config changes. 
                // We rely on SoundEventBus.isForeground to be sure, or just rely on the fact that 
                // if the user is looking at the screen, they want feedback.
                // Actually, Service handles background. VM handles foreground.
                
                if (com.misterjerry.test01.data.SoundEventBus.isForeground) {
                    val settings = _soundSettings.value
                    val urgencySetting = when (event.urgency) {
                        Urgency.HIGH -> settings.highUrgency
                        Urgency.MEDIUM -> settings.mediumUrgency
                        Urgency.LOW -> settings.lowUrgency
                    }

                    if (urgencySetting.isEnabled) {
                        vibrationHelper.vibrate(urgencySetting.vibrationPattern)
                    }
                }
            }
        }
    }

    // ... (SpeechRecognizer methods remain same) ...

    fun startEnvironmentMode() {
        val intent = Intent(getApplication(), com.misterjerry.test01.service.SoundDetectionService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopEnvironmentMode() {
        val intent = Intent(getApplication(), com.misterjerry.test01.service.SoundDetectionService::class.java)
        getApplication<Application>().stopService(intent)
    }

    // handleSoundClassification is no longer needed in ViewModel as Service handles creation and Bus handles state.
    // However, if we want to support "Low Urgency" vibration settings which Service doesn't handle (Service only does Med/High),
    // we might need to keep some logic. 
    // But the user request was about background.
    // For consistency, let's assume Service handles all detection-related side effects for now, 
    // or we accept that foreground vibration settings might be bypassed by Service's simple logic.
    // Given the task "Fix Notification Navigation...", let's focus on the state.
    
    // We can remove handleSoundClassification entirely if we trust the Bus state.


    // We need to replace the repository flow with a local flow
    // Use SoundEventBus.soundEvents instead of local flow


    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                // Handle error if needed
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    addConversationItem(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    val uiState: StateFlow<MainUiState> = combine(
        com.misterjerry.test01.data.SoundEventBus.soundEvents,
        _conversationHistory,
        _isListening,
        _soundSettings,
    ) { sounds, history, isListening, settings ->
        MainUiState(
            soundEvents = sounds,
            conversationHistory = history,
            isListening = isListening,
            soundSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun startListening() {
        viewModelScope.launch(Dispatchers.Main) {
            _isListening.value = true
            speechRecognizer.startListening(recognitionIntent)
        }
    }

    fun stopListening() {
        viewModelScope.launch(Dispatchers.Main) {
            _isListening.value = false
            speechRecognizer.stopListening()
        }
    }

    private fun addConversationItem(text: String) {
        viewModelScope.launch {
            // 1. Add item immediately with loading state
            val tempId = System.currentTimeMillis()
            val tempItem = ConversationItem(
                id = tempId,
                speaker = "상대방",
                text = text,
                emotion = "",
                emotionLabel = "",
                isUser = false,
                timestamp = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA).format(java.util.Date()),
                isLoading = true
            )
            
            _conversationHistory.value = _conversationHistory.value + tempItem

            // 2. Analyze emotion
            val emotionLabel = analyzeEmotionWithGpt(text)
            val emotionEmoji = when (emotionLabel) {
                "긍정" -> "😃"
                "부정" -> "😠"
                "놀람" -> "😲"
                "슬픔" -> "😢"
                "공포" -> "😨"
                "걱정" -> "😟"
                else -> "😐"
            }

            // 3. Update item with result
            val updatedList = _conversationHistory.value.map { item ->
                if (item.id == tempId) {
                    item.copy(
                        emotion = emotionEmoji,
                        emotionLabel = emotionLabel,
                        isLoading = false
                    )
                } else {
                    item
                }
            }
            _conversationHistory.value = updatedList
        }
    }

    private suspend fun analyzeEmotionWithGpt(text: String): String {
        return try {
            val prompt = """
                다음 텍스트의 감정을 분석해서 '긍정', '부정', '중립', '놀람', '슬픔', '공포', '걱정' 중 하나로만 대답해줘.
                각 감정의 기준은 다음과 같아:
                - 긍정: 기쁨, 행복, 동의, 칭찬, 감사 (예: "정말 좋아", "고마워")
                - 부정: 화남, 짜증, 비판, 거절, 불만 (예: "싫어", "그만해")
                - 놀람: 충격, 믿기 힘듦, 예상치 못한 상황 (예: "정말?", "헐")
                - 슬픔: 후회, 실망, 비탄, 우울 (예: "너무 슬퍼", "아쉬워")
                - 공포: 무서움, 위협, 다급함 (예: "도와줘", "무서워")
                - 걱정: 불안, 근심, 상대방의 안부를 묻거나 염려함 (예: "괜찮아?", "조심해")
                - 중립: 감정이 드러나지 않는 사실 전달, 단순 질문 (예: "지금 몇 시야?", "밥 먹었어")

                텍스트: $text
            """.trimIndent()
            val request = com.misterjerry.test01.data.api.ChatRequest(
                messages = listOf(
                    com.misterjerry.test01.data.api.Message(role = "user", content = prompt)
                )
            )
            val response = com.misterjerry.test01.data.api.RetrofitClient.instance.getChatCompletion(request)
            val content = response.choices.firstOrNull()?.message?.content?.trim() ?: "중립"
            
            // Validate response just in case
            if (content in listOf("긍정", "부정", "중립", "놀람", "슬픔", "공포", "걱정")) content else "중립"
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to heuristic analysis
            analyzeEmotion(text)
        }
    }

    private fun analyzeEmotion(text: String): String {
        return when {
            text.contains("화나") || text.contains("짜증") -> "부정"
            text.contains("행복") || text.contains("좋아") || text.contains("사랑") -> "긍정"
            text.contains("놀라") || text.contains("헉") -> "놀람"
            text.contains("슬퍼") || text.contains("우울") -> "슬픔"
            text.contains("무서") || text.contains("공포") -> "공포"
            text.contains("걱정") || text.contains("불안") || text.contains("근심") || text.contains("괜찮아") -> "걱정"
            else -> "중립"
        }
    }

    fun updateSoundSettings(newSettings: SoundSettings) {
        _soundSettings.value = newSettings
    }

    fun clearSoundEvents() {
        // _soundEventsFlow.value = emptyList()
        // To implement clear, we should add a clear method to SoundEventBus
        com.misterjerry.test01.data.SoundEventBus.clearEvents()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
    }
}
