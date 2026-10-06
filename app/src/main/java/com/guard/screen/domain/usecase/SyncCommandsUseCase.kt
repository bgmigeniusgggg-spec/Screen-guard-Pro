package com.guard.screen.domain.usecase

import com.guard.screen.core.AppResult
import com.guard.screen.core.Logger
import com.guard.screen.data.model.Command
import com.guard.screen.data.repository.CommandRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase se pending commands fetch karta hai aur process karta hai.
 */
@Singleton
class SyncCommandsUseCase @Inject constructor(
    private val commandRepository: CommandRepository,
    private val processCommandUseCase: ProcessCommandUseCase
) {

    /**
     * Pending commands sync karo aur process karo.
     */
    suspend operator fun invoke(deviceKey: String): AppResult<Int> {
        Logger.d("SyncCommands", "Syncing for $deviceKey")

        return when (val result = commandRepository.fetchPendingCommands(deviceKey)) {
            is AppResult.Success -> {
                val commands = result.data

                if (commands.isEmpty()) {
                    Logger.d("SyncCommands", "No pending commands")
                    return AppResult.Success(0)
                }

                Logger.i("SyncCommands", "Found ${commands.size} pending commands")

                var processedCount = 0
                commands.forEach { command ->
                    try {
                        val processResult = processCommandUseCase(command)
                        if (processResult is AppResult.Success) {
                            processedCount++
                        }
                    } catch (e: Exception) {
                        Logger.e("SyncCommands", "Failed to process: ${command.cmd}", e)
                    }
                }

                Logger.i("SyncCommands", "Processed $processedCount/${commands.size}")
                AppResult.Success(processedCount)
            }
            is AppResult.Error -> {
                Logger.e("SyncCommands", "Fetch failed: ${result.message}")
                result
            }
            else -> AppResult.Success(0)
        }
    }
}
