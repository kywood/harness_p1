package com.example.torrentstreamplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TorrentStreamPlayerTheme {
                MagnetLinkInputScreen()
            }
        }
    }
}

@Composable
fun MagnetLinkInputScreen() {
    var magnetLink by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Torrent Stream Player",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = magnetLink,
            onValueChange = { magnetLink = it },
            label = { Text("Magnet Link") },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (magnetLink.trim().isEmpty()) {
                    errorMessage = "Magnet link cannot be empty"
                } else {
                    errorMessage = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MagnetLinkInputScreenPreview() {
    TorrentStreamPlayerTheme {
        MagnetLinkInputScreen()
    }
}
