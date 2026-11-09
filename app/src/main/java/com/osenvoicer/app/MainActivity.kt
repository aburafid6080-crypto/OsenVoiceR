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
