package io.github.personan1.setloop.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/** Links of the About group (SPEC D20): the project's GitHub Pages and the Telegram support chat. */
object SupportLinks {
    const val TELEGRAM = "https://t.me/setnewloop"
    const val PRIVACY = "https://personan1.github.io/SetLoop/privacy/"
    const val SUPPORT = "https://personan1.github.io/SetLoop/support/"
}

/** Name and code of the installed build, read from the package so there is a single source: the Gradle file. */
data class AppVersion(val name: String, val code: Long) {
    companion object {
        fun of(context: Context): AppVersion {
            val packages = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packages.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packages.getPackageInfo(context.packageName, 0)
            }
            return AppVersion(info.versionName.orEmpty(), PackageInfoCompat.getLongVersionCode(info))
        }
    }
}

/** Asset with the license of the bundled font. */
const val NUNITO_LICENSE_ASSET = "licenses/Nunito-OFL.txt"
