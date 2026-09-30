package com.watchfaces.btceur

import org.json.JSONObject

object QuoteParser {
    fun parseKraken(body: String, fetchedAtEpochMs: Long): Quote {
        val root = JSONObject(body)
        val errors = root.optJSONArray("error")
        if (errors != null && errors.length() > 0) {
            throw IllegalStateException(errors.getString(0))
        }
        val result = root.getJSONObject("result")
        val pairName = result.keys().asSequence().firstOrNull()
            ?: throw IllegalStateException("Kraken response has no pair")
        val ticker = result.getJSONObject(pairName)
        val last = ticker.getJSONArray("c").getString(0).toDouble()
        val open = ticker.optString("o").toDoubleOrNull()
        return Quote(
            priceEur = last,
            changePercent = percentChange(last, open),
            fetchedAtEpochMs = fetchedAtEpochMs,
        )
    }

    fun parseCoinbase(body: String, fetchedAtEpochMs: Long): Quote {
        val amount = JSONObject(body).getJSONObject("data").getString("amount").toDouble()
        return Quote(
            priceEur = amount,
            changePercent = null,
            fetchedAtEpochMs = fetchedAtEpochMs,
        )
    }

    private fun percentChange(last: Double, open: Double?): Double? {
        if (open == null || open == 0.0) return null
        return (last - open) / open * 100.0
    }
}
