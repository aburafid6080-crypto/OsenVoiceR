package com.osenvoicer.app
import androidx.activity.compose.setContent
import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import java.io.File

private val Ink = Color(0xFF080914)
private val Purple = Color(0xFF8B5CF6)
private val Blue = Color(0xFF38BDF8)

class MainActivity : ComponentActivity() {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OsenApp(
            onRecord = { startRecording() },
            onStop = { stopRecording() },
            onPlay = { playRecording() },
            hasRecording = { recordingFile?.exists() == true }
        ) }
    }

    private fun startRecording() {
        try {
            val file = File(cacheDir, "osen_recording.m4a")
            recordingFile = file
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            recorder = r
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (_: Exception) { recorder = null }
    }

    private fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) { }
        try { recorder?.release() } catch (_: Exception) { }
        recorder = null
    }

    private fun playRecording() {
        val file = recordingFile ?: return
        if (!file.exists()) return
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { it.release(); player = null }
            }
        } catch (_: Exception) { player = null }
    }

    override fun onDestroy() {
        stopRecording()
        player?.release()
        super.onDestroy()
    }
}

@Composable
private fun OsenApp(
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onPlay: () -> Unit,
    hasRecording: () -> Boolean
) {
    var recording by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to create your voice") }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        status = if (granted) "Microphone ready" else "Microphone permission required"
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Color(0xFF15172A), primary = Purple
    )) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Ink, Color(0xFF17102D), Ink))
            ).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("OSEN VOICE R", color = Color.White, fontSize = 27.sp)
            Text("GAMING VOICE STUDIO", color = Blue, fontSize = 11.sp)
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191B31)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("VOICE STUDIO", color = Purple, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Create your sound.", color = Color.White, fontSize = 23.sp)
                    Text(status, color = Color.LightGray, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (!permissionGranted) permission.launch(Manifest.permission.RECORD_AUDIO)
                            else if (!recording) {
                                onRecord(); recording = true; status = "Recording…"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) { Text(if (recording) "RECORDING" else "START RECORDING") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onStop(); recording = false; status = "Recording stopped" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("STOP") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onPlay(); status = "Playing saved test recording" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("PLAY RECORDING") }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("VOICE EFFECTS", color = Blue, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            val effects = listOf("Deep", "Robot", "Echo", "Radio", "Monster", "Chipmunk", "Reverb", "Pitch")
            effects.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { effect ->
                        Card(
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF20213A))
                        ) {
                            Box(Modifier.fillMaxWidth().padding(15.dp), contentAlignment = Alignment.Center) {
                                Text(effect, color = Color.White)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Local recording prototype • Effects UI preview only", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
