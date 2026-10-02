package com.example.aimetadatacleaner.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimetadatacleaner.MetaCleanApplication
import com.example.aimetadatacleaner.data.database.CleanedRecordEntity
import com.example.aimetadatacleaner.data.model.CleanExecutionResult
import com.example.aimetadatacleaner.data.model.CleaningOptions
import com.example.aimetadatacleaner.data.model.ImageInspectionResult
import com.example.aimetadatacleaner.data.model.PrivacyInspectionReport
import com.example.aimetadatacleaner.util.MetadataCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System Default", "Follows device dark/light setting"),
    LIGHT("Light Mode", "Crisp daylight high-contrast theme"),
    DARK("Dark Mode", "Deep slate OLED privacy dark theme")
}

data class BatchProgressState(
    val isRunning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val completedResults: List<CleanExecutionResult> = emptyList(),
    val verifiedCleanCount: Int = 0,
    val partialCleanCount: Int = 0,
    val failedCount: Int = 0
)

data class BatchItemInspection(
    val uri: Uri,
    val inspection: ImageInspectionResult? = null,
    val isInspecting: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MetaCleanApplication).repository
    private val prefs = application.getSharedPreferences("metaclean_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
        showToast("Theme changed to ${mode.title}")
    }

    val historyRecords: StateFlow<List<CleanedRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRecordsCount: StateFlow<Int> = repository.totalRecordsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalTagsRemoved: StateFlow<Int> = repository.totalTagsRemoved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _inspectionResult = MutableStateFlow<ImageInspectionResult?>(null)
    val inspectionResult: StateFlow<ImageInspectionResult?> = _inspectionResult.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    private val _cleaningOptions = MutableStateFlow(CleaningOptions())
    val cleaningOptions: StateFlow<CleaningOptions> = _cleaningOptions.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _cleanResult = MutableStateFlow<CleanExecutionResult?>(null)
    val cleanResult: StateFlow<CleanExecutionResult?> = _cleanResult.asStateFlow()

    private val _batchUris = MutableStateFlow<List<Uri>>(emptyList())
    val batchUris: StateFlow<List<Uri>> = _batchUris.asStateFlow()

    private val _batchInspections = MutableStateFlow<List<BatchItemInspection>>(emptyList())
    val batchInspections: StateFlow<List<BatchItemInspection>> = _batchInspections.asStateFlow()

    private val _batchState = MutableStateFlow(BatchProgressState())
    val batchState: StateFlow<BatchProgressState> = _batchState.asStateFlow()

    private val _activePrivacyReport = MutableStateFlow<PrivacyInspectionReport?>(null)
    val activePrivacyReport: StateFlow<PrivacyInspectionReport?> = _activePrivacyReport.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun selectImage(uri: Uri) {
        _batchUris.value = emptyList()
        _batchInspections.value = emptyList()
        _batchState.value = BatchProgressState()
        _selectedUri.value = uri
        _cleanResult.value = null
        inspectCurrentUri(uri)
    }

    fun clearSelection() {
        _selectedUri.value = null
        _inspectionResult.value = null
        _cleanResult.value = null
    }

    fun clearBatch() {
        _batchUris.value = emptyList()
        _batchInspections.value = emptyList()
        _batchState.value = BatchProgressState()
    }

    private fun inspectCurrentUri(uri: Uri) {
        viewModelScope.launch {
            _isInspecting.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.inspectImage(uri)
                }
                _inspectionResult.value = result
            } catch (e: Exception) {
                _toastMessage.value = "Failed to read image metadata: ${e.message}"
            } finally {
                _isInspecting.value = false
            }
        }
    }

    fun updateOptions(transform: (CleaningOptions) -> CleaningOptions) {
        _cleaningOptions.value = transform(_cleaningOptions.value)
    }

    fun cleanCurrentImage() {
        val uri = _selectedUri.value ?: return
        viewModelScope.launch {
            _isCleaning.value = true
            try {
                val result = repository.cleanSingleImage(uri, _cleaningOptions.value)
                _cleanResult.value = result
                if (result.success) {
                    if (result.isVerifiedClean) {
                        _toastMessage.value = "Cleaned & Verified! 100% clean."
                    } else {
                        _toastMessage.value = "Sanitized, but verification noted residual fields."
                    }
                } else {
                    _toastMessage.value = "Cleaning failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _toastMessage.value = "Error: ${e.message}"
            } finally {
                _isCleaning.value = false
            }
        }
    }

    fun saveCleanedToGallery(filePath: String?) {
        if (filePath == null) return
        viewModelScope.launch {
            val savedUri = repository.saveToGallery(filePath)
            if (savedUri != null) {
                _toastMessage.value = "Saved to Pictures/Metadata_Cleaner in Gallery!"
            } else {
                _toastMessage.value = "Could not save to gallery."
            }
        }
    }

    fun shareImage(uri: Uri?, fileName: String) {
        if (uri == null) return
        repository.shareImage(uri, fileName)
    }

    fun showPrivacyReport(report: PrivacyInspectionReport) {
        _activePrivacyReport.value = report
    }

    fun dismissPrivacyReport() {
        _activePrivacyReport.value = null
    }

    fun sharePrivacyReport(report: PrivacyInspectionReport) {
        MetadataCleaner.shareReport(getApplication(), report.toFormattedText(), report.fileName)
    }

    fun setBatchUris(uris: List<Uri>) {
        _selectedUri.value = null
        _inspectionResult.value = null
        _cleanResult.value = null
        _batchUris.value = uris
        _batchState.value = BatchProgressState(total = uris.size)
        _batchInspections.value = uris.map { BatchItemInspection(uri = it, isInspecting = true) }

        viewModelScope.launch {
            val currentList = uris.map { BatchItemInspection(uri = it, isInspecting = true) }.toMutableList()
            for ((index, uri) in uris.withIndex()) {
                val result = withContext(Dispatchers.IO) {
                    try {
                        repository.inspectImage(uri)
                    } catch (_: Exception) {
                        null
                    }
                }
                currentList[index] = BatchItemInspection(uri = uri, inspection = result, isInspecting = false)
                _batchInspections.value = currentList.toList()
            }
        }
    }

    fun startBatchCleaning() {
        val uris = _batchUris.value
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _batchState.value = BatchProgressState(isRunning = true, current = 0, total = uris.size)
            val results = mutableListOf<CleanExecutionResult>()

            for ((index, uri) in uris.withIndex()) {
                val res = repository.cleanSingleImage(uri, _cleaningOptions.value)
                results.add(res)
                val verifiedCount = results.count { it.success && it.isVerifiedClean }
                val partialCount = results.count { it.success && !it.isVerifiedClean }
                val failedCount = results.count { !it.success }

                _batchState.value = BatchProgressState(
                    isRunning = true,
                    current = index + 1,
                    total = uris.size,
                    completedResults = results.toList(),
                    verifiedCleanCount = verifiedCount,
                    partialCleanCount = partialCount,
                    failedCount = failedCount
                )
            }

            val finalVerified = results.count { it.success && it.isVerifiedClean }
            val finalPartial = results.count { it.success && !it.isVerifiedClean }
            val finalFailed = results.count { !it.success }

            _batchState.value = BatchProgressState(
                isRunning = false,
                current = uris.size,
                total = uris.size,
                completedResults = results.toList(),
                verifiedCleanCount = finalVerified,
                partialCleanCount = finalPartial,
                failedCount = finalFailed
            )
            _toastMessage.value = "Batch complete: $finalVerified verified clean, $finalPartial partial, $finalFailed failed."
        }
    }

    fun deleteHistoryRecord(id: Long, filePath: String?) {
        viewModelScope.launch {
            repository.deleteRecord(id, filePath)
            _toastMessage.value = "Record deleted."
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _toastMessage.value = "Local history cleared."
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun dismissCleanResult() {
        _cleanResult.value = null
    }
}
