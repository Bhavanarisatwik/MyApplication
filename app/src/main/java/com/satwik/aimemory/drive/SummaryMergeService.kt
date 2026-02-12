package com.satwik.aimemory.drive

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.satwik.aimemory.data.model.MemorySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Merges hourly summaries into a single daily knowledge artifact.
 *
 * At day boundary:
 * 1. Fetches all hourly summaries for the previous day
 * 2. Combines topics, decisions, tasks across all hours
 * 3. Uploads the consolidated daily summary
 * 4. Optionally deletes hourly files after successful merge
 */
class SummaryMergeService(
    private val driveStorage: DriveStorageService
) {
    companion object {
        private const val TAG = "SummaryMergeService"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val gson = Gson()
    }

    /**
     * Data class representing a daily consolidated summary.
     */
    data class DailyConsolidation(
        val date: String,
        val totalHours: Int,
        val allTopics: List<String>,
        val allDecisions: List<String>,
        val allTasks: List<String>,
        val dominantTone: String,
        val hourlySummaries: List<MemorySummary>
    )

    /**
     * Merge all hourly summaries for a given date into a daily summary.
     * @param date The date to consolidate
     * @param deleteHourlyAfterMerge If true, delete hourly files after successful merge
     * @return true if merge completed successfully
     */
    suspend fun mergeDay(
        date: LocalDate,
        deleteHourlyAfterMerge: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dateStr = date.format(DATE_FORMAT)
            Log.d(TAG, "Starting daily merge for $dateStr")

            // 1. List all hourly files
            val hourlyFiles = driveStorage.listSummaries(isDaily = false)
            val dayFiles = hourlyFiles.filter { it.name.startsWith(dateStr) }

            if (dayFiles.isEmpty()) {
                Log.d(TAG, "No hourly files found for $dateStr")
                return@withContext true // Nothing to merge
            }

            // 2. Download and parse each hourly summary
            val summaries = mutableListOf<MemorySummary>()
            for (file in dayFiles) {
                val content = driveStorage.downloadSummary(file.id) ?: continue
                try {
                    val summary = gson.fromJson(content, MemorySummary::class.java)
                    summaries.add(summary)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse ${file.name}", e)
                }
            }

            if (summaries.isEmpty()) {
                Log.d(TAG, "No valid summaries parsed for $dateStr")
                return@withContext true
            }

            // 3. Consolidate into daily summary
            val consolidation = DailyConsolidation(
                date = dateStr,
                totalHours = summaries.size,
                allTopics = summaries.flatMap { it.topics }.distinct(),
                allDecisions = summaries.flatMap { it.decisions }.distinct(),
                allTasks = summaries.flatMap { it.tasks }.distinct(),
                dominantTone = findDominantTone(summaries),
                hourlySummaries = summaries.sortedBy { it.timestamp }
            )

            // 4. Upload daily summary
            val dailyJson = gson.toJson(consolidation)
            val dailyFileName = "$dateStr.json"
            val fileId = driveStorage.uploadSummary(dailyFileName, dailyJson, isDaily = true)

            if (fileId == null) {
                Log.e(TAG, "Failed to upload daily summary for $dateStr")
                return@withContext false
            }

            Log.d(TAG, "Daily summary uploaded: $dailyFileName")

            // 5. Optionally delete hourly files
            if (deleteHourlyAfterMerge) {
                for (file in dayFiles) {
                    driveStorage.deleteFile(file.id)
                }
                Log.d(TAG, "Deleted ${dayFiles.size} hourly files for $dateStr")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Daily merge failed", e)
            false
        }
    }

    /**
     * Find the most common emotional tone across summaries.
     */
    private fun findDominantTone(summaries: List<MemorySummary>): String {
        return summaries
            .groupingBy { it.emotionalTone.lowercase() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "neutral"
    }
}
