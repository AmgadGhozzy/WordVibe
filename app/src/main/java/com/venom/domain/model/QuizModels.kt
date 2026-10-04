package com.venom.domain.model

import androidx.compose.runtime.Immutable

enum class QuestionType { MEANING, DEFINITION, REVERSE }
enum class QuizSource { FLASHCARD, UNIT, PLACEMENT }

@Immutable
data class QuizInput(
    val wordId: Int,
    val questionType: QuestionType,
    val prompt: String,
    val correctAnswer: String,
    val options: List<String>,
    val source: QuizSource,
    val sessionId: String? = null,
    val cardPosition: Int? = null,
    val cefrLevel: String? = null,
    val difficulty: Int = 5
)

@Immutable
data class QuizResult(
    val wordId: Int,
    val isCorrect: Boolean,
    val selectedAnswer: String,
    val correctAnswer: String,
    val timeSpentMs: Long,
    val attempts: Int,
    val usedSkip: Boolean,
    val source: QuizSource,
    val sessionId: String? = null,
    val cardPosition: Int? = null,
    val questionType: QuestionType,
    val cefrLevel: String? = null,
    val difficulty: Int = 5
)

sealed class QuizMode {
    data class Word(val level: WordLevels) : QuizMode()
    data class Test(val testId: Int) : QuizMode()
    data class Flashcard(val input: QuizInput) : QuizMode()
    data object Placement : QuizMode()
}

sealed class QuizQuestionUi {
    abstract val prompt: String
    abstract val isHtml: Boolean
    abstract val options: List<String>
    abstract val correctAnswer: String?

    data class WordQuestion(
        val word: WordMaster,
        override val options: List<String>,
        override val correctAnswer: String,
        val translation: String
    ) : QuizQuestionUi() {
        override val prompt: String = word.wordEn
        override val isHtml: Boolean = false
    }

    data class TestQuestion(
        val question: Question,
        override val options: List<String>,
        override val correctAnswer: String?
    ) : QuizQuestionUi() {
        override val prompt: String = question.question
        override val isHtml: Boolean = true
    }

    data class FlashcardQuestion(
        val input: QuizInput,
        override val options: List<String> = input.options,
        override val correctAnswer: String = input.correctAnswer
    ) : QuizQuestionUi() {
        override val prompt: String = input.prompt
        override val isHtml: Boolean = false
    }
}

data class QuizUiState(
    val mode: QuizMode? = null,
    val questions: List<QuizQuestionUi> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val selectedAnswers: Map<Int, String> = emptyMap(),
    val isAnswered: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val timeRemaining: Int = 0,
    val hearts: Int = 0,
    val streak: Int = 0,
    val score: Float = 0f,
    val showResults: Boolean = false,
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val placementBand: Int = 0,
    val placementBandLabel: String = ""
) {
    val currentQuestion: QuizQuestionUi? get() = questions.getOrNull(currentIndex)
    val progress: Float get() = if (totalQuestions == 0) 0f else (currentIndex + 1f) / totalQuestions
}
