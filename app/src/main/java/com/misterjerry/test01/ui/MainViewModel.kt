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

data class MainUiState(
    val soundEvents: List<SoundEvent> = emptyList(),
    val conversationHistory: List<ConversationItem> = emptyList(),
    val isListening: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val soundRepository = SoundRepository()
    // Remove ConversationRepository as we will generate real data
    // private val conversationRepository = ConversationRepository()

    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())
    private val _isListening = MutableStateFlow(false)

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
    private val audioClassifierHelper = AudioClassifierHelper(application)
    private val vibrationHelper = VibrationHelper(application)

    init {
        // ... (SpeechRecognizer init code remains same) ...
        
        // Listen to audio classification results
        viewModelScope.launch {
            audioClassifierHelper.classificationFlow.collect { (label, direction) ->
                handleSoundClassification(label, direction)
            }
        }
    }

    // ... (SpeechRecognizer methods remain same) ...

    fun startEnvironmentMode() {
        audioClassifierHelper.startAudioClassification()
    }

    fun stopEnvironmentMode() {
        audioClassifierHelper.stopAudioClassification()
    }

    private fun handleSoundClassification(label: String, direction: Float) {
        val (koreanLabel, urgency) = when (label) {
            "Clapping", "Hands" -> "박수 소리" to Urgency.LOW
            "Knock" -> "노크 소리" to Urgency.LOW
            "Finger snapping" -> "핑거 스냅" to Urgency.LOW
            "Siren", "Ambulance (siren)", "Fire engine, fire truck (siren)" -> "사이렌" to Urgency.HIGH
            "Car horn, honking" -> "자동차 경적" to Urgency.HIGH
            "Dog", "Bark" -> "개 짖는 소리" to Urgency.MEDIUM
            "Baby cry, infant cry" -> "아기 울음소리" to Urgency.HIGH
            "Speech" -> "말소리" to Urgency.LOW
            else -> return // Ignore other sounds for now
        }

        val newEvent = SoundEvent(
            id = System.currentTimeMillis(),
            name = koreanLabel,
            direction = direction,
            distance = (1..10).random().toFloat(), // Random distance for demo
            urgency = urgency
        )

        vibrationHelper.vibrate(urgency)

        // Update sound events list (keep events within last 1 hour)
        val currentEvents = uiState.value.soundEvents
        val oneHourAgo = System.currentTimeMillis() - 3600000 // 1 hour in millis
        val updatedEvents = (listOf(newEvent) + currentEvents).filter { it.id > oneHourAgo }
        
        // We need to update the state. Since uiState is a combine of flows, we need a way to emit this.
        // The current architecture uses SoundRepository. Let's modify SoundRepository or just use a MutableStateFlow for sounds in VM.
        // For simplicity in this refactor, let's override the sound list in the UI state directly or add a local flow.
        // Wait, uiState is derived from soundRepository.getSoundEvents().
        // I should update SoundRepository to accept new events or mock it here.
        // Let's add a method to SoundRepository to add an event? No, it's a mock repo.
        // Let's change the logic: MainViewModel should manage the source of truth for sounds now.
        
        _soundEventsFlow.value = updatedEvents
    }

    // We need to replace the repository flow with a local flow
    private val _soundEventsFlow = MutableStateFlow<List<SoundEvent>>(emptyList())

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
        _soundEventsFlow,
        _conversationHistory,
        _isListening
    ) { sounds, history, isListening ->
        MainUiState(
            soundEvents = sounds,
            conversationHistory = history,
            isListening = isListening
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
            val prompt = "다음 텍스트의 감정을 분석해서 '긍정', '부정', '중립' 중 하나로만 대답해줘. 텍스트: $text"
            val request = com.misterjerry.test01.data.api.ChatRequest(
                messages = listOf(
                    com.misterjerry.test01.data.api.Message(role = "user", content = prompt)
                )
            )
            val response = com.misterjerry.test01.data.api.RetrofitClient.instance.getChatCompletion(request)
            val content = response.choices.firstOrNull()?.message?.content?.trim() ?: "중립"
            
            // Validate response just in case
            if (content in listOf("긍정", "부정", "중립")) content else "중립"
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
            else -> "중립"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
    }
}
