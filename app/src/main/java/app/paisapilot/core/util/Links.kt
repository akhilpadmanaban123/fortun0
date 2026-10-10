package app.paisapilot.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Public web pages for the app, hosted on Firebase Hosting from the `public/` folder. */
object Links {
    const val PRIVACY_POLICY = "https://app-paisapilot.web.app/privacy.html"
    const val DELETE_ACCOUNT = "https://app-paisapilot.web.app/delete-account.html"

    /** Opens [url] in the browser; does nothing on the rare device without one. */
    fun open(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
