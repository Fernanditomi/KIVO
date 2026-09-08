package com.example.kivo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kivo.ui.theme.KivoBlack
import com.example.kivo.ui.theme.KivoPurple
import com.example.kivo.ui.viewmodels.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var numBands by remember { mutableIntStateOf(0) }
    val bands = remember { mutableStateListOf<String>() }
    val gains = remember { mutableStateListOf<Float>() }
    var minLevel by remember { mutableIntStateOf(-1500) }
    var maxLevel by remember { mutableIntStateOf(1500) }
    var bassStrength by remember { mutableFloatStateOf(0f) }
    var virtStrength by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val data = viewModel.getEqualizerData()
        if (data != null) {
            numBands = data.getInt("numBands")
            minLevel = data.getInt("minLevel")
            maxLevel = data.getInt("maxLevel")
            
            val levels = data.getShortArray("bandLevels")
            bands.clear()
            gains.clear()
            for (i in 0 until numBands) {
                bands.add("Banda ${i + 1}")
                val currentLevel = levels?.get(i)?.toFloat() ?: 0f
                // Map currentLevel (min..max) to -1..1
                val normalized = if (maxLevel > minLevel) {
                    2f * (currentLevel - minLevel) / (maxLevel - minLevel) - 1f
                } else 0f
                gains.add(normalized)
            }
            
            bassStrength = data.getInt("bassStrength") / 1000f
            virtStrength = data.getInt("virtStrength") / 1000f
        } else {
            // Fallback
            numBands = 5
            for (i in 0 until 5) {
                bands.add(listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")[i])
                gains.add(0f)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ECUALIZADOR", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KivoBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = KivoBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Ajusta las frecuencias para personalizar tu sonido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            itemsIndexed(gains) { index, gain ->
                if (index < bands.size) {
                    BandSlider(
                        label = bands[index],
                        value = gain,
                        onValueChange = { newValue ->
                            gains[index] = newValue
                            val gainInt = (minLevel + (newValue + 1f) / 2f * (maxLevel - minLevel)).toInt()
                            viewModel.setEqualizerGain(index, gainInt)
                        }
                    )
                }
            }

            item {
                Text(text = "EFECTOS EXTRA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }

            item {
                BandSlider(
                    label = "BASS BOOST",
                    value = bassStrength,
                    onValueChange = {
                        bassStrength = it
                        viewModel.setBassBoost((it * 1000).toInt())
                    }
                )
            }

            item {
                BandSlider(
                    label = "VIRTUALIZADOR",
                    value = virtStrength,
                    onValueChange = {
                        virtStrength = it
                        viewModel.setVirtualizer((it * 1000).toInt())
                    }
                )
            }
            
            item {
                Button(
                    onClick = { 
                        gains.forEachIndexed { i, _ -> 
                            gains[i] = 0f
                            viewModel.setEqualizerGain(i, 0) 
                        }
                        bassStrength = 0f
                        virtStrength = 0f
                        viewModel.setBassBoost(0)
                        viewModel.setVirtualizer(0)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KivoPurple)
                ) {
                    Text("RESETEAR")
                }
            }
        }
    }
}

@Composable
fun BandSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "${(value * 100).toInt()}%", color = KivoPurple)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f, // Simplified to 0..1 for easier mapping
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = KivoPurple,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}
