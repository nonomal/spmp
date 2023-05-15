package com.spectre7.utils.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidthShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
	var text_style by remember(style) { mutableStateOf(style) }
	var text_style_large: TextStyle? by remember(style) { mutableStateOf(null) }
	var ready_to_draw by remember { mutableStateOf(false) }

	val delta = 0.05

    Box {
        Text(
            text,
            modifier.drawWithContent { if (ready_to_draw) drawContent() },
            maxLines = 1,
            softWrap = false,
            style = text_style,
            onTextLayout = { layout_result ->
                if (!layout_result.didOverflowWidth) {
                    ready_to_draw = true
                    text_style_large = text_style
                } else {
                    text_style = text_style.copy(fontSize = text_style.fontSize * (1.0 - delta))
                }
            }
        )

        text_style_large?.also {
            Text(
                text,
                modifier.drawWithContent {}.requiredHeight(1.dp),
                maxLines = 1,
                softWrap = false,
                style = it,
                onTextLayout = { layout_result ->
                    if (!layout_result.didOverflowWidth) {
                        text_style_large = it.copy(fontSize = minOf(style.fontSize.value, it.fontSize.value * (1.0f + delta.toFloat())).sp)
                        text_style = it
                    }
                }
            )
        }
    }
}

@Composable
fun WidthShrinkText(text: String, fontSize: TextUnit, modifier: Modifier = Modifier, fontWeight: FontWeight? = null, colour: Color = LocalContentColor.current) {
    WidthShrinkText(
        text,
        modifier,
        LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = fontWeight, color = colour)
    )
}