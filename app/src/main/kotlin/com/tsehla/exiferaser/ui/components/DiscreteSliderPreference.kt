package com.tsehla.exiferaser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.tsehla.exiferaser.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun DiscreteSliderPreference(
    position: Int,
    positionLabels: List<String>,
    dynamicLabel: String,
    gradientStartLabel: String,
    gradientEndLabel: String,
    gradientColors: List<Color>,
    onPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPositionLabels: Boolean = true,
    isDescriptionExpanded: Boolean = false,
    onDescriptionToggle: (() -> Unit)? = null,
    descriptionContent: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = dynamicLabel,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = position.toFloat(),
            onValueChange = { onPositionChange(it.toInt()) },
            valueRange = 0f..(positionLabels.size - 1).toFloat(),
            steps = positionLabels.size - 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showPositionLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                positionLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        GradientArrow(
            startLabel = gradientStartLabel,
            endLabel = gradientEndLabel,
            colors = gradientColors,
        )
        if (descriptionContent != null) {
            if (onDescriptionToggle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDescriptionToggle() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(if (isDescriptionExpanded) {
                            R.drawable.baseline_keyboard_arrow_up_24
                        } else {
                            R.drawable.baseline_keyboard_arrow_down_24
                        }),
                        contentDescription = if (isDescriptionExpanded) {
                            "Collapse description"
                        } else {
                            "Expand description"
                        },
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Collapse" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(
                    visible = isDescriptionExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    descriptionContent()
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                descriptionContent()
            }
        }
    }
}

@Composable
private fun GradientArrow(
    startLabel: String,
    endLabel: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val lineHeight = size.height / 2f
            val arrowHeadSize = 10.dp.toPx()
            val lineEnd = size.width - arrowHeadSize

            val brush = Brush.linearGradient(
                colors = colors,
                start = Offset(0f, lineHeight),
                end = Offset(size.width, lineHeight),
            )

            drawLine(
                brush = brush,
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
                brush = brush,
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
