package com.abht.manga_dt.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(onFilePicked: (content: String, fileName: String) -> Unit): () -> Unit
