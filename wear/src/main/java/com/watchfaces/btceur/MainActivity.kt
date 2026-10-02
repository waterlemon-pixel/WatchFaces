package com.watchfaces.btceur

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eurRepository = PriceRepository(applicationContext, FiatCurrency.EUR)
        val usdRepository = PriceRepository(applicationContext, FiatCurrency.USD)
        val updateRequesters = listOf(BtcEurComplicationService::class.java, BtcUsdComplicationService::class.java)
            .map { service ->
                ComplicationDataSourceUpdateRequester.create(this, ComponentName(this, service))
            }
        setContent {
            var state by remember { mutableStateOf(PriceUiState(loading = true)) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                state = loadBoth(eurRepository, usdRepository, force = false)
            }
            MaterialTheme {
                PriceScreen(
                    state = state,
                    onRefresh = {
                        if (state.loading) return@PriceScreen
                        state = state.copy(loading = true)
                        scope.launch {
                            state = loadBoth(eurRepository, usdRepository, force = true)
                            updateRequesters.forEach { it.requestUpdateAll() }
                        }
                    },
                )
            }
        }
    }
}

private data class PriceUiState(
    val eurText: String = "–",
    val usdText: String = "–",
    val statusText: String = "",
    val loading: Boolean = false,
)

private suspend fun loadBoth(
    eurRepository: PriceRepository,
    usdRepository: PriceRepository,
    force: Boolean,
): PriceUiState = coroutineScope {
    val eur = async { eurRepository.load(force) }
    val usd = async { usdRepository.load(force) }
    toUiState(eur.await(), usd.await())
}

private fun toUiState(eur: LoadResult, usd: LoadResult): PriceUiState {
    val quotes = listOfNotNull(eur.quote, usd.quote)
    val time = quotes.maxOfOrNull { it.fetchedAtEpochMs }?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMANY).format(Date(it))
    }
    val failed = eur.networkFailed || usd.networkFailed
    val status = when {
        quotes.isEmpty() -> "Kein Kurs"
        failed && time != null -> "Zuletzt $time"
        time != null -> "Stand $time"
        else -> ""
    }
    return PriceUiState(
        eurText = eur.quote?.let { PriceFormat.withSymbol(it.price, FiatCurrency.EUR) } ?: "–",
        usdText = usd.quote?.let { PriceFormat.withSymbol(it.price, FiatCurrency.USD) } ?: "–",
        statusText = status,
        loading = false,
    )
}

@Composable
private fun PriceScreen(
    state: PriceUiState,
    onRefresh: () -> Unit,
) {
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { PairPrice(label = "BTC/EUR", price = state.eurText) }
            item { PairPrice(label = "BTC/USD", price = state.usdText) }
            item {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.caption2,
                )
            }
            item {
                Chip(
                    onClick = onRefresh,
                    enabled = !state.loading,
                    label = { Text(if (state.loading) "…" else "Aktualisieren") },
                )
            }
        }
    }
}

@Composable
private fun PairPrice(label: String, price: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.caption2)
        Text(
            text = price,
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.title2,
        )
    }
}
