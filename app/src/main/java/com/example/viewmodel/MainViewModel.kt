package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ControlLayout
import com.example.data.model.DeviceCapability
import com.example.data.model.GameConfig
import com.example.data.model.GameEntity
import com.example.data.model.GameGenre
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.GamingEnvironmentState
import com.example.data.model.PerformanceProfile
import com.example.data.model.RuntimeComponent
import com.example.data.repository.GameRepository
import com.example.engine.ArchiveManager
import com.example.engine.ControlEngine
import com.example.engine.CrashLogger
import com.example.engine.DeviceAnalyzer
import com.example.engine.ExtractionProgress
import com.example.engine.GameAnalyzer
import com.example.engine.OptimizationEngine
import com.example.engine.OptimizationRecommendation
import com.example.engine.RuntimeCatalog
import com.example.engine.RuntimeDownloadManager
import com.example.engine.StorageCacheManager
import com.example.engine.StorageCheckResult
import com.example.engine.SubsystemStatus
import com.example.engine.controls.ControlsIntelligenceEngine
import com.example.engine.controls.GameControlProfile
import com.example.engine.controls.TouchInputManager
import com.example.engine.performance.AdaptiveOptimizationEngine
import com.example.engine.performance.GamePerformanceProfile
import com.example.engine.performance.PerformanceMonitor
import com.example.engine.performance.PerformanceOptimizationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ImportState {
    object Idle : ImportState()
    data class StorageWarning(val result: StorageCheckResult, val pendingUri: Uri) : ImportState()
    data class Extracting(val progress: ExtractionProgress) : ImportState()
    data class Analyzing(val stepText: String) : ImportState()
    data class MissingComponentsRequired(
        val analysis: GameLoadAnalysis,
        val missingComponents: List<RuntimeComponent>,
        val targetDir: File,
        val originalUri: Uri
    ) : ImportState()
    data class DownloadingGameRequirements(val activeComponentName: String?, val progress: Float) : ImportState()
    data class Success(val game: GameEntity, val recommendation: OptimizationRecommendation) : ImportState()
    data class Error(val message: String) : ImportState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository.getInstance(application)
    val downloadManager = RuntimeDownloadManager(application, viewModelScope)

    // Dedicated Independent Systems
    val performanceOptimizationEngine = PerformanceOptimizationEngine(application)
    val controlsIntelligenceEngine = ControlsIntelligenceEngine(application)
    val performanceMonitor = PerformanceMonitor(viewModelScope)
    val adaptiveOptimizationEngine = AdaptiveOptimizationEngine(
        scope = viewModelScope,
        optimizationEngine = performanceOptimizationEngine,
        performanceMonitor = performanceMonitor
    )
    val touchInputManager = TouchInputManager(application)

    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<GameEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Initialized synchronously with non-blocking safe default
    private val _deviceCapability = MutableStateFlow(DeviceAnalyzer.createSafeDefaultCapability())
    val deviceCapability: StateFlow<DeviceCapability> = _deviceCapability.asStateFlow()

    val gamingEnvironmentState: StateFlow<GamingEnvironmentState> = downloadManager.state

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _isSafeModeActive = MutableStateFlow(false)
    val isSafeModeActive: StateFlow<Boolean> = _isSafeModeActive.asStateFlow()

    private val _subsystems = MutableStateFlow<Map<String, SubsystemStatus>>(emptyMap())
    val subsystems: StateFlow<Map<String, SubsystemStatus>> = _subsystems.asStateFlow()

    init {
        initSafeStartup()
    }

    private fun initSafeStartup() {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()

                // 1. Check Safe Mode flag asynchronously
                val safeModeOn = withContext(Dispatchers.IO) {
                    CrashLogger.isSafeModeActive(app)
                }
                _isSafeModeActive.value = safeModeOn

                // 2. Perform device analysis in IO dispatcher without blocking main thread
                val capability = withContext(Dispatchers.IO) {
                    DeviceAnalyzer.analyzeDevice(app)
                }
                _deviceCapability.value = capability

                // 3. Track subsystem health
                _subsystems.value = CrashLogger.getAllSubsystemStatuses()

                // 4. Initialize first-launch runtime queue if environment is not configured
                if (downloadManager.state.value.isFirstLaunch || !downloadManager.state.value.isEnvironmentReady) {
                    val required = RuntimeCatalog.determineFirstLaunchComponents(capability)
                    downloadManager.prepareFirstLaunchQueue(required)
                }

                // 5. Mark clean startup after brief stability delay
                delay(800)
                withContext(Dispatchers.IO) {
                    CrashLogger.markSuccessfulStartup(app)
                }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "Error during initSafeStartup", t)
                CrashLogger.recordCrash(getApplication(), t, "ViewModelStartup", isFatal = false)
            }
        }
    }

    fun startFirstLaunchSetup(onComplete: (() -> Unit)? = null) {
        val required = RuntimeCatalog.determineFirstLaunchComponents(_deviceCapability.value)
        downloadManager.prepareFirstLaunchQueue(required)
        downloadManager.startOrResumeDownloads(onComplete)
    }

    fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    fun resumeDownloads(onComplete: (() -> Unit)? = null) {
        downloadManager.resumeDownloads(onComplete)
    }

    fun retryFailedDownloads(onComplete: (() -> Unit)? = null) {
        downloadManager.retryFailedDownloads(onComplete)
    }

    fun retrySingleComponent(componentId: String, onComplete: (() -> Unit)? = null) {
        downloadManager.retrySingleComponent(componentId, onComplete)
    }

    fun cancelSingleComponent(componentId: String) {
        downloadManager.cancelSingleComponent(componentId)
    }

    fun dismissDownloadError() {
        downloadManager.dismissError()
    }

    fun reinstallEnvironment(onComplete: (() -> Unit)? = null) {
        downloadManager.resetEnvironmentForReinstall()
        val required = RuntimeCatalog.determineFirstLaunchComponents(_deviceCapability.value)
        downloadManager.prepareFirstLaunchQueue(required)
        downloadManager.startOrResumeDownloads(onComplete)
    }

    fun refreshDeviceAnalysis() {
        viewModelScope.launch {
            val capability = withContext(Dispatchers.IO) {
                DeviceAnalyzer.analyzeDevice(getApplication())
            }
            _deviceCapability.value = capability
            if (downloadManager.state.value.isFirstLaunch || !downloadManager.state.value.isEnvironmentReady) {
                val required = RuntimeCatalog.determineFirstLaunchComponents(capability)
                downloadManager.prepareFirstLaunchQueue(required)
            }
        }
    }

    fun handleZipImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val estimatedSize = withContext(Dispatchers.IO) {
                    queryUriFileSize(app, uri)
                }

                // Storage safety check
                val storageCheck = withContext(Dispatchers.IO) {
                    ArchiveManager.checkStorageAvailable(app, estimatedSize)
                }

                if (!storageCheck.hasEnoughSpace) {
                    _importState.value = ImportState.StorageWarning(storageCheck, uri)
                    return@launch
                }

                startImportPipeline(uri, estimatedSize)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error initiating import", e)
                CrashLogger.recordCrash(getApplication(), e, "ArchiveImport", isFatal = false)
                _importState.value = ImportState.Error("Failed to open archive: ${e.localizedMessage}")
            }
        }
    }

    fun proceedDespiteStorageWarning(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val estimatedSize = withContext(Dispatchers.IO) {
                queryUriFileSize(app, uri)
            }
            startImportPipeline(uri, estimatedSize)
        }
    }

    private fun startImportPipeline(uri: Uri, estimatedArchiveSize: Long) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val gameId = "game_${System.currentTimeMillis()}"
                val targetDir = File(app.filesDir, "installed_games/$gameId")
                if (!targetDir.exists()) targetDir.mkdirs()

                // Step 1: Extraction with live progress
                _importState.value = ImportState.Extracting(ExtractionProgress(0, "Starting extraction…", 0L, estimatedArchiveSize, 0f))

                val extractResult = ArchiveManager.extractZip(
                    context = app,
                    zipUri = uri,
                    destDirectory = targetDir,
                    estimatedTotalBytes = estimatedArchiveSize,
                    onProgress = { progress ->
                        _importState.value = ImportState.Extracting(progress)
                    }
                )

                if (extractResult.isFailure) {
                    val ex = extractResult.exceptionOrNull()
                    _importState.value = ImportState.Error(ex?.localizedMessage ?: "Extraction failed")
                    return@launch
                }

                // Step 2: Automated Game Architecture & Engine Analysis
                _importState.value = ImportState.Analyzing("Analyzing game executable, shaders & engine signatures…")
                delay(300)

                val analysis = withContext(Dispatchers.IO) {
                    GameAnalyzer.analyzeDirectory(targetDir)
                }

                // Step 3: Check Installed Runtime vs Game Requirements
                val installedSet = downloadManager.state.value.installedComponentIds
                val missing = RuntimeCatalog.detectMissingComponentsForGame(
                    analysis = analysis,
                    installedComponentIds = installedSet,
                    device = _deviceCapability.value
                )

                if (missing.isNotEmpty()) {
                    _importState.value = ImportState.MissingComponentsRequired(
                        analysis = analysis,
                        missingComponents = missing,
                        targetDir = targetDir,
                        originalUri = uri
                    )
                    return@launch
                }

                // Proceed with complete optimization & control synthesis
                finalizeGameSetup(gameId, targetDir, uri.toString(), analysis)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Import pipeline error", e)
                CrashLogger.recordCrash(getApplication(), e, "GameImportPipeline", isFatal = false)
                _importState.value = ImportState.Error("Game setup error: ${e.localizedMessage}")
            }
        }
    }

    fun downloadMissingComponentsAndFinalize(missingState: ImportState.MissingComponentsRequired) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.DownloadingGameRequirements("Preparing download queue…", 0f)
                downloadManager.prepareGameSpecificQueue(missingState.missingComponents)

                downloadManager.startOrResumeDownloads(
                    onComplete = {
                        val gameId = missingState.targetDir.name
                        finalizeGameSetup(
                            gameId = gameId,
                            targetDir = missingState.targetDir,
                            archivePath = missingState.originalUri.toString(),
                            analysis = missingState.analysis
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed downloading missing components", e)
                _importState.value = ImportState.Error("Component download error: ${e.localizedMessage}")
            }
        }
    }

    private fun finalizeGameSetup(
        gameId: String,
        targetDir: File,
        archivePath: String,
        analysis: GameLoadAnalysis
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                _importState.value = ImportState.Analyzing("Synthesizing DXVK Vulkan async configuration…")
                delay(300)

                val optResult = performanceOptimizationEngine.generateRecommendation(analysis, _deviceCapability.value)

                _importState.value = ImportState.Analyzing("Generating adaptive touch layout for ${analysis.detectedGenre.name}…")
                delay(300)

                val controlProfile = controlsIntelligenceEngine.getOrGenerateProfile(gameId, analysis.detectedGenre)
                val controlLayout = ControlEngine.generateLayoutForGenre(analysis.detectedGenre)

                val recommendation = OptimizationRecommendation(
                    recommendedProfile = optResult.recommendedProfile,
                    generatedConfig = optResult.generatedConfig,
                    rationale = optResult.rationale,
                    deviceCapabilitySummary = optResult.deviceCapabilitySummary,
                    gameLoadSummary = optResult.gameLoadSummary,
                    performanceProfile = optResult.performanceProfile
                )

                val gameEntity = GameEntity(
                    id = gameId,
                    title = analysis.gameTitle,
                    archivePath = archivePath,
                    installDirectory = targetDir.absolutePath,
                    executableRelativePath = analysis.detectedExecutable,
                    sizeBytes = analysis.totalSizeBytes,
                    extractedSizeBytes = analysis.extractedEstimateBytes,
                    genre = analysis.detectedGenre,
                    detectedEngine = analysis.detectedEngine,
                    detectedGraphicsApi = analysis.detectedGraphicsApi,
                    detectedArchitecture = analysis.detectedArchitecture,
                    gameLoadScore = analysis.loadScore,
                    recommendedProfile = recommendation.recommendedProfile,
                    recommendationReason = recommendation.rationale,
                    activeProfile = recommendation.recommendedProfile,
                    controlLayoutJson = "",
                    configJson = "",
                    isSafeMode = _isSafeModeActive.value
                )

                withContext(Dispatchers.IO) {
                    StorageCacheManager.backupConfiguration(app, gameEntity)
                    repository.saveGame(gameEntity)
                }

                _importState.value = ImportState.Success(gameEntity, recommendation)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to finalize game setup", e)
                _importState.value = ImportState.Error("Game setup error: ${e.localizedMessage}")
            }
        }
    }

    fun loadSampleDemoGame() {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val device = _deviceCapability.value
                val gameId = "demo_speed_racer_${System.currentTimeMillis()}"

                _importState.value = ImportState.Analyzing("Setting up Indie PC DirectX 11 Demo…")
                delay(300)

                val demoDir = File(app.filesDir, "installed_games/$gameId")
                withContext(Dispatchers.IO) {
                    ArchiveManager.createSampleDemoGame(app, demoDir)
                }

                val analysis = withContext(Dispatchers.IO) {
                    GameAnalyzer.analyzeDirectory(demoDir, "Speed Racer 2026")
                }

                val optResult = performanceOptimizationEngine.generateRecommendation(analysis, device)
                val controlProfile = controlsIntelligenceEngine.getOrGenerateProfile(gameId, GameGenre.RACING)

                val recommendation = OptimizationRecommendation(
                    recommendedProfile = optResult.recommendedProfile,
                    generatedConfig = optResult.generatedConfig,
                    rationale = optResult.rationale,
                    deviceCapabilitySummary = optResult.deviceCapabilitySummary,
                    gameLoadSummary = optResult.gameLoadSummary,
                    performanceProfile = optResult.performanceProfile
                )

                val gameEntity = GameEntity(
                    id = gameId,
                    title = "Speed Racer 2026 (Demo)",
                    archivePath = "sample://speed_racer.zip",
                    installDirectory = demoDir.absolutePath,
                    executableRelativePath = "SpeedRacer2026.exe",
                    sizeBytes = 280L * 1024 * 1024,
                    extractedSizeBytes = 540L * 1024 * 1024,
                    genre = GameGenre.RACING,
                    detectedEngine = "Unity Engine",
                    detectedGraphicsApi = "DirectX 11 (DXVK 2.3)",
                    detectedArchitecture = "64-bit (x86_64)",
                    gameLoadScore = analysis.loadScore,
                    recommendedProfile = recommendation.recommendedProfile,
                    recommendationReason = recommendation.rationale,
                    activeProfile = recommendation.recommendedProfile,
                    controlLayoutJson = "",
                    configJson = "",
                    isSafeMode = false
                )

                withContext(Dispatchers.IO) {
                    StorageCacheManager.backupConfiguration(app, gameEntity)
                    repository.saveGame(gameEntity)
                }

                _importState.value = ImportState.Success(gameEntity, recommendation)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load sample demo", e)
                CrashLogger.recordCrash(getApplication(), e, "SampleDemoLoader", isFatal = false)
                _importState.value = ImportState.Error("Failed to initialize demo: ${e.localizedMessage}")
            }
        }
    }

    private fun queryUriFileSize(context: Application, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val size = cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) it.getLong(sizeIndex) else 100L * 1024 * 1024
                } else {
                    100L * 1024 * 1024
                }
            } ?: (100L * 1024 * 1024)
            size
        } catch (e: Exception) {
            100L * 1024 * 1024
        }
    }

    fun dismissImportState() {
        _importState.value = ImportState.Idle
    }

    fun updateGameProfile(game: GameEntity, profile: PerformanceProfile) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfile(game.id, profile)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update profile", e)
            }
        }
    }

    fun updateControlLayout(gameId: String, layout: ControlLayout) {
        viewModelScope.launch {
            try {
                val json = ControlEngine.serializeToJson(layout)
                withContext(Dispatchers.IO) {
                    repository.updateControlLayout(gameId, json)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update control layout", e)
            }
        }
    }

    fun autoOptimizeControls(gameId: String, genre: GameGenre, widthPx: Int, heightPx: Int, dpi: Int) {
        controlsIntelligenceEngine.autoOptimizeControls(gameId, genre, widthPx, heightPx, dpi)
    }

    fun startAdaptiveOptimization(gameId: String, targetFps: Int = 60) {
        performanceMonitor.startMonitoring(targetFps)
        adaptiveOptimizationEngine.startAdaptiveOptimization(gameId)
    }

    fun stopAdaptiveOptimization() {
        performanceMonitor.stopMonitoring()
        adaptiveOptimizationEngine.stopAdaptiveOptimization()
    }

    fun toggleSafeMode(game: GameEntity, enabled: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.setSafeMode(game.id, enabled)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to toggle safe mode", e)
            }
        }
    }

    fun clearGameCache(gameId: String): Boolean {
        return StorageCacheManager.clearGameCache(getApplication(), gameId)
    }

    fun restoreBackupConfig(game: GameEntity): Boolean {
        val json = StorageCacheManager.restoreBackupConfiguration(getApplication(), game.id)
        return if (json != null) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.updateConfig(game.id, json)
                }
            }
            true
        } else {
            false
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteGame(game)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to delete game", e)
            }
        }
    }

    fun toggleGlobalSafeMode(enabled: Boolean) {
        val app = getApplication<Application>()
        CrashLogger.setSafeModeActive(app, enabled)
        _isSafeModeActive.value = enabled
    }
}
