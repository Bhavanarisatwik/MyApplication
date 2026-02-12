package com.satwik.aimemory.drive

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles all Google Drive file operations for summary storage.
 *
 * Folder structure:
 * - AiMemory/hourly/YYYY-MM-DD_HH.json
 * - AiMemory/daily/YYYY-MM-DD.json
 *
 * Uses `drive.file` scope — can only access files created by this app.
 */
class DriveStorageService(
    private val context: Context
) {
    companion object {
        private const val TAG = "DriveStorageService"
        private const val APP_FOLDER_NAME = "AiMemory"
        private const val HOURLY_FOLDER_NAME = "hourly"
        private const val DAILY_FOLDER_NAME = "daily"
        private const val MIME_JSON = "application/json"
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    }

    private var driveService: Drive? = null

    /**
     * Initialize the Drive service with the signed-in account.
     */
    fun initialize(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("AiMemory")
            .build()

        Log.d(TAG, "Drive service initialized for ${account.email}")
    }

    /**
     * Ensure the app folder structure exists:
     * AiMemory/ → hourly/ + daily/
     */
    suspend fun ensureFolderStructure(): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext false

            val appFolderId = findOrCreateFolder(drive, APP_FOLDER_NAME, "root")
            findOrCreateFolder(drive, HOURLY_FOLDER_NAME, appFolderId)
            findOrCreateFolder(drive, DAILY_FOLDER_NAME, appFolderId)

            Log.d(TAG, "Folder structure verified")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create folder structure", e)
            false
        }
    }

    /**
     * Upload a summary JSON file to the appropriate folder.
     * @param fileName e.g., "2025-01-15_14.json" for hourly, "2025-01-15.json" for daily
     * @param jsonContent The serialized summary JSON
     * @param isDaily true = daily folder, false = hourly folder
     */
    suspend fun uploadSummary(
        fileName: String,
        jsonContent: String,
        isDaily: Boolean
    ): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            val appFolderId = findOrCreateFolder(drive, APP_FOLDER_NAME, "root")
            val targetFolder = if (isDaily) DAILY_FOLDER_NAME else HOURLY_FOLDER_NAME
            val targetFolderId = findOrCreateFolder(drive, targetFolder, appFolderId)

            // Check if file already exists (update instead of create)
            val existing = findFile(drive, fileName, targetFolderId)
            val fileId = if (existing != null) {
                // Update existing file
                val content = ByteArrayContent.fromString(MIME_JSON, jsonContent)
                drive.files().update(existing.id, null, content).execute()
                Log.d(TAG, "Updated existing file: $fileName")
                existing.id
            } else {
                // Create new file
                val metadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(targetFolderId)
                    mimeType = MIME_JSON
                }
                val content = ByteArrayContent.fromString(MIME_JSON, jsonContent)
                val file = drive.files().create(metadata, content)
                    .setFields("id, name")
                    .execute()
                Log.d(TAG, "Created new file: $fileName (${file.id})")
                file.id
            }

            fileId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload summary: $fileName", e)
            null
        }
    }

    /**
     * List all summary files in a folder.
     * @param isDaily true = daily folder, false = hourly folder
     * @return List of file metadata (id, name, modifiedTime)
     */
    suspend fun listSummaries(isDaily: Boolean): List<DriveFile> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext emptyList()

            val appFolderId = findFolderId(drive, APP_FOLDER_NAME, "root")
                ?: return@withContext emptyList()
            val targetFolder = if (isDaily) DAILY_FOLDER_NAME else HOURLY_FOLDER_NAME
            val targetFolderId = findFolderId(drive, targetFolder, appFolderId)
                ?: return@withContext emptyList()

            val result = drive.files().list()
                .setQ("'$targetFolderId' in parents and trashed = false")
                .setFields("files(id, name, modifiedTime, size)")
                .setOrderBy("name desc")
                .setPageSize(100)
                .execute()

            result.files ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list summaries", e)
            emptyList()
        }
    }

    /**
     * Download a summary file's content.
     */
    suspend fun downloadSummary(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null
            val outputStream = java.io.ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download summary: $fileId", e)
            null
        }
    }

    /**
     * Delete a file from Drive by ID.
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext false
            drive.files().delete(fileId).execute()
            Log.d(TAG, "Deleted file: $fileId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file: $fileId", e)
            false
        }
    }

    // --- Private helpers ---

    private fun findOrCreateFolder(drive: Drive, name: String, parentId: String): String {
        return findFolderId(drive, name, parentId) ?: createFolder(drive, name, parentId)
    }

    private fun findFolderId(drive: Drive, name: String, parentId: String): String? {
        val result = drive.files().list()
            .setQ("name = '$name' and '$parentId' in parents and mimeType = '$MIME_FOLDER' and trashed = false")
            .setFields("files(id)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }

    private fun createFolder(drive: Drive, name: String, parentId: String): String {
        val metadata = DriveFile().apply {
            this.name = name
            this.parents = listOf(parentId)
            this.mimeType = MIME_FOLDER
        }
        val folder = drive.files().create(metadata)
            .setFields("id")
            .execute()
        Log.d(TAG, "Created folder: $name (${folder.id})")
        return folder.id
    }

    private fun findFile(drive: Drive, name: String, parentId: String): DriveFile? {
        val result = drive.files().list()
            .setQ("name = '$name' and '$parentId' in parents and trashed = false")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()
    }
}
