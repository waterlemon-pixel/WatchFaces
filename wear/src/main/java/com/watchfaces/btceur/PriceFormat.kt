package com.watchfaces.btceur

import java.text.NumberFormat
import java.util.Locale

object PriceFormat {
    private val eurosFormat = NumberFormat.getIntegerInstance(Locale.GERMANY)
    private val changeFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    fun euros(value: Double): String = eurosFormat.format(value)

    fun eurosWithSymbol(value: Double): String = "${euros(value)} €"

    fun change(percent: Double?): String {
        percent ?: return ""
        val number = changeFormat.format(percent)
        return if (percent > 0) "+$number %" else "$number %"
    }
}
