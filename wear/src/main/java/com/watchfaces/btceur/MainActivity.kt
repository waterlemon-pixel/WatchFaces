package com.watchfaces.btceur

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = PriceRepository(applicationContext)
        val updateRequester = ComplicationDataSourceUpdateRequester.create(
            this,
            ComponentName(this, BtcEurComplicationService::class.java),
        )
        setContent {
            var state by remember { mutableStateOf(PriceUiState(loading = true)) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                state = repository.load(force = false).toUiState()
            }
            MaterialTheme {
                PriceScreen(
                    state = state,
                    onRefresh = {
                        if (state.loading) return@PriceScreen
                        state = state.copy(loading = true)
                        scope.launch {
                            val result = repository.load(force = true)
                            updateRequester.requestUpdateAll()
                            state = result.toUiState()
                        }
                    },
                )
            }
        }
    }
}

private data class PriceUiState(
    val priceText: String = "–",
    val changeText: String = "",
    val changePositive: Boolean? = null,
    val statusText: String = "",
    val loading: Boolean = false,
)

private fun LoadResult.toUiState(): PriceUiState {
    val time = quote?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMANY).format(Date(it.fetchedAtEpochMs))
    }
    val status = when {
        quote == null -> "Kein Kurs"
        networkFailed && time != null -> "Zuletzt $time"
        time != null -> "Stand $time"
        else -> ""
    }
    return PriceUiState(
        priceText = quote?.let { PriceFormat.eurosWithSymbol(it.priceEur) } ?: "–",
        changeText = quote?.changePercent?.let { "heute ${PriceFormat.change(it)}" }.orEmpty(),
        changePositive = quote?.changePercent?.let { it >= 0 },
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
            item {
                Text(
                    text = "BTC/EUR",
                    style = MaterialTheme.typography.title3,
                )
            }
            item {
                Text(
                    text = state.priceText,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.display3,
                )
            }
            item {
                val color = when (state.changePositive) {
                    true -> Color(0xFF3DDC97)
                    false -> Color(0xFFFF6B6B)
                    null -> MaterialTheme.colors.onSurface
                }
                Text(text = state.changeText, color = color)
            }
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
