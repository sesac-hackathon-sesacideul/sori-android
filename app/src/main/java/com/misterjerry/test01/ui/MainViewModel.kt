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

    private val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    // Variables for Voice Emotion Analysis
    private var minRmsDb: Float = 100f
    private var maxRmsDb: Float = -100f
    private var avgRmsDb: Float = 0f
    private var rmsSampleCount: Int = 0
    private var currentVoiceLabel: String = ""

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                currentVoiceLabel = "" // Reset label
                minRmsDb = 100f
                maxRmsDb = -100f
                avgRmsDb = 0f
                rmsSampleCount = 0
            }
            override fun onRmsChanged(rmsdB: Float) {
                // Track stats
                if (rmsdB < minRmsDb) minRmsDb = rmsdB
                if (rmsdB > maxRmsDb) maxRmsDb = rmsdB
                
                avgRmsDb += rmsdB
                rmsSampleCount++
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
                // Reset is handled in onBeginningOfSpeech, but good to reset here too just in case
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
        // Show temporary item while analyzing
        val tempId = System.currentTimeMillis()
        val tempItem = ConversationItem(
            id = tempId,
            speaker = "상대방",
            text = text,
            emotion = "⏳", // Loading
            emotionLabel = "분석 중...",
            isUser = false,
            timestamp = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA).format(java.util.Date())
        )
        
        val currentHistory = _conversationHistory.value
        _conversationHistory.value = currentHistory + tempItem

        // Call GPT for analysis
        viewModelScope.launch {
            val emotionLabel = analyzeEmotionWithGPT(text, currentVoiceLabel, maxRmsDb)
            val emotionEmoji = when (emotionLabel) {
                "긍정" -> "😃"
                "부정" -> "😠"
                else -> "😐"
            }
            
            // Update the item with result
            val updatedHistory = _conversationHistory.value.map { item ->
                if (item.id == tempId) {
                    item.copy(emotion = emotionEmoji, emotionLabel = emotionLabel)
                } else {
                    item
                }
            }
            _conversationHistory.value = updatedHistory
        }
    }

    private suspend fun analyzeEmotionWithGPT(text: String, voiceLabel: String, rmsDb: Float): String {
        return try {
            val prompt = """
                Analyze the emotion of the following spoken text.
                Context:
                - Volume: ${String.format("%.1f", rmsDb)}dB (Normal ~5.6, High > 7.0)
                - Voice Type: $voiceLabel
                - Text: "$text"
                
                Determine if the emotion is '긍정' (Positive), '부정' (Negative), or '중립' (Neutral).
                Consider that high volume or shouting usually indicates '부정' (Anger), even if the text is neutral.
                Laughter indicates '긍정'.
                
                Return ONLY one word: 긍정, 부정, or 중립.
            """.trimIndent()

            val request = com.misterjerry.test01.network.ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    com.misterjerry.test01.network.Message("system", "You are an emotion analysis assistant."),
                    com.misterjerry.test01.network.Message("user", prompt)
                )
            )

            val response = com.misterjerry.test01.network.RetrofitClient.openAIService.createChatCompletion(request)
            val content = response.choices.firstOrNull()?.message?.content?.trim() ?: "중립"
            
            // Basic validation to ensure we get one of the expected labels
            when {
                content.contains("긍정") -> "긍정"
                content.contains("부정") -> "부정"
                else -> "중립"
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "GPT Analysis Error", e)
            // Fallback to local logic in case of network error
            analyzeComplexEmotionFallback(text, voiceLabel, rmsDb)
        }
    }

    private fun analyzeComplexEmotionFallback(text: String, voiceLabel: String, rmsDb: Float): String {
        // 1. Voice Class Analysis (YAMNet)
        if (voiceLabel in listOf("Laughter", "Giggle", "Chuckle")) return "긍정"
        if (voiceLabel in listOf("Yell", "Shout", "Screaming", "Cry", "Sob")) return "부정"

        // 2. Volume Analysis (RMS)
        if (rmsDb > 7.0f) { 
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
            text.contains("화나") || text.contains("짜증") || text.contains("미워") || 
            text.contains("바보") || text.contains("멍청이") -> "부정"
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
