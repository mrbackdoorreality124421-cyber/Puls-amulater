package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ControlLayout
import com.example.data.model.DeviceCapability
import com.example.data.model.GameConfig
import com.example.data.model.GameEntity
import com.example.data.model.PerformanceProfile
import com.example.data.repository.GameRepository
import com.example.engine.ControlEngine
import com.example.engine.CrashLogger
import com.example.engine.DeviceAnalyzer
import com.example.engine.OptimizationEngine
import com.example.engine.RuntimeCompatibilityLayer
import com.example.engine.RuntimeState
import com.example.engine.RuntimeTelemetry
import com.example.engine.ThermalProtectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamePlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository.getInstance(application)
    private val runtime = RuntimeCompatibilityLayer(application, viewModelScope)
    private val thermalManager = ThermalProtectionManager(application)

    val runtimeState: StateFlow<RuntimeState> = runtime.state
    val telemetry: StateFlow<RuntimeTelemetry> = runtime.telemetry
    val thermalState = thermalManager.thermalState

    private val _currentGame = MutableStateFlow<GameEntity?>(null)
    val currentGame: StateFlow<GameEntity?> = _currentGame.asStateFlow()

    private val _controlLayout = MutableStateFlow(ControlLayout())
    val controlLayout: StateFlow<ControlLayout> = _controlLayout.asStateFlow()

    private val _showPerformanceHud = MutableStateFlow(true)
    val showPerformanceHud: StateFlow<Boolean> = _showPerformanceHud.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun initializeGameSession(gameId: String) {
        viewModelScope.launch {
            try {
                val game = withContext(Dispatchers.IO) {
                    repository.getGameById(gameId)
                } ?: return@launch

                _currentGame.value = game

                val layout = ControlEngine.deserializeFromJson(game.controlLayoutJson, game.genre)
                _controlLayout.value = layout

                val device = withContext(Dispatchers.IO) {
                    DeviceAnalyzer.analyzeDevice(getApplication())
                }
                val baseConfig = OptimizationEngine.applyProfileToConfig(GameConfig(), game.activeProfile)

                thermalManager.startMonitoring(game.activeProfile)
                runtime.launchGame(
                    game = game,
                    config = baseConfig,
                    profile = game.activeProfile,
                    device = device,
                    isSafeMode = game.isSafeMode
                )
            } catch (t: Throwable) {
                Log.e("GamePlayerViewModel", "Error in initializeGameSession", t)
                CrashLogger.recordCrash(getApplication(), t, "GameSessionStartup", isFatal = false)
            }
        }
    }

    fun togglePerformanceHud() {
        _showPerformanceHud.value = !_showPerformanceHud.value
    }

    fun pauseGame() {
        _isPaused.value = true
        runtime.pause()
    }

    fun resumeGame() {
        _isPaused.value = false
        runtime.resume()
    }

    fun switchActiveProfile(profile: PerformanceProfile) {
        val game = _currentGame.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfile(game.id, profile)
                }
                _currentGame.value = game.copy(activeProfile = profile)
                val device = withContext(Dispatchers.IO) {
                    DeviceAnalyzer.analyzeDevice(getApplication())
                }
                val config = OptimizationEngine.applyProfileToConfig(GameConfig(), profile)
                runtime.launchGame(game, config, profile, device, game.isSafeMode)
            } catch (t: Throwable) {
                Log.e("GamePlayerViewModel", "Error switching profile", t)
            }
        }
    }

    fun launchSafeMode() {
        val game = _currentGame.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.setSafeMode(game.id, true)
                }
                _currentGame.value = game.copy(isSafeMode = true)
                val device = withContext(Dispatchers.IO) {
                    DeviceAnalyzer.analyzeDevice(getApplication())
                }
                val config = OptimizationEngine.applyProfileToConfig(GameConfig(), PerformanceProfile.SUPER_SMOOTH)
                runtime.launchGame(game, config, PerformanceProfile.SUPER_SMOOTH, device, isSafeMode = true)
            } catch (t: Throwable) {
                Log.e("GamePlayerViewModel", "Error launching safe mode", t)
            }
        }
    }

    fun handleInputTrigger(key: String, isDown: Boolean) {
        // Dispatches to runtime input queue
    }

    fun terminateSession() {
        val game = _currentGame.value
        val playDuration = runtime.terminate()
        thermalManager.stopMonitoring()
        if (game != null && playDuration > 0) {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository.recordGameSession(game.id, playDuration)
                    }
                } catch (t: Throwable) {
                    Log.e("GamePlayerViewModel", "Error recording session", t)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runtime.terminate()
        thermalManager.stopMonitoring()
    }
}
