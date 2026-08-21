package com.abht.manga_dt.ui.components

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePicker(onFilePicked: (content: String, fileName: String) -> Unit): () -> Unit {
    return {
        try {
            val dialog = FileDialog(null as Frame?, "Select Backup File", FileDialog.LOAD)
            dialog.isVisible = true
            val file = dialog.file
            val dir = dialog.directory
            if (file != null && dir != null) {
                val fullFile = File(dir, file)
                if (fullFile.exists() && fullFile.isFile) {
                    val content = fullFile.readText(Charsets.UTF_8)
                    onFilePicked(content, fullFile.name)
                }
            }
        } catch (_: Exception) {
        }
    }
}
