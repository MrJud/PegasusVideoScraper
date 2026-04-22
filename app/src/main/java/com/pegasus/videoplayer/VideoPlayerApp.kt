package com.pegasus.videoplayer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VideoPlayerApp : Application() {
    /**
     * Scope di ricerca che sopravvive alla SearchActivity (che si finish() subito).
     * Vive quanto il processo: se Android uccide il processo in background,
     * il job viene perso e QML timeout → utente riprova. Accettabile.
     */
    val searchScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * Scope di download trailer che sopravvive alla DownloadActivity (headless, finish() subito).
     * Stesso pattern di searchScope.
     */
    val downloadScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
