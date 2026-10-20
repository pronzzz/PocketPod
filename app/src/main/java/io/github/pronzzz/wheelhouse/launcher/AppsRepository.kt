package io.github.pronzzz.wheelhouse.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import io.github.pronzzz.wheelhouse.util.Text

data class LaunchableApp(val label: String, val component: ComponentName)

/** Lists the other apps on the phone so the Extras › Apps menu can open them. */
class AppsRepository(private val context: Context) {

    private val icons = LruCache<ComponentName, Bitmap>(80)

    fun load(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(pm).toString(), ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
            .sortedBy { Text.sortKey(it.label) }
    }

    fun launch(app: LaunchableApp): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
        true
    } catch (_: Exception) {
        false
    }

    /** Small icon bitmap, loaded synchronously but cached (icons are cheap to load and few). */
    fun icon(app: LaunchableApp, px: Int): Bitmap? {
        icons.get(app.component)?.let { return it }
        val d: Drawable = try {
            context.packageManager.getActivityIcon(app.component)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val bmp = if (d is BitmapDrawable && d.bitmap != null) {
            Bitmap.createScaledBitmap(d.bitmap, px, px, true)
        } else {
            Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also {
                val c = Canvas(it)
                d.setBounds(0, 0, px, px)
                d.draw(c)
            }
        }
        icons.put(app.component, bmp)
        return bmp
    }
}
