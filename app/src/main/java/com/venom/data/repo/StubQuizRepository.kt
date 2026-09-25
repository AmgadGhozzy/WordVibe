package com.venom.data.repo

import com.venom.domain.model.Question
import com.venom.domain.model.TestCategory
import com.venom.domain.model.TestClass
import com.venom.domain.model.TestList
import com.venom.domain.repo.IQuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * NOT STARTED: quiz content backend. Returns empty flows by design; quiz content arrives
 * through the corpus + learning loop, not this interface, until a gated decision says so.
 */
class StubQuizRepository @Inject constructor() : IQuizRepository {
    override fun getAllTestClasses(): Flow<List<TestClass>> = emptyFlow()
    override fun getCategoriesForClass(classId: Int): Flow<List<TestCategory>> = emptyFlow()
    override fun getTestsForCategory(categoryId: Int): Flow<List<TestList>> = emptyFlow()
    override fun getQuestionsForTest(testId: Int): Flow<List<Question>> = emptyFlow()
    override suspend fun getTestClassById(classId: Int): TestClass? = null
    override suspend fun getCategoryById(categoryId: Int): TestCategory? = null
    override suspend fun getTestById(testId: Int): TestList? = null
}
