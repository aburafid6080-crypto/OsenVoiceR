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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val Background = Color(0xFF080713)
private val SurfaceDark = Color(0xFF151126)
private val SurfaceLight = Color(0xFF201936)
private val Purple = Color(0xFF9B5CFF)
private val Blue = Color(0xFF36CFFF)
private val TextMuted = Color(0xFFAAA2BD)
private val White = Color(0xFFF8F5FF)

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
    val status = remember { mutableStateOf("Your gaming space is ready") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF19102E), Background, Color(0xFF090D18))))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("OSEN GAMING", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("TURBOPLAY", color = White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("GAME BOOSTER", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    }
                    Column(
                        modifier = Modifier.size(54.dp).background(
                            Brush.linearGradient(listOf(Purple, Blue)), CircleShape
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚡", fontSize = 28.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    BoxDot()
                    Text("SYSTEM READY", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("VERSION 1.1", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = 0.75f), RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF30204D), Color(0xFF171126), Color(0xFF111A2C))))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text("YOUR NEXT MATCH
STARTS HERE", color = White, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                        Text("Launch Free Fire and jump straight into the action.", color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                        Button(
                            onClick = {
                                status.value = if (launchFreeFire(context)) "Free Fire launch requested" else "Free Fire not found. Install the game first."
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("▶   PLAY FREE FIRE", color = White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                        Text("Supports Free Fire and Free Fire MAX when installed", color = TextMuted, fontSize = 10.sp)
                    }
                }

                SectionTitle("DEVICE OVERVIEW", "LIVE DEVICE INFORMATION")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("RAM AVAILABLE", stats.value.ramAvailable, "MEMORY", Modifier.weight(1f))
                    MetricCard("STORAGE FREE", stats.value.storageFree, "INTERNAL", Modifier.weight(1f))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Blue.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📱", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("YOUR DEVICE", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(stats.value.deviceName, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                SectionTitle("GAMING TOOLS", "QUICK ACCESS")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolCard("🎮", "GAME LAUNCHER", "Open Free Fire", Modifier.weight(1f)) {
                        status.value = if (launchFreeFire(context)) "Free Fire launch requested" else "Free Fire not found. Install the game first."
                    }
                    ToolCard("↻", "REFRESH", "Update device info", Modifier.weight(1f)) {
                        stats.value = readDeviceStats(context)
                        status.value = "Device information refreshed"
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ℹ  GAMING NOTE", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Text("Android controls many performance settings automatically. TurboPlay offers quick game launch and device information; it does not promise extra FPS or modify Free Fire.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                }

                Text(status.value, color = Blue, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("HOME", color = White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text("   •   ", color = Purple, fontSize = 10.sp)
                    Text("GAMING TOOLS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("   •   ", color = Purple, fontSize = 10.sp)
                    Text("OSEN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun BoxDot() {
    Spacer(modifier = Modifier.size(7.dp).background(Blue, CircleShape))
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Text(subtitle, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun MetricCard(title: String, value: String, footer: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, Blue.copy(alpha = 0.32f), RoundedCornerShape(19.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(19.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(footer, color = Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun ToolCard(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick).border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(icon, color = Purple, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(title, color = White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
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
