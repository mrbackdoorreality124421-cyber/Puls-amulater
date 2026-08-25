package com.example.engine

import com.example.data.model.ComponentCategory
import com.example.data.model.ComponentScope
import com.example.data.model.DeviceCapability
import com.example.data.model.GameGenre
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.RuntimeComponent

object RuntimeCatalog {

    const val LATEST_CATALOG_VERSION = "2.4.0"

    val ALL_COMPONENTS: List<RuntimeComponent> = listOf(
        RuntimeComponent(
            id = "wine_runtime_core",
            name = "Wine 9.2 Staging (Windows Compatibility)",
            version = "9.2.1-proton",
            category = ComponentCategory.WINDOWS_COMPATIBILITY,
            scope = ComponentScope.CORE,
            sizeBytes = 148L * 1024L * 1024L,
            formattedSize = "148 MB",
            sha256Checksum = "9e2c65a83b9c47e8412e8790b4142f1f3a8b27c65d9e018a2bc491f2b604e0a1",
            targetSubdir = "Wine/wine-9.2",
            description = "Provides complete Win32/Win64 API compatibility layer, isolated prefix sandbox, and core DLL hooks."
        ),
        RuntimeComponent(
            id = "box64_runtime",
            name = "Box64 x86_64 Dynarec Translator",
            version = "0.2.8-release",
            category = ComponentCategory.CPU_TRANSLATION,
            scope = ComponentScope.CORE,
            sizeBytes = 34L * 1024L * 1024L,
            formattedSize = "34 MB",
            sha256Checksum = "e5b81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8c90",
            targetSubdir = "Box64/v0.2.8",
            description = "High-performance dynamic recompiler translating x86_64 CPU instructions to native ARM64 with vectorized extensions.",
            requiredForArch = listOf("arm64-v8a", "universal")
        ),
        RuntimeComponent(
            id = "box86_runtime",
            name = "Box86 32-bit x86 Dynarec Translator",
            version = "0.3.2-release",
            category = ComponentCategory.CPU_TRANSLATION,
            scope = ComponentScope.OPTIONAL,
            sizeBytes = 24L * 1024L * 1024L,
            formattedSize = "24 MB",
            sha256Checksum = "7b2c918a3e9c47e8412e8790b4142f1f3a8b27c65d9e018a2bc491f2b604e03b",
            targetSubdir = "Box86/v0.3.2",
            description = "Translates 32-bit x86 games and legacy launchers with custom dynarec opcode optimizations.",
            dependencies = listOf("wine_runtime_core")
        ),
        RuntimeComponent(
            id = "dxvk_vulkan",
            name = "DXVK 2.3.1 (DirectX 9/10/11 → Vulkan)",
            version = "2.3.1-async",
            category = ComponentCategory.GRAPHICS_TRANSLATION,
            scope = ComponentScope.CORE,
            sizeBytes = 18L * 1024L * 1024L,
            formattedSize = "18 MB",
            sha256Checksum = "3a8c1f92e0714b6e5b8821a4f913d077c5b6a71e2c94318fb4a1e948a31e8d44",
            targetSubdir = "DXVK/v2.3.1",
            description = "Vulkan-based translation layer for D3D9, D3D10, and D3D11 with asynchronous pipeline compilation.",
            requiresVulkan = true
        ),
        RuntimeComponent(
            id = "vkd3d_proton",
            name = "VKD3D-Proton 2.11 (DirectX 12 → Vulkan)",
            version = "2.11.0",
            category = ComponentCategory.GRAPHICS_TRANSLATION,
            scope = ComponentScope.OPTIONAL,
            sizeBytes = 28L * 1024L * 1024L,
            formattedSize = "28 MB",
            sha256Checksum = "4c9e81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8e55",
            targetSubdir = "VKD3D/v2.11",
            description = "Direct3D 12 to Vulkan translation layer enabling DirectX 12 games with root signature caching.",
            requiresVulkan = true,
            requiresVulkan13 = true,
            dependencies = listOf("dxvk_vulkan")
        ),
        RuntimeComponent(
            id = "graphics_driver_support",
            name = "GPU & Driver Infrastructure (Turnip/Vulkan ICD)",
            version = "24.1.0-mesa",
            category = ComponentCategory.GRAPHICS_DEVICE_SUPPORT,
            scope = ComponentScope.CORE,
            sizeBytes = 42L * 1024L * 1024L,
            formattedSize = "42 MB",
            sha256Checksum = "5d1f81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8f66",
            targetSubdir = "Graphics/drivers",
            description = "Hardware-optimized driver dispatch layer with Qualcomm Turnip & ARM Mali low-overhead configurations."
        ),
        RuntimeComponent(
            id = "input_controller_layer",
            name = "Input & Virtual Controller Bridge",
            version = "1.4.2",
            category = ComponentCategory.INPUT_LAYER,
            scope = ComponentScope.CORE,
            sizeBytes = 12L * 1024L * 1024L,
            formattedSize = "12 MB",
            sha256Checksum = "6e2a81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8a77",
            targetSubdir = "Input/bridge",
            description = "Zero-latency DirectInput, XInput 1.4, virtual mouse touchpad, and multi-touch controller bridge."
        ),
        RuntimeComponent(
            id = "runtime_libraries",
            name = "Runtime C++ & DirectShow Media Libraries",
            version = "2022.4",
            category = ComponentCategory.RUNTIME_LIBRARIES,
            scope = ComponentScope.CORE,
            sizeBytes = 64L * 1024L * 1024L,
            formattedSize = "64 MB",
            sha256Checksum = "7f3b81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8b88",
            targetSubdir = "Libraries/redist",
            description = "Microsoft Visual C++ (2015-2022), OpenAL 1.1, FAudio, dsound, and Windows Media codec redistributables.",
            dependencies = listOf("wine_runtime_core")
        ),
        RuntimeComponent(
            id = "shader_infrastructure",
            name = "Async Shader Pipeline & State Cache",
            version = "1.8.0",
            category = ComponentCategory.SHADER_INFRASTRUCTURE,
            scope = ComponentScope.CORE,
            sizeBytes = 16L * 1024L * 1024L,
            formattedSize = "16 MB",
            sha256Checksum = "8a4c81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8c99",
            targetSubdir = "Shaders/cache_engine",
            description = "Pre-compiled SPIR-V shader cache, async shader compilation manager, and stutter-elimination cache."
        ),
        RuntimeComponent(
            id = "fonts_compatibility_data",
            name = "Windows System Fonts & Locale Data",
            version = "3.1.0",
            category = ComponentCategory.FONTS_DATA,
            scope = ComponentScope.CORE,
            sizeBytes = 22L * 1024L * 1024L,
            formattedSize = "22 MB",
            sha256Checksum = "9b5d81a7d609214ef5e9821a4f913d077c5b6a71e2c94318fb4a1e948a31e8d00",
            targetSubdir = "Fonts/system",
            description = "DirectWrite compatible TrueType fonts (Arial, Tahoma, Consolas, Segoe UI) and system registry entries."
        )
    )

