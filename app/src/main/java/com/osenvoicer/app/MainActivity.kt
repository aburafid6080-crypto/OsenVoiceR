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
import java.util.concurrent.atomic.AtomicReference

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

class MainActivity : ComponentActivity() {
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

    var isLive by remember { mutableStateOf(false) }
    var liveRecorder by remember { mutableStateOf<AudioRecord?>(null) }
    var liveTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var liveActive by remember { mutableStateOf(false) }
    val liveEffectRef = remember { AtomicReference(VoiceEffect.DEEP) }

    fun startRecording() {
        if (isRecording || isPlaying || isLive) return
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

    fun stopLiveVoice() {
        liveActive = false
        try { liveRecorder?.stop() } catch (_: Exception) {}
        try { liveRecorder?.release() } catch (_: Exception) {}
        try {
            liveTrack?.pause()
            liveTrack?.flush()
            liveTrack?.stop()
        } catch (_: Exception) {}
        try { liveTrack?.release() } catch (_: Exception) {}
        liveRecorder = null
        liveTrack = null
        isLive = false
        status = "Live voice changer stopped"
    }

    fun startLiveVoice() {
        if (isLive || isRecording || isPlaying) return
        val minRecordBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val minTrackBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minRecordBuffer <= 0 || minTrackBuffer <= 0) {
            status = "Live audio is unavailable on this device"
            return
        }
        var input: AudioRecord? = null
        var output: AudioTrack? = null
        try {
            val createdInput = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minRecordBuffer * 2, 8192)
            )
            input = createdInput
            if (createdInput.state != AudioRecord.STATE_INITIALIZED) {
                createdInput.release()
                status = "Could not initialize live microphone"
                return
            }
            val createdOutput = AudioTrack.Builder()
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
                .setBufferSizeInBytes(maxOf(minTrackBuffer * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            output = createdOutput
            createdInput.startRecording()
            createdOutput.play()
            liveRecorder = createdInput
            liveTrack = createdOutput
            liveActive = true
            isLive = true
            status = "Live ${liveEffectRef.get().title} voice is on — use headphones"
            val activeInput = createdInput
            val activeOutput = createdOutput
            thread(name = "OsenLiveVoice") {
                val buffer = ByteArray(2048)
                try {
                    while (liveActive) {
                        val count = activeInput.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                        if (count > 1 && liveActive) {
                            val usableCount = count - (count % 2)
                            val changed = applyVoiceEffect(buffer.copyOf(usableCount), liveEffectRef.get())
                            if (changed.isNotEmpty() && liveActive) {
                                activeOutput.write(changed, 0, changed.size, AudioTrack.WRITE_BLOCKING)
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    (context as? ComponentActivity)?.runOnUiThread {
                        if (isLive) {
                            stopLiveVoice()
                            status = "Live voice session ended"
                        }
                    }
                }
            }
        } catch (_: Exception) {
            try { input?.release() } catch (_: Exception) {}
            try { output?.release() } catch (_: Exception) {}
            liveRecorder = null
            liveTrack = null
            liveActive = false
            isLive = false
            status = "Could not start live voice changer"
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
        if (isRecording || isPlaying || isLive) return
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

    val liveMicrophonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLiveVoice() else status = "Microphone permission is required for live voice"
    }

    val effects = VoiceEffect.values()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090711)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF17102D), Color(0xFF090711), Color(0xFF10172B))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "OSEN VOICE R",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "VOICE CHANGER STUDIO",
                color = Color(0xFFB69CFF),
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1730)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Choose Voice Effect", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                    liveEffectRef.set(effect)
                                    status = "${effect.title} selected"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFF7046D8) else Color(0xFF302849)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(effect.title, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21183A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LIVE VOICE CHANGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Speak and hear your selected voice effect in real time", color = Color(0xFFC8BCEB), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (isLive) {
                                stopLiveVoice()
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startLiveVoice()
                            } else {
                                liveMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLive) Color(0xFFB83250) else Color(0xFF267D91))
                    ) {
                        Text(if (isLive) "STOP LIVE VOICE" else "●  START LIVE VOICE")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Headphones recommended to prevent audio feedback. This is live monitoring inside the app, not direct Free Fire microphone routing.",
                        color = Color(0xFFAAA2BC), fontSize = 10.sp, lineHeight = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1730)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("VOICE RECORDER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                            containerColor = if (isRecording) Color(0xFFB83250) else Color(0xFF7046D8)
                        )
                    ) {
                        Text(if (isRecording) "STOP RECORDING" else "●  START RECORDING")
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { playRecording() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRecording && !isPlaying
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

            Spacer(Modifier.height(16.dp))
            Text(
                "Live voice effects are monitored inside this app. Direct routing of changed microphone audio into Free Fire is not provided by this build.",
                color = Color(0xFFAAA2BC),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
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

