package com.watchfaces.btceur

data class Quote(
    val price: Double,
    val changePercent: Double?,
    val fetchedAtEpochMs: Long,
)
