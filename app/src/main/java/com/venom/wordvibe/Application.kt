package com.venom.wordvibe

import android.app.Application
import android.os.StrictMode
import com.venom.data.corpus.CorpusSeeder
import com.venom.data.corpus.CorpusSeederEntryPoint
import com.venom.data.corpus.DataStoreSeedFlag
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        seedCorpusOnce()
    }

    /**
     * First-launch pilot corpus seed: idempotent, offline, fire-and-forget.
     * Failures only log — a failed seed must never crash launch.
     */
    private fun seedCorpusOnce() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entry = EntryPoints.get(
                    this@Application,
                    CorpusSeederEntryPoint::class.java
                )
                val json = assets.open("words_pilot.json").bufferedReader().use { it.readText() }
                val inserted = CorpusSeeder(
                    entry.wordRepository(),
                    DataStoreSeedFlag(entry.dataStore())
                ).seedIfNeeded(json)
                println("Corpus seed complete, inserted=$inserted")
            } catch (e: Exception) {
                println("Corpus seed failed: ${e.message}")
            }
        }
    }

    private fun enableStrictMode() {
        println("StrictMode Enabled")
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    companion object {
        private const val TAG = "WordVibeApp"
    }
}
