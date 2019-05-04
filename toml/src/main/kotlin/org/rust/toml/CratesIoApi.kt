/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.util.io.HttpRequests
import org.jetbrains.annotations.TestOnly
import org.rust.ide.notifications.showBalloon
import org.rust.openapiext.isUnitTestMode
import org.rust.openapiext.runWithCheckCanceled
import org.toml.lang.psi.TomlKey
import java.io.IOException

data class SearchResult(val crates: List<CrateDescription>)

data class CrateDescription(
    val name: String,
    @SerializedName("max_version")
    val maxVersion: String
)

val CrateDescription.dependencyLine: String
    get() = "$name = \"$maxVersion\""

fun searchCrate(key: TomlKey): Collection<CrateDescription> {
    if (isUnitTestMode) return MOCK!!

    // Stuff like [dependencies.'crate'] can contain single quotes which we need to remove or otherwise the crates API
    // request would not work
    val name = CompletionUtil.getOriginalElement(key)?.text?.removeSurrounding("'") ?: ""
    if (name.isEmpty()) return emptyList()

    val response = requestCratesIo<SearchResult>(key, "crates?page=1&per_page=20&q=$name&sort=") ?: return emptyList()
    return response.crates
}

data class CrateFullDescription(
    val name: String,
    @SerializedName("max_version")
    val maxVersion: String,
    val versions: List<CrateVersionDescription>
)

data class CrateVersionDescription(val id: Int, val num: String, val yanked: Boolean)

private class CratesIoApiResponse(val crate: CrateDescription, val versions: List<CrateVersionDescription>)

fun getCrateFullDescription(crateName: TomlKey): CrateFullDescription? =
// Stuff like [dependencies.'crate'] can contain single quotes which we need to remove or otherwise the crates API
    // request would not work
    getCrateFullDescription(crateName, crateName.text.removeSurrounding("'"))

fun getCrateFullDescription(context: PsiElement, name: String): CrateFullDescription? {
    if (isUnitTestMode) return MOCK2

    val response = requestCratesIo<CratesIoApiResponse>(context, "crates/$name") ?: return null
    return CrateFullDescription(response.crate.name, response.crate.maxVersion, response.versions)
}

private inline fun <reified T> requestCratesIo(context: PsiElement, path: String): T? {
    return requestCratesIo(context, path, T::class.java)
}

private fun <T> requestCratesIo(context: PsiElement, path: String, cls: Class<T>): T? {
    return try {
        runWithCheckCanceled {
            val response = HttpRequests.request("https://crates.io/api/v1/$path")
                .readString(ProgressManager.getInstance().progressIndicator)
            Gson().fromJson(response, cls)
        }
    } catch (e: IOException) {
        context.project.showBalloon("Could not reach crates.io", NotificationType.WARNING)
        null
    } catch (e: JsonSyntaxException) {
        context.project.showBalloon("Bad answer from crates.io", NotificationType.WARNING)
        null
    }
}

private var MOCK: List<CrateDescription>? = null
private var MOCK2: CrateFullDescription? = null

@TestOnly
fun withMockedFullCrateDescription(mock: CrateFullDescription, action: () -> Unit) {
    MOCK2 = mock
    try {
        action()
    } finally {
        MOCK2 = null
    }
}

@TestOnly
fun withMockedCrateSearch(mock: List<CrateDescription>, action: () -> Unit) {
    MOCK = mock
    try {
        action()
    } finally {
        MOCK = null
    }
}
