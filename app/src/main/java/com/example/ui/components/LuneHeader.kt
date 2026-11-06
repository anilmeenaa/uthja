package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LuneHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionPress: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppText(
                text = "Uthja",
                variant = AppTextVariant.LABEL,
                tone = AppTextTone.ACCENT
            )
            AppText(
                text = title,
                variant = AppTextVariant.TITLE_SMALL,
                tone = AppTextTone.DEFAULT
            )
            if (!subtitle.isNullOrBlank()) {
                AppText(
                    text = subtitle,
                    variant = AppTextVariant.BODY_SMALL,
                    tone = AppTextTone.SECONDARY
                )
            }
        }

        if (actionLabel != null && onActionPress != null) {
            LuneButton(
                onClick = onActionPress,
                variant = LuneButtonVariant.SECONDARY,
                size = LuneButtonSize.SM,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                AppText(
                    text = actionLabel,
                    variant = AppTextVariant.BODY_SMALL,
                    tone = AppTextTone.ACCENT
                )
            }
        }
    }
}
