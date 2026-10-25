package com.smartexpense.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportRequest
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ExportReportCsvUseCase
import com.smartexpense.domain.usecase.ExportReportPdfUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.reports.effect.ReportsEffect
import com.smartexpense.reports.effect.ReportsEffect.LaunchSaveDocument
import com.smartexpense.reports.effect.ReportsEffect.LaunchShare
import com.smartexpense.reports.effect.ReportsEffect.ShowError
import com.smartexpense.reports.effect.ReportsEffect.ShowMessage
import com.smartexpense.reports.export.ReportExportFileWriter
import com.smartexpense.reports.export.displayName
import com.smartexpense.reports.export.mimeType
import com.smartexpense.reports.intent.ReportsIntent
import com.smartexpense.reports.state.ReportsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ReportsViewModel(
    private val getExpenseReportUseCase: GetExpenseReportUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val exportReportCsvUseCase: ExportReportCsvUseCase,
    private val exportReportPdfUseCase: ExportReportPdfUseCase,
    private val reportExportFileWriter: ReportExportFileWriter
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val initialDateRange = getCurrentMonthRange()

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            startDateInclusive = initialDateRange.first,
            endDateExclusive = initialDateRange.second,
            isLoading = true
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ReportsEffect>()
    val effect = _effect.asSharedFlow()

    private var reportJob: Job? = null
    private var exportJob: Job? = null
    private var saveJob: Job? = null
    private var pendingSaveExport: ReportExportFile? = null

    init {
        observeCategories()

        loadReport(
            startDateInclusive = initialDateRange.first,
            endDateExclusive = initialDateRange.second
        )
    }

    fun onIntent(intent: ReportsIntent) {
        when (intent) {
            is ReportsIntent.DateRangeChanged -> {
                if (_uiState.value.isExporting) {
                    showBusyMessage()
                    return
                }

                if (intent.endDateExclusive <= intent.startDateInclusive) {
                    viewModelScope.launch {
                        _effect.emit(
                            ShowError(
                                "End date must be after start date"
                            )
                        )
                    }
                    return
                }

                loadReport(
                    startDateInclusive = intent.startDateInclusive,
                    endDateExclusive = intent.endDateExclusive
                )
            }

            ReportsIntent.Retry -> {
                val state = _uiState.value
                loadReport(
                    startDateInclusive = state.startDateInclusive,
                    endDateExclusive = state.endDateExclusive
                )
            }

            ReportsIntent.DatePickerDismissed -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = false,
                        isEndDatePickerOpen = false
                    )
                }
            }

            ReportsIntent.EndDatePickerOpened -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = false,
                        isEndDatePickerOpen = true
                    )
                }
            }

            ReportsIntent.StartDatePickerOpened -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = true,
                        isEndDatePickerOpen = false
                    )
                }
            }

            is ReportsIntent.ExportTypeSelected -> {
                if (_uiState.value.isExporting) {
                    showBusyMessage()
                    return
                }

                _uiState.update {
                    it.copy(exportType = intent.exportType)
                }
            }

            is ReportsIntent.ExportFormatSelected -> {
                if (_uiState.value.isExporting) {
                    showBusyMessage()
                    return
                }

                _uiState.update {
                    it.copy(exportFormat = intent.exportFormat)
                }
            }

            ReportsIntent.SaveClicked -> {
                startExport(shouldShare = false)
            }

            ReportsIntent.ShareClicked -> {
                startExport(shouldShare = true)
            }

            is ReportsIntent.SaveDestinationSelected -> {
                writePendingSave(intent.uri)
            }

            ReportsIntent.SaveDestinationSelectionCancelled -> {
                if (pendingSaveExport == null && !_uiState.value.isExporting) {
                    return
                }

                pendingSaveExport = null
                _uiState.update { it.copy(isExporting = false) }
                viewModelScope.launch {
                    _effect.emit(ShowMessage("Export cancelled"))
                }
            }

            is ReportsIntent.ExportLaunchFailed -> {
                pendingSaveExport = null
                _uiState.update { it.copy(isExporting = false) }
                viewModelScope.launch {
                    _effect.emit(ShowError(intent.message))
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryUseCase.getCategories()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.message
                                ?: "Failed to load categories",
                            isLoading = false
                        )
                    }

                    _effect.emit(
                        ShowError(
                            throwable.message ?: "Failed to load categories"
                        )
                    )
                }
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categoryNames = categories.associate {
                                it.id to it.name
                            }
                        )
                    }
                }
        }
    }

    private fun loadReport(
        startDateInclusive: Long,
        endDateExclusive: Long
    ) {
        reportJob?.cancel()

        _uiState.update {
            it.copy(
                startDateInclusive = startDateInclusive,
                endDateExclusive = endDateExclusive,
                isStartDatePickerOpen = false,
                isEndDatePickerOpen = false,
                isLoading = true,
                error = null
            )
        }

        reportJob = viewModelScope.launch {
            getExpenseReportUseCase(
                startDateInclusive = startDateInclusive,
                endDateExclusive = endDateExclusive
            )
                .catch { throwable ->
                    val message = throwable.message
                        ?: "Failed to load expense report"

                    _uiState.update {
                        it.copy(isLoading = false, error = message)
                    }

                    _effect.emit(ShowError(message))
                }
                .collect { report ->
                    _uiState.update {
                        it.copy(
                            report = report,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun startExport(shouldShare: Boolean) {
        if (_uiState.value.isExporting) {
            showBusyMessage()
            return
        }

        val state = _uiState.value

        if (state.endDateExclusive <= state.startDateInclusive) {
            viewModelScope.launch {
                _effect.emit(
                    ShowError("End date must be after start date")
                )
            }
            return
        }

        pendingSaveExport = null
        _uiState.update {
            it.copy(
                isExporting = true,
                isStartDatePickerOpen = false,
                isEndDatePickerOpen = false,
                error = null
            )
        }

        val request = ReportExportRequest(
            type = state.exportType,
            startDateInclusive = state.startDateInclusive,
            endDateExclusive = state.endDateExclusive
        )

        exportJob = viewModelScope.launch {
            try {
                val export = when (state.exportFormat) {
                    ReportExportFormat.CSV -> exportReportCsvUseCase(
                        request = request,
                        categoryNames = state.categoryNames
                    )

                    ReportExportFormat.PDF -> exportReportPdfUseCase(
                        request = request,
                        categoryNames = state.categoryNames
                    )
                }

                if (export.isEmpty) {
                    _uiState.update { it.copy(isExporting = false) }
                    _effect.emit(
                        ShowMessage(
                            when (export.type) {
                                ReportExportType.TRANSACTIONS ->
                                    "No transactions found in the selected date range."

                                ReportExportType.SUMMARY ->
                                    "No expenses found in the selected date range."
                            }
                        )
                    )
                    return@launch
                }

                if (shouldShare) {
                    val sharedFile = reportExportFileWriter.createShareFile(
                        export = export
                    )

                    _uiState.update { it.copy(isExporting = false) }
                    _effect.emit(
                        LaunchShare(
                            fileUri = sharedFile.uri,
                            mimeType = sharedFile.mimeType,
                            title = "Share SmartExpense ${export.format.displayName}"
                        )
                    )
                } else {
                    pendingSaveExport = export
                    _effect.emit(
                        LaunchSaveDocument(
                            fileName = export.fileName,
                            mimeType = export.format.mimeType
                        )
                    )
                }
            } catch (exception: CancellationException) {
                _uiState.update { it.copy(isExporting = false) }
                throw exception
            } catch (exception: Exception) {
                pendingSaveExport = null
                _uiState.update { it.copy(isExporting = false) }
                _effect.emit(
                    ShowError(
                        exception.message
                            ?: "Failed to export ${state.exportFormat.displayName}"
                    )
                )
            }
        }
    }

    private fun writePendingSave(uri: String) {
        if (saveJob?.isActive == true) {
            return
        }

        val pendingExport = pendingSaveExport
        if (pendingExport == null) {
            _uiState.update { it.copy(isExporting = false) }
            viewModelScope.launch {
                _effect.emit(
                    ShowError("The export is no longer available")
                )
            }
            return
        }

        _uiState.update { it.copy(isExporting = true) }

        saveJob = viewModelScope.launch {
            try {
                reportExportFileWriter.writeToDocument(
                    uri = uri,
                    export = pendingExport
                )
                pendingSaveExport = null
                _uiState.update { it.copy(isExporting = false) }
                _effect.emit(
                    ShowMessage(
                        "${pendingExport.format.displayName} saved successfully"
                    )
                )
            } catch (exception: CancellationException) {
                _uiState.update { it.copy(isExporting = false) }
                throw exception
            } catch (exception: Exception) {
                pendingSaveExport = null
                _uiState.update { it.copy(isExporting = false) }
                _effect.emit(
                    ShowError(
                        exception.message
                            ?: "Failed to save ${pendingExport.format.displayName}"
                    )
                )
            }
        }
    }

    private fun showBusyMessage() {
        viewModelScope.launch {
            _effect.emit(
                ShowMessage("An export is already in progress")
            )
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val firstDay = LocalDate.now(zoneId).withDayOfMonth(1)
        val firstDayOfNextMonth = firstDay.plusMonths(1)

        return Pair(
            firstDay.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            firstDayOfNextMonth.atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        )
    }
}
