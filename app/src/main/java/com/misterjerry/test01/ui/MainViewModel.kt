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
    // private val soundRepository = SoundRepository() // Not used anymore
    
    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())
    private val _isListening = MutableStateFlow(false)
    private val _soundEventsFlow = MutableStateFlow<List<SoundEvent>>(emptyList())

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
    private val audioClassifierHelper = AudioClassifierHelper(application)

    // Variables for Voice Emotion Analysis
    private var currentRmsDb: Float = 0f
    private var currentVoiceLabel: String = ""

    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        // Initialize SpeechRecognizer Listener
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                currentVoiceLabel = "" // Reset label
            }
            override fun onRmsChanged(rmsdB: Float) {
                // Keep track of the maximum volume during speech
                if (rmsdB > currentRmsDb) {
                    currentRmsDb = rmsdB
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                android.util.Log.e("MainViewModel", "SpeechRecognizer Error: $error")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    addConversationItem(text)
                }
                // Reset for next turn
                currentRmsDb = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        // Listen to audio classification results (YAMNet)
        viewModelScope.launch {
            audioClassifierHelper.classificationFlow.collect { label ->
                // Store for emotion analysis (if we could run it)
                currentVoiceLabel = label
                
                // Also handle as SoundEvent if it's an environmental sound
                handleSoundClassification(label) 
            }
        }
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

    // --- Voice Recognition Control ---
    fun startListening() {
        viewModelScope.launch(Dispatchers.Main) {
            _isListening.value = true
            speechRecognizer.startListening(recognitionIntent)
            // Note: We cannot run AudioClassifier simultaneously with SpeechRecognizer due to mic conflict.
            // We will rely on RMS (Volume) for voice emotion detection.
            // audioClassifierHelper.startAudioClassification() 
        }
    }

    fun stopListening() {
        viewModelScope.launch(Dispatchers.Main) {
            _isListening.value = false
            speechRecognizer.stopListening()
            // audioClassifierHelper.stopAudioClassification()
        }
    }

    // --- Environmental Sound Mode Control ---
    fun startEnvironmentMode() {
        audioClassifierHelper.startAudioClassification()
    }

    fun stopEnvironmentMode() {
        audioClassifierHelper.stopAudioClassification()
    }

    // --- Logic ---

    private fun handleSoundClassification(label: String) {
        // If we are listening for speech, we might want to ignore some sounds or treat them as emotion.
        // But for now, let's just log everything to the sound list as well.
        
        val (koreanLabel, urgency) = when (label) {
            "Clapping", "Hands" -> "박수 소리" to Urgency.LOW
            "Knock" -> "노크 소리" to Urgency.LOW
            "Finger snapping" -> "핑거 스냅" to Urgency.LOW
            "Siren", "Ambulance (siren)", "Fire engine, fire truck (siren)" -> "사이렌" to Urgency.HIGH
            "Car horn, honking" -> "자동차 경적" to Urgency.HIGH
            "Dog", "Bark" -> "개 짖는 소리" to Urgency.MEDIUM
            "Baby cry, infant cry" -> "아기 울음소리" to Urgency.HIGH
            "Speech" -> "말소리" to Urgency.LOW
            else -> return // Ignore other sounds
        }

        val newEvent = SoundEvent(
            id = System.currentTimeMillis(),
            name = koreanLabel,
            direction = (0..360).random().toFloat(),
            distance = (1..10).random().toFloat(),
            urgency = urgency
        )

        val currentEvents = _soundEventsFlow.value
        val updatedEvents = (listOf(newEvent) + currentEvents).take(5)
        _soundEventsFlow.value = updatedEvents
    }

    private fun addConversationItem(text: String) {
        val emotionLabel = analyzeComplexEmotion(text, currentVoiceLabel, currentRmsDb)
        val emotionEmoji = when (emotionLabel) {
            "긍정" -> "😃"
            "부정" -> "😠"
            else -> "😐"
        }
        
        // DEBUG: Show RMS value to help tuning
        val debugLabel = "$emotionLabel (${String.format("%.1f", currentRmsDb)})"
        
        val newItem = ConversationItem(
            id = System.currentTimeMillis(),
            speaker = "상대방",
            text = text,
            emotion = emotionEmoji,
            emotionLabel = debugLabel,
            isUser = false,
            timestamp = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA).format(java.util.Date())
        )
        
        val currentHistory = _conversationHistory.value
        _conversationHistory.value = currentHistory + newItem
    }

    private fun analyzeComplexEmotion(text: String, voiceLabel: String, rmsDb: Float): String {
        // 1. Voice Class Analysis (YAMNet)
        if (voiceLabel in listOf("Laughter", "Giggle", "Chuckle")) return "긍정"
        if (voiceLabel in listOf("Yell", "Shout", "Screaming", "Cry", "Sob")) return "부정"

        // 2. Volume Analysis (RMS)
        // Heuristic: If volume is high (> 10.0), consider it negative unless text is explicitly positive.
        // Adjusted to 10.0f based on user feedback (8.0 too sensitive, 12.0 too insensitive).
        if (rmsDb > 10.0f) { 
            if (!isTextPositive(text)) return "부정"
        }

        // 3. Text Analysis (Fallback)
        return analyzeTextEmotion(text)
    }

    private fun isTextPositive(text: String): Boolean {
        return text.contains("행복") || text.contains("좋아") || text.contains("사랑") || text.contains("감사")
    }

    private fun analyzeTextEmotion(text: String): String {
        return when {
            text.contains("화나") || text.contains("짜증") || text.contains("미워") -> "부정"
            isTextPositive(text) -> "긍정"
            else -> "중립"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        // audioClassifierHelper.stopAudioClassification() // Already handled in stop methods usually, but good practice to clear
    }
}
