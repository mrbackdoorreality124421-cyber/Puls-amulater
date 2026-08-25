package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ComponentScope
import com.example.data.model.DeviceCapability
import com.example.data.model.DevicePerformanceTier
import com.example.data.model.GameGenre
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.GameLoadScore
import com.example.data.model.PerformanceProfile
import com.example.engine.ControlEngine
import com.example.engine.CrashLogger
import com.example.engine.DeviceAnalyzer
import com.example.engine.OptimizationEngine
import com.example.engine.RuntimeCatalog
import com.example.engine.RuntimeDownloadManager
import com.example.engine.SubsystemStatus
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PulsePC", appName)
  }

  @Test
  fun `verify device analyzer runs safely on application context without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val capability = DeviceAnalyzer.analyzeDevice(context)
    assertNotNull(capability)
    assertTrue(capability.totalRamBytes > 0)
    assertTrue(capability.cpuCores >= 1)
    assertNotNull(capability.gpuRenderer)
    assertNotNull(capability.displayResolution)
  }

  @Test
  fun `verify crash logger and safe mode mechanisms`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    CrashLogger.resetCrashCount(context)
    CrashLogger.setSafeModeActive(context, false)
    assertFalse(CrashLogger.isSafeModeActive(context))

    // Record non-fatal crash
    val entry = CrashLogger.recordCrash(
      context = context,
      throwable = IllegalStateException("Simulated transient subsystem error"),
      subsystem = "TestEngine",
      isFatal = false
    )
    assertNotNull(entry)
    assertEquals("TestEngine", entry.subsystem)
    assertEquals(SubsystemStatus.DEGRADED, CrashLogger.getSubsystemStatus("TestEngine"))

    // Test consecutive crash loop trigger
    CrashLogger.incrementConsecutiveCrashCount(context)
    CrashLogger.incrementConsecutiveCrashCount(context)
    assertTrue(CrashLogger.isSafeModeActive(context))

    // Clean startup resets
    CrashLogger.markSuccessfulStartup(context)
    assertFalse(CrashLogger.isSafeModeActive(context))
  }

  @Test
  fun `verify runtime catalog first launch component resolution and dependencies`() {
    val device = DeviceCapability(
      socModel = "Snapdragon 8 Gen 2",
      cpuArchitecture = "arm64-v8a",
      cpuCores = 8,
      cpuMaxFreqGhz = 3.2f,
      totalRamBytes = 12L * 1024L * 1024L * 1024L,
      availableRamBytes = 8L * 1024L * 1024L * 1024L,
      totalStorageBytes = 256L * 1024L * 1024L * 1024L,
      freeStorageBytes = 180L * 1024L * 1024L * 1024L,
      gpuRenderer = "Adreno 740",
      gpuVendor = "Qualcomm",
      openGlesVersion = "3.2",
      isVulkanSupported = true,
      vulkanVersion = "1.3",
      displayRefreshRateHz = 120,
      displayResolution = "1440x3200",
      androidVersion = "Android 14",
      is64BitSupported = true,
      thermalLevel = 0,
      thermalStatusString = "Nominal",
      performanceScore = 90,
      performanceTier = DevicePerformanceTier.ULTRA_FLAGSHIP
    )

    val firstLaunchComps = RuntimeCatalog.determineFirstLaunchComponents(device)
    assertTrue(firstLaunchComps.isNotEmpty())
    assertTrue(firstLaunchComps.any { it.id == "wine_runtime_core" })
    assertTrue(firstLaunchComps.any { it.id == "box64_runtime" })
    assertTrue(firstLaunchComps.any { it.id == "dxvk_vulkan" })

    // Check dependency resolution: Wine comes before redist runtime libraries
    val wineIndex = firstLaunchComps.indexOfFirst { it.id == "wine_runtime_core" }
    val redistIndex = firstLaunchComps.indexOfFirst { it.id == "runtime_libraries" }
    if (wineIndex != -1 && redistIndex != -1) {
      assertTrue(wineIndex < redistIndex)
    }
  }

  @Test
  fun `verify missing components detection for specific game architectures`() {
    val device = DeviceCapability(
      socModel = "Snapdragon 8 Gen 2",
      cpuArchitecture = "arm64-v8a",
      cpuCores = 8,
      cpuMaxFreqGhz = 3.2f,
      totalRamBytes = 12L * 1024L * 1024L * 1024L,
      availableRamBytes = 8L * 1024L * 1024L * 1024L,
      totalStorageBytes = 256L * 1024L * 1024L * 1024L,
      freeStorageBytes = 180L * 1024L * 1024L * 1024L,
      gpuRenderer = "Adreno 740",
      gpuVendor = "Qualcomm",
      openGlesVersion = "3.2",
      isVulkanSupported = true,
      vulkanVersion = "1.3",
      displayRefreshRateHz = 120,
      displayResolution = "1440x3200",
      androidVersion = "Android 14",
      is64BitSupported = true,
      thermalLevel = 0,
      thermalStatusString = "Nominal",
      performanceScore = 90,
      performanceTier = DevicePerformanceTier.ULTRA_FLAGSHIP
    )

    val legacy32BitAnalysis = GameLoadAnalysis(
      gameTitle = "Classic 32-bit Game",
      totalFiles = 80,
      totalSizeBytes = 300L * 1024 * 1024,
      extractedEstimateBytes = 600L * 1024 * 1024,
      detectedExecutable = "classic.exe",
      detectedArchitecture = "32-bit (x86)",
      detectedEngine = "Custom Engine",
      detectedGraphicsApi = "DirectX 9",
      detectedDlls = listOf("d3d9.dll"),
      shaderFilesCount = 4,
      textureFilesCount = 20,
      audioFilesCount = 10,
      videoFilesCount = 1,
      largeAssetsSizeMb = 120L,
      loadScore = GameLoadScore.LIGHT,
      estimatedCpuWeight = 25,
      estimatedGpuWeight = 30,
      estimatedRamMb = 800,
      detectedGenre = GameGenre.PLATFORMER,
      compatibilitySummary = "Requires Box86",
      potentialBottlenecks = emptyList()
    )

    // Suppose only core 64-bit components are installed
    val installedSet = setOf("wine_runtime_core", "box64_runtime", "dxvk_vulkan", "input_controller_layer")
    val missing = RuntimeCatalog.detectMissingComponentsForGame(legacy32BitAnalysis, installedSet, device)

    // Should detect that box86_runtime is required
    assertTrue(missing.any { it.id == "box86_runtime" })
  }

  @Test
  fun `verify optimization matrix mapping`() {
    val analysis = GameLoadAnalysis(
      gameTitle = "Test Game",
      totalFiles = 120,
      totalSizeBytes = 1024L * 1024L * 500L,
      extractedEstimateBytes = 1024L * 1024L * 800L,
      detectedExecutable = "game.exe",
      detectedArchitecture = "x64",
      detectedEngine = "Unity",
      detectedGraphicsApi = "DirectX 11",
      detectedDlls = listOf("d3d11.dll", "UnityPlayer.dll"),
      shaderFilesCount = 10,
      textureFilesCount = 40,
      audioFilesCount = 20,
      videoFilesCount = 2,
      largeAssetsSizeMb = 350L,
      loadScore = GameLoadScore.MODERATE,
      estimatedCpuWeight = 45,
      estimatedGpuWeight = 50,
      estimatedRamMb = 1600,
      detectedGenre = GameGenre.SHOOTER,
      compatibilitySummary = "Great compatibility",
      potentialBottlenecks = emptyList()
    )

    val device = DeviceCapability(
      socModel = "Snapdragon 778G",
      cpuArchitecture = "arm64-v8a",
      cpuCores = 8,
      cpuMaxFreqGhz = 2.4f,
      totalRamBytes = 6L * 1024L * 1024L * 1024L,
      availableRamBytes = 3L * 1024L * 1024L * 1024L,
      totalStorageBytes = 128L * 1024L * 1024L * 1024L,
      freeStorageBytes = 64L * 1024L * 1024L * 1024L,
      gpuRenderer = "Adreno 642L",
      gpuVendor = "Qualcomm",
      openGlesVersion = "3.2",
      isVulkanSupported = true,
      vulkanVersion = "1.3",
      displayRefreshRateHz = 120,
      displayResolution = "1080x2400",
      androidVersion = "Android 14",
      is64BitSupported = true,
      thermalLevel = 0,
      thermalStatusString = "Nominal",
      performanceScore = 55,
      performanceTier = DevicePerformanceTier.MID
    )

    val recommendation = OptimizationEngine.generateRecommendation(analysis, device)
    assertEquals(PerformanceProfile.SMOOTH, recommendation.recommendedProfile)
  }

  @Test
  fun `verify control engine layout generation`() {
    val layout = ControlEngine.generateLayoutForGenre(GameGenre.SHOOTER)
    assertNotNull(layout)
    assertTrue(layout.elements.isNotEmpty())

    val tpsLayout = ControlEngine.generateLayoutForGenre(GameGenre.TPS)
    assertNotNull(tpsLayout)
    assertTrue(tpsLayout.elements.isNotEmpty())
  }

  @Test
  fun `verify main view model initializes safely`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(application)
    assertNotNull(viewModel.deviceCapability.value)
    assertNotNull(viewModel.allGames.value)
    assertNotNull(viewModel.gamingEnvironmentState.value)
  }
}