    fun getComponentById(id: String): RuntimeComponent? {
        return ALL_COMPONENTS.firstOrNull { it.id == id }
    }

    /**
     * Determines which components are required for the device during First Launch
     * based on CPU architecture, GPU, Vulkan version, and RAM.
     */
    fun determineFirstLaunchComponents(device: DeviceCapability): List<RuntimeComponent> {
        val required = mutableListOf<RuntimeComponent>()

        for (comp in ALL_COMPONENTS) {
            // Check architecture compatibility
            if (comp.requiredForArch.isNotEmpty() && !comp.requiredForArch.contains("universal")) {
                val archMatches = comp.requiredForArch.any { device.cpuArchitecture.contains(it, ignoreCase = true) }
                if (!archMatches) continue
            }

            // Check Vulkan requirements
            if (comp.requiresVulkan && !device.isVulkanSupported) {
                continue
            }
            if (comp.requiresVulkan13 && !device.vulkanVersion.contains("1.3")) {
                // If device lacks Vulkan 1.3, keep VKD3D as optional / do not auto-include in core first launch
                continue
            }

            // Include CORE components for initial setup
            if (comp.scope == ComponentScope.CORE) {
                required.add(comp)
            }
        }

        // Sort by dependency order so prerequisites are placed first
        return resolveDependencyOrder(required)
    }

    /**
     * Identifies missing runtime requirements for a specific imported game.
     */
    fun detectMissingComponentsForGame(
        analysis: GameLoadAnalysis,
        installedComponentIds: Set<String>,
        device: DeviceCapability
    ): List<RuntimeComponent> {
        val needed = mutableListOf<RuntimeComponent>()

        // 1. Check if 32-bit legacy game needs Box86
        val is32Bit = analysis.detectedArchitecture.contains("32") || analysis.detectedArchitecture.contains("x86") && !analysis.detectedArchitecture.contains("64")
        if (is32Bit && !installedComponentIds.contains("box86_runtime")) {
            getComponentById("box86_runtime")?.let { needed.add(it) }
        }

        // 2. Check if DirectX 12 game needs VKD3D-Proton
        val isDx12 = analysis.detectedGraphicsApi.contains("12", ignoreCase = true) || analysis.detectedDlls.any { it.contains("d3d12", ignoreCase = true) }
        if (isDx12 && !installedComponentIds.contains("vkd3d_proton") && device.isVulkanSupported) {
            getComponentById("vkd3d_proton")?.let { needed.add(it) }
        }

        // 3. Check if DirectX 9/10/11 game is missing DXVK
        val isDirectX = analysis.detectedGraphicsApi.contains("DirectX", ignoreCase = true) || analysis.detectedGraphicsApi.contains("DX", ignoreCase = true)
        if (isDirectX && !installedComponentIds.contains("dxvk_vulkan") && device.isVulkanSupported) {
            getComponentById("dxvk_vulkan")?.let { needed.add(it) }
        }

        // 4. Ensure core prerequisites are present
        for (coreComp in ALL_COMPONENTS.filter { it.scope == ComponentScope.CORE }) {
            if (!installedComponentIds.contains(coreComp.id)) {
                needed.add(coreComp)
            }
        }

        return resolveDependencyOrder(needed.distinctBy { it.id })
    }

    /**
     * Resolves dependency ordering using topological sort.
     */
    fun resolveDependencyOrder(components: List<RuntimeComponent>): List<RuntimeComponent> {
        val result = mutableListOf<RuntimeComponent>()
        val visited = mutableSetOf<String>()

        fun visit(comp: RuntimeComponent) {
            if (visited.contains(comp.id)) return
            for (depId in comp.dependencies) {
                val depComp = ALL_COMPONENTS.firstOrNull { it.id == depId }
                if (depComp != null && !visited.contains(depId)) {
                    visit(depComp)
                }
            }
            visited.add(comp.id)
            result.add(comp)
        }

        for (comp in components) {
            visit(comp)
        }

        return result
    }
}
