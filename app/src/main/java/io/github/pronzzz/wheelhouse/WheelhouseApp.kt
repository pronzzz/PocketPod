package io.github.pronzzz.wheelhouse

import android.app.Application
import io.github.pronzzz.wheelhouse.data.ArtworkLoader
import io.github.pronzzz.wheelhouse.data.Library
import io.github.pronzzz.wheelhouse.data.Prefs
import io.github.pronzzz.wheelhouse.data.db.WheelhouseDatabase
import io.github.pronzzz.wheelhouse.launcher.AppsRepository
import io.github.pronzzz.wheelhouse.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app-wide singletons. Kept deliberately simple: no DI framework. */
class WheelhouseApp : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs by lazy { Prefs(this) }
    val database by lazy { WheelhouseDatabase.create(this) }
    val library by lazy { Library(this, database.dao(), scope) }
    val artwork by lazy { ArtworkLoader(this) }
    val player by lazy { PlayerConnection(this, library, prefs, scope) }
    val apps by lazy { AppsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WheelhouseApp
            private set
    }
}
