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

private val Bg = Color(0xFF05040D)
private val Panel = Color(0xFF100D20)
private val Purple = Color(0xFF9C18FF)
private val Violet = Color(0xFF6D35FF)
private val Cyan = Color(0xFF00D9FF)
private val Green = Color(0xFF00E58B)
private val Gold = Color(0xFFFFB52E)
private val White = Color(0xFFF8F5FF)
private val Muted = Color(0xFFB8B0CC)

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
    val status = remember { mutableStateOf("TurboPlay is ready for your next match") }
    val selectedTab = remember { mutableStateOf("Home") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF10051F), Bg, Color(0xFF020713))))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("♛  OSEN GAMING", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("OSEN", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("TURBOPLAY", color = Purple, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                        Text("━  GAME BOOSTER  ━", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonCircle("⚙", Violet)
                        NeonCircle("⚡", Cyan)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.75f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Column(
                        modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF201044), Color(0xFF0B1028), Color(0xFF17052B))))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("PLAY SMARTER\nBE STRONGER", color = White, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black)
                        Text("FREE FIRE  •  FREE FIRE MAX", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Text("Your gaming space. Your next victory starts here.", color = Muted, fontSize = 12.sp)
                        Button(
                            onClick = { status.value = if (launchFreeFire(context)) "Free Fire launch requested" else "Free Fire not found. Install it first." },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("▶   PLAY FREE FIRE  🔥", color = Color(0xFF170B20), fontSize = 15.sp, fontWeight = FontWeight.Black) }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF110A22))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🚀  YOUR NEXT MATCH STARTS HERE", color = White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Launch Free Fire and jump into the action!", color = Muted, fontSize = 12.sp)
                        Button(
                            onClick = { status.value = if (launchFreeFire(context)) "Free Fire launch requested" else "Free Fire not found. Install it first." },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            shape = RoundedCornerShape(15.dp)
                        ) { Text("▶  LAUNCH GAME", color = White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                        Text("⚡  Free Fire & Free Fire MAX supported when installed", color = Cyan, fontSize = 10.sp)
                    }
                }

                Text("DEVICE OVERVIEW", color = White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonMetric("▦", "RAM AVAILABLE", stats.value.ramAvailable, "MEMORY", Purple, Modifier.weight(1f))
                    NeonMetric("▤", "STORAGE FREE", stats.value.storageFree, "INTERNAL", Cyan, Modifier.weight(1f))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📱", fontSize = 31.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("▣  DEVICE INFORMATION", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(stats.value.deviceName, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = Muted, fontSize = 11.sp)
                        }
                        Text("VIEW\nDETAILS  ➜", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }

                Text("🎮  GAMING TOOLS", color = Purple, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolTile("🚀", "GAME\nLAUNCHER", "Open games", Purple, Modifier.weight(1f)) {
                        status.value = if (launchFreeFire(context)) "Free Fire launch requested" else "Free Fire not found. Install it first."
                    }
                    ToolTile("⟳", "REFRESH", "Update info", Cyan, Modifier.weight(1f)) {
                        stats.value = readDeviceStats(context); status.value = "Device information refreshed"
                    }
                    ToolTile("⚡", "BOOSTER", "Performance info", Green, Modifier.weight(1f)) {
                        status.value = "Android manages game performance automatically; no FPS boost is guaranteed."
                    }
                    ToolTile("▤", "CLEANER", "App storage", Gold, Modifier.weight(1f)) {
                        status.value = "Use Android Settings to review and clear app storage safely."
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF160C2C)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💡  GAMING NOTE", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("TurboPlay provides quick game launch and device information. Android controls many performance settings automatically; this app does not promise extra FPS or unfair advantages.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }

                Text(status.value, color = Cyan, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF100B20), RoundedCornerShape(18.dp)).padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("⌂" to "Home", "▥" to "Stats", "🎮" to "Games", "◇" to "Tools", "♛" to "About").forEach { item ->
                        Column(
                            modifier = Modifier.clickable { selectedTab.value = item.second; status.value = when(item.second) {
                                "Home" -> "Home dashboard"
                                "Stats" -> "RAM and storage values are shown above"
                                "Games" -> "Use Play Free Fire to launch your installed game"
                                "Tools" -> "Gaming tools are shown above"
                                else -> "OSEN TURBOPLAY 1.1"
                            }}.padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(item.first, color = if (selectedTab.value == item.second) Purple else Muted, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text(item.second, color = if (selectedTab.value == item.second) White else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("OSEN GAMING  •  TURBOPLAY 1.2", color = Muted.copy(alpha = 0.55f), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun NeonCircle(symbol: String, color: Color) {
    Column(
        modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(color, Violet)), CircleShape)
            .border(1.dp, color, CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { Text(symbol, color = White, fontSize = 25.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun NeonMetric(icon: String, title: String, value: String, footer: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.9f), RoundedCornerShape(19.dp)),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(19.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(title, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, color = White, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(footer, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun ToolTile(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(126.dp).clickable(onClick = onClick)
            .border(1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(17.dp)),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(title, color = White, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
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
