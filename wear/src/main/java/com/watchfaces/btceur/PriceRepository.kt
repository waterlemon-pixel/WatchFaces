package com.watchfaces.btceur

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PriceRepository(
    context: Context,
    private val currency: FiatCurrency,
) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName(currency), Context.MODE_PRIVATE)

    suspend fun load(force: Boolean): LoadResult = withContext(Dispatchers.IO) {
        lockFor(currency).withLock {
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
        val coinbase = "https://api.coinbase.com/v2/prices/${currency.coinbasePair}/spot"
        val kraken = "https://api.kraken.com/0/public/Ticker?pair=${currency.krakenPair}"
        return runCatching { QuoteParser.parseCoinbase(get(coinbase), now) }.getOrNull()
            ?: runCatching { QuoteParser.parseKraken(get(kraken), now) }.getOrNull()
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
            price = prefs.getString(KEY_PRICE, null)?.toDoubleOrNull() ?: return null,
            changePercent = prefs.getString(KEY_CHANGE, null)?.toDoubleOrNull(),
            fetchedAtEpochMs = prefs.getLong(KEY_FETCHED_AT, 0L),
        )
    }

    private fun write(quote: Quote) {
        prefs.edit()
            .putString(KEY_PRICE, quote.price.toString())
            .putString(KEY_CHANGE, quote.changePercent?.toString())
            .putLong(KEY_FETCHED_AT, quote.fetchedAtEpochMs)
            .apply()
    }

    companion object {
        private val locks = mutableMapOf<FiatCurrency, Mutex>()
        private const val KEY_PRICE = "price"
        private const val KEY_CHANGE = "change"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val FRESH_FOR_MS = 4 * 60 * 1000L
        private const val TIMEOUT_MS = 8_000
        private const val USER_AGENT = "WatchFacesBtc/1.0"

        private fun prefsName(currency: FiatCurrency) = "btc_${currency.code.lowercase()}"

        private fun lockFor(currency: FiatCurrency): Mutex = synchronized(locks) {
            locks.getOrPut(currency) { Mutex() }
        }
    }
}

data class LoadResult(
    val quote: Quote?,
    val networkFailed: Boolean,
)
