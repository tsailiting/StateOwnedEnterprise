package com.exam.stateownedenterprise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.exam.stateownedenterprise.data.local.ExamSubjectOption
import com.exam.stateownedenterprise.data.repository.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: QuestionRepository
) : ViewModel() {

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedExamSubjectId = MutableStateFlow<Long?>(null)
    val selectedExamSubjectId: StateFlow<Long?> = _selectedExamSubjectId.asStateFlow()

    val years: StateFlow<List<Int>> = repository.getYears()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<String>> = _selectedYear.flatMapLatest { year ->
        if (year != null) {
            repository.getCategoriesForYear(year)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjectOptions: StateFlow<List<ExamSubjectOption>> = combine(
        _selectedYear,
        _selectedCategory
    ) { year, category ->
        Pair(year, category)
    }.flatMapLatest { (year, category) ->
        if (year != null && category != null) {
            repository.getSubjectOptions(year, category)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectYear(year: Int?) {
        _selectedYear.value = year
        _selectedCategory.value = null
        _selectedExamSubjectId.value = null
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _selectedExamSubjectId.value = null
    }

    fun selectExamSubject(examSubjectId: Long?) {
        _selectedExamSubjectId.value = examSubjectId
    }
}

class HomeViewModelFactory(
    private val repository: QuestionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
