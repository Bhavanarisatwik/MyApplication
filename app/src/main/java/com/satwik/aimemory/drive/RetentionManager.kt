package com.satwik.aimemory.drive

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Enforces data retention policies on Google Drive summaries.
 *
 * Rules:
 * - Hourly summaries: auto-delete after [hourlyRetentionDays] (default 7)
 * - Daily summaries: auto-delete after [dailyRetentionDays] (default 90)
 * - Pinned summaries: exempt from deletion (detected by filename suffix "_pinned")
 *
 * Should run on app launch and periodically thereafter.
 */
class RetentionManager(
    private val driveStorage: DriveStorageService,
    private val hourlyRetentionDays: Int = 7,
    private val dailyRetentionDays: Int = 90
) {
    companion object {
        private const val TAG = "RetentionManager"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * Run the full retention sweep: check both hourly and daily folders.
     * @return Pair of (hourlyDeleted, dailyDeleted) counts
     */
    suspend fun enforceRetention(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val hourlyDeleted = sweepFolder(isDaily = false, retentionDays = hourlyRetentionDays)
        val dailyDeleted = sweepFolder(isDaily = true, retentionDays = dailyRetentionDays)

        Log.d(TAG, "Retention sweep complete: $hourlyDeleted hourly, $dailyDeleted daily files deleted")
        Pair(hourlyDeleted, dailyDeleted)
    }

    private suspend fun sweepFolder(isDaily: Boolean, retentionDays: Int): Int {
        val folderType = if (isDaily) "daily" else "hourly"
        val cutoffDate = LocalDate.now().minusDays(retentionDays.toLong())
        var deleted = 0

        try {
            val files = driveStorage.listSummaries(isDaily)

            for (file in files) {
                val fileName = file.name ?: continue

                // Skip pinned summaries
                if (fileName.contains("_pinned")) {
                    Log.d(TAG, "Skipping pinned: $fileName")
                    continue
                }

                // Extract date from filename (format: YYYY-MM-DD_HH.json or YYYY-MM-DD.json)
                val dateStr = extractDateFromFilename(fileName) ?: continue

                try {
                    val fileDate = LocalDate.parse(dateStr, DATE_FORMAT)
                    if (fileDate.isBefore(cutoffDate)) {
                        val success = driveStorage.deleteFile(file.id)
                        if (success) {
                            deleted++
                            Log.d(TAG, "Deleted expired $folderType file: $fileName (date: $fileDate)")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse date from $fileName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Retention sweep failed for $folderType folder", e)
        }

        return deleted
    }

    /**
     * Extract the date portion from a summary filename.
     * Supports: "2025-01-15_14.json" → "2025-01-15"
     *           "2025-01-15.json" → "2025-01-15"
     */
    private fun extractDateFromFilename(filename: String): String? {
        val baseName = filename.removeSuffix(".json").removeSuffix("_pinned")
        // Take first 10 chars which should be YYYY-MM-DD
        return if (baseName.length >= 10) {
            baseName.substring(0, 10)
        } else {
            null
        }
    }
}
