package com.watchfaces.btceur

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PriceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun load(force: Boolean): LoadResult = withContext(Dispatchers.IO) {
        refreshLock.withLock {
            val cached = read()
            val fresh = cached != null &&
                System.currentTimeMillis() - cached.fetchedAtEpochMs < FRESH_FOR_MS
            if (!force && fresh) {
                return@withLock LoadResult(quote = cached, networkFailed = false)
            }
            val fetched = fetch()
            if (fetched != null) {
                write(fetched)
                LoadResult(quote = fetched, networkFailed = false)
            } else {
                LoadResult(quote = cached, networkFailed = true)
            }
        }
    }

    private fun fetch(): Quote? {
        val now = System.currentTimeMillis()
        return runCatching { QuoteParser.parseCoinbase(get(COINBASE_URL), now) }.getOrNull()
            ?: runCatching { QuoteParser.parseKraken(get(KRAKEN_URL), now) }.getOrNull()
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun read(): Quote? {
        if (!prefs.contains(KEY_PRICE)) return null
        return Quote(
            priceEur = prefs.getString(KEY_PRICE, null)?.toDoubleOrNull() ?: return null,
            changePercent = prefs.getString(KEY_CHANGE, null)?.toDoubleOrNull(),
            fetchedAtEpochMs = prefs.getLong(KEY_FETCHED_AT, 0L),
        )
    }

    private fun write(quote: Quote) {
        prefs.edit()
            .putString(KEY_PRICE, quote.priceEur.toString())
            .putString(KEY_CHANGE, quote.changePercent?.toString())
            .putLong(KEY_FETCHED_AT, quote.fetchedAtEpochMs)
            .apply()
    }

    companion object {
        private val refreshLock = Mutex()
        private const val PREFS = "btc_eur"
        private const val KEY_PRICE = "price"
        private const val KEY_CHANGE = "change"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val FRESH_FOR_MS = 4 * 60 * 1000L
        private const val TIMEOUT_MS = 8_000
        private const val USER_AGENT = "WatchFacesBtcEur/1.0"
        private const val KRAKEN_URL = "https://api.kraken.com/0/public/Ticker?pair=XBTEUR"
        private const val COINBASE_URL = "https://api.coinbase.com/v2/prices/BTC-EUR/spot"
    }
}

data class LoadResult(
    val quote: Quote?,
    val networkFailed: Boolean,
)
