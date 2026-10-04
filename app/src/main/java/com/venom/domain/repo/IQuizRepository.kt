package com.venom.domain.repo

import com.venom.domain.model.Question
import com.venom.domain.model.TestCategory
import com.venom.domain.model.TestClass
import com.venom.domain.model.TestList
import kotlinx.coroutines.flow.Flow

interface IQuizRepository {
    fun getAllTestClasses(): Flow<List<TestClass>>
    fun getCategoriesForClass(classId: Int): Flow<List<TestCategory>>
    fun getTestsForCategory(categoryId: Int): Flow<List<TestList>>
    fun getQuestionsForTest(testId: Int): Flow<List<Question>>
    suspend fun getTestClassById(classId: Int): TestClass?
    suspend fun getCategoryById(categoryId: Int): TestCategory?
    suspend fun getTestById(testId: Int): TestList?
}
