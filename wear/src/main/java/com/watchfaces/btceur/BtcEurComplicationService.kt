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

class BtcEurComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = PriceRepository(this).load(force = false).quote
            ?: return NoDataComplicationData()
        return complicationData(request.complicationType, quote)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return complicationData(
            type = type,
            quote = Quote(priceEur = 97_500.0, changePercent = 1.2, fetchedAtEpochMs = 0L),
        )
    }

    private fun complicationData(type: ComplicationType, quote: Quote): ComplicationData? {
        val price = PriceFormat.euros(quote.priceEur)
        val priceWithSymbol = PriceFormat.eurosWithSymbol(quote.priceEur)
        val change = PriceFormat.change(quote.changePercent)
        val changeTitle = if (change.isBlank()) getString(R.string.pair_label) else "heute $change"
        val description = PlainComplicationText.Builder(
            text = getString(R.string.price_content_description, price),
        ).build()
        val icon = MonochromaticImage.Builder(
            image = Icon.createWithResource(this, R.drawable.ic_btc),
        ).build()
        val tapAction = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(price).build(),
                contentDescription = description,
            )
                .setTitle(PlainComplicationText.Builder(getString(R.string.btc)).build())
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
