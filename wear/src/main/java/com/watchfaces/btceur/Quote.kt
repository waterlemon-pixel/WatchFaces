package com.watchfaces.btceur

data class Quote(
    val priceEur: Double,
    val changePercent: Double?,
    val fetchedAtEpochMs: Long,
)
