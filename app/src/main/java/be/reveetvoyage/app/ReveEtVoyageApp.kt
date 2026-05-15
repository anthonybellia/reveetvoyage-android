package be.reveetvoyage.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class ReveEtVoyageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@ReveEtVoyageApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }
}
