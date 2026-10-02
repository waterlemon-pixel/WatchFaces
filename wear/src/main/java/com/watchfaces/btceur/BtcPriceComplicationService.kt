package com.watchfaces.btceur

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

abstract class BtcPriceComplicationService : SuspendingComplicationDataSourceService() {
    protected abstract val currency: FiatCurrency
    protected abstract val previewPrice: Double

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = PriceRepository(this, currency).load(force = false).quote
            ?: return NoDataComplicationData()
        return complicationData(request.complicationType, quote)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return complicationData(
            type = type,
            quote = Quote(price = previewPrice, changePercent = 1.2, fetchedAtEpochMs = 0L),
        )
    }

    private fun complicationData(type: ComplicationType, quote: Quote): ComplicationData? {
        val price = PriceFormat.amount(quote.price)
        val priceWithSymbol = PriceFormat.withSymbol(quote.price, currency)
        val change = PriceFormat.change(quote.changePercent)
        val changeTitle = if (change.isBlank()) currency.code else "heute $change"
        val description = PlainComplicationText.Builder(
            text = getString(R.string.price_content_description, price, currency.code),
        ).build()
        val icon = MonochromaticImage.Builder(
            image = Icon.createWithResource(this, R.drawable.ic_btc),
        ).build()
        val tapAction = PendingIntent.getActivity(
            this,
            currency.ordinal,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(price).build(),
                contentDescription = description,
            )
                .setTitle(PlainComplicationText.Builder(currency.code).build())
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(priceWithSymbol).build(),
                contentDescription = description,
            )
                .setTitle(PlainComplicationText.Builder(changeTitle).build())
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            else -> null
        }
    }
}

class BtcEurComplicationService : BtcPriceComplicationService() {
    override val currency = FiatCurrency.EUR
    override val previewPrice = 97_500.0
}

class BtcUsdComplicationService : BtcPriceComplicationService() {
    override val currency = FiatCurrency.USD
    override val previewPrice = 105_000.0
}
