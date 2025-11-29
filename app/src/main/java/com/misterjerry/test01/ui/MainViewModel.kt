package com.misterjerry.test01.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misterjerry.test01.data.ConversationItem
import com.misterjerry.test01.data.SoundEvent
import com.misterjerry.test01.data.SoundRepository
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
        soundRepository.getSoundEvents(),
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
        val emotion = analyzeEmotion(text)
        val newItem = ConversationItem(
            id = System.currentTimeMillis().toString(),
            text = text,
            isUser = false, // Assuming the app listens to "others"
            emotionLabel = emotion,
            timestamp = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA).format(java.util.Date())
        )
        
        val currentHistory = _conversationHistory.value
        _conversationHistory.value = currentHistory + newItem
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
