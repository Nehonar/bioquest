package com.bioquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bioquest.ui.theme.AlertRed
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorDim
import com.bioquest.ui.theme.PhosphorGreen

/** A bordered terminal-style panel with an optional header line. */
@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PhosphorDim, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(12.dp),
    ) {
        if (title != null) {
            Text(
                text = "// $title",
                style = MaterialTheme.typography.labelLarge,
                color = PhosphorGreen,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        content()
    }
}

/**
 * Thick ASCII-inspired stat bar: LABEL [#####-----] value.
 * Colour shifts green -> amber -> red as [invert] stats (like corruption) rise.
 */
@Composable
fun StatBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    invert: Boolean = false,
    segments: Int = 10,
) {
    val filled = (value.coerceIn(0, 100) * segments) / 100
    val barColor = statColor(value, invert)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.padEnd(11).take(11),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .weight(1f)
                .height(14.dp)
                .background(Color(0xFF07110C), RoundedCornerShape(2.dp))
                .border(1.dp, PhosphorDim, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (segments == 0) 0f else filled.toFloat() / segments)
                    .height(14.dp)
                    .background(barColor, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = value.toString().padStart(3),
            style = MaterialTheme.typography.labelLarge,
            color = barColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = PhosphorGreen,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

fun statColor(value: Int, invert: Boolean): Color {
    val v = if (invert) 100 - value else value
    return when {
        v >= 60 -> PhosphorGreen
        v >= 35 -> Amber
        else -> AlertRed
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
