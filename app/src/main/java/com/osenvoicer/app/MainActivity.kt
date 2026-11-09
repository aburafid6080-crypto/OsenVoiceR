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
            "Recording saved. Audio effects are not processed yet."
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
            val bytes = recordingFile.readBytes()
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
            status = "Playing original recording (no effect processing yet)"
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
                                    status = "${effect.title} selected. Audio effect processing is not connected yet."
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
                "Note: This build restores the app screen and basic recording/playback. " +
                    "Deep, Robot, Echo and other audio processing still need to be implemented. " +
                    "This app does not currently send changed microphone audio directly into Free Fire.",
                color = Color(0xFFAAA2BC),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
