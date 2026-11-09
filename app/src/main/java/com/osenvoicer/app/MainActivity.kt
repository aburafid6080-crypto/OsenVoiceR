package com.osenvoicer.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

private const val RATE = 44100
private enum class Effect(val label: String) {
    Normal("Normal"), Deep("Deep"), Robot("Robot"), Echo("Echo"), Radio("Radio"),
    Monster("Monster"), Chipmunk("Chipmunk"), Reverb("Reverb"), Pitch("Pitch")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OsenVoiceR() }
    }
}

@Composable
private fun OsenVoiceR() {
    val ctx = LocalContext.current
    val file = remember { File(ctx.cacheDir, "osen_recording.pcm") }
    var effect by remember { mutableStateOf(Effect.Deep) }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to create your voice") }
    var recorder by remember { mutableStateOf<AudioRecord?>(null) }
    var track by remember { mutableStateOf<AudioTrack?>(null) }
    var recOn by remember { mutableStateOf(false) }
    var playOn by remember { mutableStateOf(false) }

    fun startRec() {
        if (recording || playing) return
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) { status = "Microphone unavailable"; return }
        try {
            val r = AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, 8192))
            if (r.state != AudioRecord.STATE_INITIALIZED) { r.release(); status = "Microphone could not start"; return }
            r.startRecording()
            recorder = r; recOn = true; recording = true; status = "Recording… speak now"
            thread {
                try {
                    FileOutputStream(file, false).use { out ->
                        val b = ByteArray(4096)
                        while (recOn) {
                            val n = r.read(b, 0, b.size)
                            if (n > 0) out.write(b, 0, n)
                        }
                    }
                } catch (_: Exception) { (ctx as ComponentActivity).runOnUiThread { status = "Recording error" } }
            }
        } catch (_: Exception) { status = "Could not start recording" }
    }

    fun stopRec() {
        recOn = false
        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
        recorder = null; recording = false
        status = if (file.exists() && file.length() > 0) "Recording saved • ${effect.label} selected" else "Recording stopped"
    }

    fun stopPlay() {
        playOn = false
        try { track?.pause(); track?.flush(); track?.stop(); track?.release() } catch (_: Exception) {}
        track = null; playing = false
    }
