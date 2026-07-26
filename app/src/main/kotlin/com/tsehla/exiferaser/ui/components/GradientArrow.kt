package com.tsehla.exiferaser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun GradientArrow(
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
) {
    val arrowColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val lineHeight = size.height / 2f
            val arrowHeadSize = 10.dp.toPx()
            val lineEnd = size.width - arrowHeadSize

            drawLine(
                color = arrowColor,
                start = Offset(0f, lineHeight),
                end = Offset(lineEnd, lineHeight),
                strokeWidth = 2.dp.toPx(),
            )

            val arrowPath = Path().apply {
                moveTo(size.width, lineHeight)
                lineTo(lineEnd, lineHeight - arrowHeadSize / 1.5f)
                lineTo(lineEnd, lineHeight + arrowHeadSize / 1.5f)
                close()
            }
            drawPath(
                path = arrowPath,
                color = arrowColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
