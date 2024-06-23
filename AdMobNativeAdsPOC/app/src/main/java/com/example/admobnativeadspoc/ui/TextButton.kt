package com.example.admobnativeadspoc.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TextButton(
    name: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(onClick = { onClick() }, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Text(name)
    }
}