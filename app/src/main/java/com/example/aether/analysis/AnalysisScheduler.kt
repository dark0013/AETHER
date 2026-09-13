package com.example.aether.analysis

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.aether.data.db.AetherDatabase
import com.example.aether.data.db.entities.AnalysisStatus
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.util.AetherLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AnalysisOutcome {
    READY, SKIPPED, ERROR, MISSING
}

class AnalysisScheduler private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val workManager = WorkManager.getInstance(applicationContext)
    private val db = AetherDatabase.getDatabase(applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val analysisMutex = Mutex()

    companion object {
        private const val TAG = "AnalysisScheduler"

        @Volatile
        private var INSTANCE: AnalysisScheduler? = null

        fun getInstance(context: Context): AnalysisScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalysisScheduler(context).also { INSTANCE = it }
            }
        }
    }

    fun scheduleBackgroundAnalysis(songId: Long) {
        val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(workDataOf("song_id" to songId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag("analysis_$songId")
            .build()

        workManager.enqueueUniqueWork(
            "analysis_$songId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun boostAnalysis(songId: Long) {
        scope.launch {
            workManager.cancelUniqueWork("analysis_$songId")
            runAnalysis(songId)
        }
    }

    /**
     * Single in-flight analysis. File gone or corrupt → ERROR profile, track still playable.
     */
    suspend fun runAnalysis(songId: Long): AnalysisOutcome = analysisMutex.withLock {
        val songDao = db.songDao()
        val profileDao = db.profileDao()
        val tapeDao = db.densityTapeDao()

        val songEntity = songDao.getSongById(songId) ?: return@withLock AnalysisOutcome.MISSING
        val existing = profileDao.getProfileForSong(songId)
        if (existing?.status == AnalysisStatus.READY) return@withLock AnalysisOutcome.SKIPPED

        if (!isUriReadable(songEntity.contentUri)) {
            persistError(songId, "archivo ilegible")
            return@withLock AnalysisOutcome.ERROR
        }

        AetherLog.d(TAG, "Analyzing song $songId")
        return@withLock try {
            val analyzer = AudioAnalyzer(applicationContext)
            val result = analyzer.analyze(songId, Uri.parse(songEntity.contentUri))
            if (result == null) {
                persistError(songId, "análisis vacío")
                AnalysisOutcome.ERROR
            } else {
                profileDao.upsertProfile(result.profile)
                tapeDao.upsertTape(
                    DensityTapeEntity(
                        songId = songId,
                        frameCount = result.energyTape.size,
                        hopMs = 93,
                        energyTape = result.energyTape,
                        centroidTape = result.centroidTape,
                        fluxTape = result.fluxTape,
                        onsetFlags = result.onsetFlags
                    )
                )
                AetherLog.d(TAG, "Analysis ready for $songId")
                AnalysisOutcome.READY
            }
        } catch (e: AnalysisException) {
            AetherLog.e(TAG, "Analysis failed for $songId: ${e.message}", e)
            persistError(songId, e.message ?: "error de análisis")
            AnalysisOutcome.ERROR
        } catch (e: Exception) {
            AetherLog.e(TAG, "Analysis failed for $songId", e)
            persistError(songId, e.message ?: "error de análisis")
            AnalysisOutcome.ERROR
        }
    }

    private suspend fun persistError(songId: Long, message: String) {
        db.profileDao().upsertProfile(
            ProfileEntity(
                songId = songId,
                status = AnalysisStatus.ERROR,
                error = message.take(80)
            )
        )
    }

    private fun isUriReadable(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
