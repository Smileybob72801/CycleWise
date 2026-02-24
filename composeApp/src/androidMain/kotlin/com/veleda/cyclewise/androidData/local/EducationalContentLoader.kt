package com.veleda.cyclewise.androidData.local

import android.content.Context
import android.util.Log
import com.veleda.cyclewise.domain.models.EducationalArticle
import kotlinx.serialization.json.Json

/**
 * Loads [EducationalArticle] data from the bundled `res/raw/educational_content.json` asset.
 *
 * Uses `kotlinx.serialization` with `ignoreUnknownKeys = true` for forward-compatibility
 * with future schema additions. Returns an empty list on any parse or IO failure so the
 * app degrades gracefully (no educational content) rather than crashing.
 */
object EducationalContentLoader {

    private const val TAG = "EducationalContentLoader"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Reads and parses the educational content JSON from Android resources.
     *
     * @param context Application context for resource access.
     * @return Parsed articles, or an empty list on failure.
     */
    fun load(context: Context): List<EducationalArticle> {
        return try {
            val resId = context.resources.getIdentifier(
                "educational_content", "raw", context.packageName
            )
            if (resId == 0) {
                Log.e(TAG, "educational_content.json resource not found")
                return emptyList()
            }
            val jsonText = context.resources.openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }
            parseJson(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load educational content", e)
            emptyList()
        }
    }

    /**
     * Parses a JSON string into a list of [EducationalArticle].
     *
     * Exposed as `internal` for unit testing without Android context.
     *
     * @param jsonText The raw JSON string to parse.
     * @return Parsed articles, or an empty list on parse failure.
     */
    internal fun parseJson(jsonText: String): List<EducationalArticle> {
        return try {
            json.decodeFromString<List<EducationalArticle>>(jsonText)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Log.e is not available in plain unit tests; use stderr for testability.
            System.err.println("$TAG: Failed to parse educational content JSON: ${e.message}")
            emptyList()
        }
    }
}
