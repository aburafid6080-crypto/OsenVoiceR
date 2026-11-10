package com.osenturboplay.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Night = Color(0xFF090713)
private val Panel = Color(0xFF171126)
private val Purple = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF35DDF5)
private val Muted = Color(0xFFB8B1C9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TurboPlayScreen() }
    }
}

@Composable
private fun TurboPlayScreen() {
    val context = LocalContext.current
    val stats = remember { mutableStateOf(readDeviceStats(context)) }
    val status = remember { mutableStateOf("Your gaming dashboard is ready") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF17102B), Night, Color(0xFF080D19))))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OSEN", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                        Text("TURBOPLAY", color = Purple, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("GAME BOOSTER", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    }
                    Text("⚡", fontSize = 42.sp)
                }
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("READY FOR BATTLE?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Launch your game and check your device status.", color = Muted, fontSize = 13.sp)
                        Button(
                            onClick = {
                                val launched = launchFreeFire(context)
                                status.value = if (launched) "Free Fire launch requested" else "Free Fire was not found. Install it first."
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("▶  LAUNCH FREE FIRE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    }
                }
                Text("DEVICE STATUS", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("RAM AVAILABLE", stats.value.ramAvailable, Modifier.weight(1f))
                    StatCard("STORAGE FREE", stats.value.storageFree, Modifier.weight(1f))
                }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("DEVICE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(stats.value.deviceName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = Muted, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = { stats.value = readDeviceStats(context); status.value = "Device information refreshed" },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25203A)),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("↻  REFRESH DEVICE STATUS", color = Cyan, fontWeight = FontWeight.Bold) }
                Text(status.value, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.weight(1f))
                Text("OSEN GAMING • TURBOPLAY 1.0", color = Muted.copy(alpha = 0.65f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(
                    "Note: Android apps cannot guarantee extra FPS or magically free RAM. This version provides game launch and device-status tools.",
                    color = Muted.copy(alpha = 0.75f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, Cyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private data class DeviceStats(val ramAvailable: String, val storageFree: String, val deviceName: String)

private fun readDeviceStats(context: Context): DeviceStats {
    val memory = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    val availableRamGb = memory.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val storage = StatFs(Environment.getDataDirectory().path)
    val freeStorageGb = storage.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return DeviceStats(
        String.format(Locale.getDefault(), "%.1f GB", availableRamGb),
        String.format(Locale.getDefault(), "%.1f GB", freeStorageGb),
        "$manufacturer ${Build.MODEL}"
    )
}

private fun launchFreeFire(context: Context): Boolean {
    for (packageName in listOf("com.dts.freefireth", "com.dts.freefiremax")) {
        val launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }
    }
    return false
}
