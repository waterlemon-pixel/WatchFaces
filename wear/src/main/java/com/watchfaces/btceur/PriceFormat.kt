package com.watchfaces.btceur

import java.text.NumberFormat
import java.util.Locale

object PriceFormat {
    private val amountFormat = NumberFormat.getIntegerInstance(Locale.GERMANY)
    private val changeFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    fun amount(value: Double): String = amountFormat.format(value)

    fun withSymbol(value: Double, currency: FiatCurrency): String = "${amount(value)} ${currency.symbol}"

    fun change(percent: Double?): String {
        percent ?: return ""
        val number = changeFormat.format(percent)
        return if (percent > 0) "+$number %" else "$number %"
    }
}
