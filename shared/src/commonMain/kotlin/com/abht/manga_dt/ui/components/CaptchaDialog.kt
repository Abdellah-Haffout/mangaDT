package com.abht.manga_dt.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun CaptchaWebViewDialog(
    url: String,
    title: String = "Verification",
    onDismiss: () -> Unit,
    onSolved: () -> Unit
)
