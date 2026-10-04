package com.venom.domain.model

data class TestClass(
    val id: Int,
    val title: String,
    val totalTest: Int,
    val totalQuestion: Int,
    val totalCategory: Int,
    val progress: Float = 0f
)

data class TestCategory(
    val id: Int,
    val classId: Int,
    val title: String,
    val totalTest: Int,
    val totalQuestion: Int,
    val progress: Float = 0f
)

data class TestList(
    val id: Int,
    val categoryId: Int,
    val title: String,
    val questionNum: Int,
    val isCompleted: Boolean = false
)

data class Question(
    val id: Int,
    val testId: Int,
    val question: String,
    val choices: List<String>,
    val answer: String
)