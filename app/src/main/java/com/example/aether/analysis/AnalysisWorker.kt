package com.example.aether.analysis

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aether.util.AetherLog

class AnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val songId = inputData.getLong("song_id", -1L)
        if (songId == -1L) return Result.failure()

        return when (AnalysisScheduler.getInstance(applicationContext).runAnalysis(songId)) {
            AnalysisOutcome.READY, AnalysisOutcome.SKIPPED -> Result.success()
            AnalysisOutcome.MISSING, AnalysisOutcome.ERROR -> {
                AetherLog.e(TAG, "Unrecoverable analysis for $songId")
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "AnalysisWorker"
    }
}
