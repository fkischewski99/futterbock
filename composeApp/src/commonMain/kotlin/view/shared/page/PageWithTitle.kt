package view.shared.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MovableContent
import androidx.compose.ui.Modifier

@Composable
fun ColumnWithPadding(content: @Composable () -> Unit) {
    Scaffold {
        Column(modifier = Modifier.padding(it.calculateTopPadding())) {
            content()
        }
    }
}