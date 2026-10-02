package com.watchfaces.btceur

enum class FiatCurrency(
    val code: String,
    val symbol: String,
    val coinbasePair: String,
    val krakenPair: String,
) {
    EUR(
        code = "EUR",
        symbol = "€",
        coinbasePair = "BTC-EUR",
        krakenPair = "XBTEUR",
    ),
    USD(
        code = "USD",
        symbol = "$",
        coinbasePair = "BTC-USD",
        krakenPair = "XBTUSD",
    ),
}
