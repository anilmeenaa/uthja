package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LuneColors

@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "Uthja",
    subtitle: String? = null,
    showNotification: Boolean = false,
    onNotificationClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppText(
                text = eyebrow,
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

        if (showNotification) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LuneColors.surface)
                    .border(1.dp, LuneColors.border, CircleShape)
                    .clickable { onNotificationClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = LuneColors.text,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
