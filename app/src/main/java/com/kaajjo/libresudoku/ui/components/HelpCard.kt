package com.kaajjo.libresudoku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

@Composable
fun HelpCard(
    modifier: Modifier = Modifier,
    title: String,
    details: String,
    painter: Painter?,
    onCloseClicked: () -> Unit
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (painter != null) {
                        Icon(
                            painter = painter,
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onCloseClicked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_round_close_24),
                        contentDescription = null
                    )
                }
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@LightDarkPreview
@Composable
fun HelpCardPreview() {
    LibreSudokuTheme {
        HelpCard(
            title = "This is the title",
            details = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam tempus arcu vitae elit congue scelerisque. Sed a vestibulum tellus. Suspendisse tristique dui eget nisi dictum tempus",
            painter = painterResource(R.drawable.ic_outline_verified_24),
            onCloseClicked = {}
        )
    }
}

@Composable
fun GrantPermissionCard(
    modifier: Modifier = Modifier,
    title: String,
    details: String,
    painter: Painter?,
    confirmButton: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (painter != null) {
                        Icon(
                            painter = painter,
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            confirmButton()
        }
    }
}

@LightDarkPreview
@Composable
fun GrantPermissionCardPreview() {
    LibreSudokuTheme {
        GrantPermissionCard(
            title = "This is the title",
            details = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam tempus arcu vitae elit congue scelerisque. Sed a vestibulum tellus. Suspendisse tristique dui eget nisi dictum tempus",
            painter = rememberVectorPainter(image = Icons.Rounded.TipsAndUpdates),
            confirmButton = {
                Button(
                    onClick = { },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Grant")
                }
            }
        )
    }
}

