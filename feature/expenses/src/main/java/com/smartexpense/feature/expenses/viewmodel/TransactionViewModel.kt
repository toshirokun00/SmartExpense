package com.smartexpense.feature.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.SearchExpensesUseCase
import com.smartexpense.feature.expenses.effect.TransactionEffect
import com.smartexpense.feature.expenses.intent.TransactionIntent
import com.smartexpense.feature.expenses.state.FilterState
import com.smartexpense.feature.expenses.state.TransactionUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val searchExpensesUseCase: SearchExpensesUseCase,
    private val categoryUseCase: CategoryUseCase
) : ViewModel() {

    private val _criteria = MutableStateFlow(SearchCriteria())

    private val _filterState = MutableStateFlow(FilterState())

    private val _expenses = MutableStateFlow(emptyList<Expense>())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _effect = MutableSharedFlow<TransactionEffect>()
    val effect = _effect.asSharedFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                TransactionEffect.ShowError(
                    message = exception.message ?: "Failed to load categories"
                )
            )
            emptyFlow<List<Category>>()
        }

    private fun search(criteria: SearchCriteria) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val expenses = try {
                searchExpensesUseCase(criteria).first()
            } catch (e: Exception) {
                _effect.emit(
                    TransactionEffect.ShowError(
                        message = e.message ?: "Failed to search expenses"
                    )
                )
                emptyList()
            }
            _expenses.value = expenses
        }
    }

    init {
        // Trigger initial search with default (empty) criteria so all expenses
        // are shown when the screen first loads.
        search(_criteria.value)
    }

    val uiState: StateFlow<TransactionUiState> =
        combine(
            expenses,
            categoriesFlow,
            _criteria,
            _filterState
        ) { expenses, categories, criteria, filter ->
            TransactionUiState(
                expenses = expenses,
                categories = categories,
                query = criteria.query,
                selectedCategoryIds = criteria.selectedCategoryIds,
                startTime = criteria.startTime,
                endTime = criteria.endTime,
                minAmount = criteria.minAmount,
                maxAmount = criteria.maxAmount,
                sortOrder = criteria.sortOrder,
                filter = filter,
                isLoading = false,
                error = null
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TransactionUiState(isLoading = true)
            )

    fun onIntent(intent: TransactionIntent) {
        when (intent) {
            is TransactionIntent.SearchChanged -> {
                _criteria.update {
                    it.copy(query = intent.query)
                }
                search(_criteria.value)
            }

            is TransactionIntent.CategorySelectionChanged -> {
                _criteria.update {
                    it.copy(selectedCategoryIds = intent.selectedCategoryIds)
                }
                search(_criteria.value)
            }

            is TransactionIntent.DateRangeChanged -> {
                _criteria.update {
                    it.copy(
                        startTime = intent.startTime,
                        endTime = intent.endTime
                    )
                }
                search(_criteria.value)
            }

            is TransactionIntent.AmountRangeChanged -> {
                _criteria.update {
                    it.copy(
                        minAmount = intent.minAmount,
                        maxAmount = intent.maxAmount
                    )
                }
                search(_criteria.value)
            }

            is TransactionIntent.SortChanged -> {
                _criteria.update {
                    it.copy(sortOrder = intent.sortOrder)
                }
                search(_criteria.value)
            }

            TransactionIntent.ClearFilters -> {
                _criteria.value = SearchCriteria()
                _filterState.value = FilterState()
                search(_criteria.value)
            }

            // Temporary filter state intents
            TransactionIntent.ToggleFilterSection -> {
                _filterState.update {
                    it.copy(isSectionExpanded = !it.isSectionExpanded)
                }
            }

            is TransactionIntent.TempCategorySelectionChanged -> {
                _filterState.update {
                    it.copy(tempSelectedCategoryIds = intent.selectedCategoryIds)
                }
            }

            is TransactionIntent.TempDateRangeChanged -> {
                _filterState.update {
                    it.copy(tempStartTime = intent.startTime, tempEndTime = intent.endTime)
                }
            }

            is TransactionIntent.TempAmountRangeChanged -> {
                _filterState.update {
                    it.copy(tempMinAmount = intent.minAmount, tempMaxAmount = intent.maxAmount)
                }
            }

            is TransactionIntent.TempSortChanged -> {
                _filterState.update {
                    it.copy(tempSortOrder = intent.sortOrder)
                }
            }

            is TransactionIntent.TempSearchChanged -> {
                _filterState.update {
                    it.copy(tempQuery = intent.query)
                }
            }

            TransactionIntent.ApplyFilters -> {
                val filter = _filterState.value
                val applyCriteria = _criteria.value.copy(
                    query = filter.tempQuery.takeIf { it.isNotEmpty() }
                        ?: _criteria.value.query,
                    selectedCategoryIds = filter.tempSelectedCategoryIds
                        .takeIf { it.isNotEmpty() }
                        ?: _criteria.value.selectedCategoryIds,
                    startTime = filter.tempStartTime
                        ?: _criteria.value.startTime,
                    endTime = filter.tempEndTime
                        ?: _criteria.value.endTime,
                    minAmount = filter.tempMinAmount
                        ?: _criteria.value.minAmount,
                    maxAmount = filter.tempMaxAmount
                        ?: _criteria.value.maxAmount,
                    sortOrder = filter.tempSortOrder
                )
                _criteria.value = applyCriteria
                _filterState.value = FilterState()
                search(applyCriteria)
            }

            TransactionIntent.CancelFilters -> {
                _filterState.value = FilterState()
            }
        }
    }
}
