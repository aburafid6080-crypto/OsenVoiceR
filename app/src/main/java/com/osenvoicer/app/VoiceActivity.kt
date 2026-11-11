package com.osenvoicer.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioAttributes
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

private const val SAMPLE_RATE = 44100

private enum class VoiceEffect(val title: String) {
    NORMAL("Normal"),
    DEEP("Deep"),
    ROBOT("Robot"),
    ECHO("Echo"),
    RADIO("Radio"),
    MONSTER("Monster"),
    CHIPMUNK("Chipmunk"),
    REVERB("Reverb"),
    PITCH("Pitch")
}

class VoiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OsenVoiceR()
            }
        }
    }
}

@Composable
private fun OsenVoiceR() {
    val context = LocalContext.current
    val recordingFile = remember { File(context.cacheDir, "osen_recording.pcm") }

    var selectedEffect by remember { mutableStateOf(VoiceEffect.DEEP) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready — choose an effect and record your voice") }
    var recorder by remember { mutableStateOf<AudioRecord?>(null) }
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var recordingActive by remember { mutableStateOf(false) }
    var playbackActive by remember { mutableStateOf(false) }

    fun startRecording() {
        if (isRecording || isPlaying) return
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            status = "Microphone is unavailable on this device"
            return
        }

        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer * 2, 8192)
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                status = "Could not initialize microphone"
                return
            }

            audioRecord.startRecording()
            recorder = audioRecord
            recordingActive = true
            isRecording = true
            status = "Recording… speak clearly"

            thread {
                try {
                    FileOutputStream(recordingFile, false).use { output ->
                        val buffer = ByteArray(4096)
                        while (recordingActive) {
                            val count = audioRecord.read(buffer, 0, buffer.size)
                            if (count > 0) output.write(buffer, 0, count)
                        }
                    }
                } catch (_: Exception) {
                    (context as? ComponentActivity)?.runOnUiThread {
                        status = "Recording failed"
                    }
                }
            }
        } catch (_: Exception) {
            status = "Could not start recording"
        }
    }

    fun stopRecording() {
        recordingActive = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
        status = if (recordingFile.exists() && recordingFile.length() > 0L) {
            "Recording saved. Choose an effect and play it back."
        } else {
            "Recording stopped"
        }
    }

    fun stopPlayback() {
        playbackActive = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        isPlaying = false
    }

    fun playRecording() {
        if (isRecording || isPlaying) return
        if (!recordingFile.exists() || recordingFile.length() == 0L) {
            status = "Record your voice first"
            return
        }

        try {
            val originalBytes = recordingFile.readBytes()
            val bytes = applyVoiceEffect(originalBytes, selectedEffect)
            val minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            playbackActive = true
            isPlaying = true
            status = "Playing ${selectedEffect.title} effect"
            track.play()

            thread {
                try {
                    var offset = 0
                    while (offset < bytes.size && playbackActive) {
                        val written = track.write(bytes, offset, minOf(4096, bytes.size - offset))
                        if (written <= 0) break
                        offset += written
                    }
                } catch (_: Exception) {
                } finally {
                    (context as? ComponentActivity)?.runOnUiThread {
                        stopPlayback()
                        status = "Playback finished"
                    }
                }
            }
        } catch (_: Exception) {
            stopPlayback()
            status = "Could not play recording"
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else status = "Microphone permission is required"
    }

    val effects = VoiceEffect.values()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070416)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF10052A), Color(0xFF070416), Color(0xFF10052A))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🎙", fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "OSEN VOICE R",
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "VOICE CHANGER STUDIO",
                        color = Color(0xFFC04BFF),
                        fontSize = 12.sp,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("✧", color = Color(0xFFB52BFF), fontSize = 34.sp)
            }
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF110B29)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8C24FF))
            ) {
                Column(
                    Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B0D3D), Color(0xFF0D0921))))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙", color = Color(0xFFB82DFF), fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Choose Voice Effect", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false
                    ) {
                        items(effects) { effect ->
                            val selected = selectedEffect == effect
                            Button(
                                onClick = {
                                    selectedEffect = effect
                                    status = "${effect.title} selected"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF7624F5) else Color(0xFF292044)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE15BFF)) else null,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp)
                            ) {
                                Text(
                                    if (selected) "${effect.title}  ✓" else effect.title,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF160A32)),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF9C2BFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF241047), Color(0xFF100925))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎮", fontSize = 25.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "FREE FIRE MODE",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = Color(0xFF173B2A),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB43B))
                        ) {
                            Text(
                                "NOT CONNECTED",
                                color = Color(0xFFFFD36B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Choose a voice effect above and test it with your recordings. The selected effect is applied when you play back your recording.",
                        color = Color(0xFFE8DFFF),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFF29143D),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB33CDE))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ⓘ", color = Color(0xFFD77BFF), fontSize = 20.sp)
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "Free Fire microphone routing: Not supported by this APK. Changed voice cannot be sent directly into Free Fire voice chat.",
                                color = Color(0xFFFFB8EE),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100A28)),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7E35E8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1D1240), Color(0xFF0D0922))))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎙  VOICE RECORDER", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        status,
                        color = Color(0xFFC8BCEB),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            } else if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                startRecording()
                            } else {
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFB83250) else Color(0xFF7818F5)
                        ),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp)
                    ) {
                        Text(if (isRecording) "STOP RECORDING" else "●  START RECORDING")
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { playRecording() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRecording && !isPlaying,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF9D7BFF))
                    ) {
                        Text("▶  PLAY RECORDING", color = Color.White)
                    }
                    if (isPlaying) {
                        TextButton(onClick = { stopPlayback() }) {
                            Text("STOP PLAYBACK", color = Color(0xFFFF8DA5))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}


/**
 * Offline PCM effect processing for the recorded sample.
 * Input/output: mono, signed 16-bit little-endian PCM.
 */
private fun applyVoiceEffect(input: ByteArray, effect: VoiceEffect): ByteArray {
    val usableSize = input.size - (input.size % 2)
    if (usableSize < 2 || effect == VoiceEffect.NORMAL) {
        return input.copyOf(usableSize)
    }

    val samples = ShortArray(usableSize / 2) { index ->
        val low = input[index * 2].toInt() and 0xFF
        val high = input[index * 2 + 1].toInt()
        ((high shl 8) or low).toShort()
    }

    fun clamp(value: Double): Short =
        value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    fun resample(rate: Double): ShortArray {
        val outputSize = (samples.size / rate).toInt().coerceAtLeast(1)
        return ShortArray(outputSize) { i ->
            val position = i * rate
            val left = position.toInt().coerceIn(0, samples.lastIndex)
            val right = (left + 1).coerceAtMost(samples.lastIndex)
            val fraction = position - left
            clamp(samples[left] * (1.0 - fraction) + samples[right] * fraction)
        }
    }

    val processed: ShortArray = when (effect) {
        VoiceEffect.NORMAL -> samples
        VoiceEffect.DEEP -> resample(0.82)
        VoiceEffect.CHIPMUNK -> resample(1.28)
        VoiceEffect.PITCH -> resample(1.12)
        VoiceEffect.MONSTER -> {
            val lowered = resample(0.68)
            ShortArray(lowered.size) { i -> clamp(lowered[i] * 1.12) }
        }
        VoiceEffect.ROBOT -> {
            val modulationHz = 38.0
            ShortArray(samples.size) { i ->
                val carrier = kotlin.math.sin(2.0 * Math.PI * modulationHz * i / SAMPLE_RATE)
                clamp(samples[i] * carrier * 1.35)
            }
        }
        VoiceEffect.ECHO -> {
            val delay = (SAMPLE_RATE * 0.24).toInt()
            val output = ShortArray(samples.size + delay * 2)
            for (i in samples.indices) {
                output[i] = samples[i]
                output[i + delay] = clamp(output[i + delay] + samples[i] * 0.48)
                output[i + delay * 2] = clamp(output[i + delay * 2] + samples[i] * 0.22)
            }
            output
        }
        VoiceEffect.REVERB -> {
            val delay1 = (SAMPLE_RATE * 0.055).toInt()
            val delay2 = (SAMPLE_RATE * 0.091).toInt()
            val output = ShortArray(samples.size + delay2)
            for (i in samples.indices) {
                output[i] = clamp(
                    samples[i] +
                        (if (i >= delay1) output[i - delay1] * 0.24 else 0.0) +
                        (if (i >= delay2) output[i - delay2] * 0.16 else 0.0)
                )
            }
            output
        }
        VoiceEffect.RADIO -> {
            var previousInput = 0.0
            var previousOutput = 0.0
            ShortArray(samples.size) { i ->
                val current = samples[i].toDouble()
                val highPassed = 0.88 * (previousOutput + current - previousInput)
                previousInput = current
                previousOutput = highPassed
                // Gentle saturation gives a narrow-band, radio-like character.
                clamp(kotlin.math.tanh(highPassed / 9000.0) * 12500.0)
            }
        }
    }

    val output = ByteArray(processed.size * 2)
    for (i in processed.indices) {
        val value = processed[i].toInt()
        output[i * 2] = (value and 0xFF).toByte()
        output[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
    }
    return output
}

