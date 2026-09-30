package com.watchfaces.btceur

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteParserTest {
    @Test
    fun krakenTicker_readsLastPriceAndChangeFromOpen() {
        val body = """
            {
              "error": [],
              "result": {
                "XXBTZEUR": {
                  "c": ["97510.50000", "0.01"],
                  "o": "96000.00000"
                }
              }
            }
        """.trimIndent()

        val quote = QuoteParser.parseKraken(body, fetchedAtEpochMs = 1_000L)

        assertEquals(97510.5, quote.priceEur, 0.001)
        assertEquals(1.5734375, quote.changePercent!!, 0.0001)
        assertEquals(1_000L, quote.fetchedAtEpochMs)
    }

    @Test
    fun coinbaseSpot_readsAmountWithoutChange() {
        val body = """{"data":{"amount":"97000.10","base":"BTC","currency":"EUR"}}"""

        val quote = QuoteParser.parseCoinbase(body, fetchedAtEpochMs = 2_000L)

        assertEquals(97000.10, quote.priceEur, 0.001)
        assertNull(quote.changePercent)
    }
}
