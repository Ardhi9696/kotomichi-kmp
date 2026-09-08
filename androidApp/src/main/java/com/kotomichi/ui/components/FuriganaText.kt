package com.kotomichi.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.turtlekazu.furiganable.compose.m3.TextWithReading

/**
 * Merender teks Jepang dengan furigana (mono/group ruby) bila `text` memakai
 * format Furiganable (`[kanji[reading]]`), dan fallback ke [Text] biasa bila tidak.
 */
@Composable
fun FuriganaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (text.contains('[') && text.contains(']')) {
        TextWithReading(
            formattedText = text,
            modifier = modifier,
            color = color,
            fontWeight = fontWeight,
            style = style,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontWeight = fontWeight,
            style = style,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}