package io.github.cxpexporter.data

import android.app.Activity
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException

class SaveCancelledException : Exception("Save cancelled")

object PasswordSaver {

    /** Saves a password through the system Credential Manager (the user picks the target app). */
    suspend fun save(activity: Activity, username: String, password: String): Result<Unit> =
        try {
            CredentialManager.create(activity)
                .createCredential(activity, CreatePasswordRequest(username, password))
            Result.success(Unit)
        } catch (e: CreateCredentialCancellationException) {
            Result.failure(SaveCancelledException())
        } catch (e: CreateCredentialException) {
            Result.failure(e)
        }
}
