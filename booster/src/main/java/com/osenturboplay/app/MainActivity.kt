package com.osenturboplay.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
    val status = remember { mutableStateOf("TurboPlay is ready") }
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
                        Text("OSEN TURBOPLAY", color = White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("━  GAME BOOSTER  ━", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonCircle("⚙", Violet, "SETTINGS") { openAppSettings(context) }
                        NeonCircle("⚡", Cyan, "BOOST") { selectedTab.value = "Tools" }
                    }
                }

                when (selectedTab.value) {
                    "Home" -> HomePage(context, stats.value, status)
                    "Stats" -> StatsPage(stats.value) { stats.value = readDeviceStats(context); status.value = "Device information refreshed" }
                    "Games" -> GamesPage(status) { status.value = launchFreeFire(context) }
                    "Tools" -> ToolsPage(
                        onRefresh = { stats.value = readDeviceStats(context); status.value = "Device information refreshed" },
                        onBooster = { openGameSettings(context, status) },
                        onCleaner = { openAppSettings(context) },
                        onDevice = { openDeviceSettings(context) }
                    )
                    else -> AboutPage()
                }

                Text(status.value, color = Cyan, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF100B20), RoundedCornerShape(18.dp))
                        .border(1.dp, Purple.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("⌂" to "Home", "▥" to "Stats", "🎮" to "Games", "◇" to "Tools", "♛" to "About").forEach { item ->
                        Column(
                            modifier = Modifier.clickable { selectedTab.value = item.second }.padding(horizontal = 5.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(item.first, color = if (selectedTab.value == item.second) Purple else Muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(item.second, color = if (selectedTab.value == item.second) White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("OSEN GAMING  •  TURBOPLAY 1.3", color = Muted.copy(alpha = 0.55f), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HomePage(context: Context, stats: DeviceStats, status: androidx.compose.runtime.MutableState<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(modifier = Modifier.fillMaxWidth().border(1.dp, Purple, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("🚀  YOUR NEXT MATCH STARTS HERE", color = White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("Launch Free Fire and jump into the action!", color = Muted, fontSize = 12.sp)
                Button(onClick = { status.value = launchFreeFire(context) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(15.dp)) {
                    Text("▶  LAUNCH GAME", color = White, fontWeight = FontWeight.ExtraBold)
                }
                Text("⚡  Free Fire & Free Fire MAX supported when installed", color = Cyan, fontSize = 10.sp)
            }
        }
        Text("DEVICE OVERVIEW", color = White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonMetric("▦", "RAM AVAILABLE", stats.ramAvailable, "MEMORY", Purple, Modifier.weight(1f))
            NeonMetric("▤", "STORAGE FREE", stats.storageFree, "INTERNAL", Cyan, Modifier.weight(1f))
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { openDeviceSettings(context) }.border(1.dp, Cyan, RoundedCornerShape(20.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📱", fontSize = 30.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DEVICE INFORMATION", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stats.deviceName, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = Muted, fontSize = 11.sp)
                }
                Text("VIEW DETAILS ➜", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("🎮  GAMING TOOLS", color = Purple, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolTile("🚀", "GAME LAUNCHER", "Open Free Fire", Purple, Modifier.weight(1f)) { status.value = launchFreeFire(context) }
            ToolTile("⟳", "REFRESH", "Update info", Cyan, Modifier.weight(1f)) { status.value = "Device information refreshed" }
            ToolTile("⚡", "BOOSTER", "Game settings", Green, Modifier.weight(1f)) { openGameSettings(context, status) }
            ToolTile("▤", "CLEANER", "App storage", Gold, Modifier.weight(1f)) { openAppSettings(context) }
        }
        NoteCard()
    }
}

@Composable
private fun StatsPage(stats: DeviceStats, onRefresh: () -> Unit) {
    PageTitle("DEVICE STATISTICS", "Live information reported by Android")
    NeonMetric("▦", "AVAILABLE RAM", stats.ramAvailable, "CURRENT FREE MEMORY", Purple)
    NeonMetric("▤", "FREE STORAGE", stats.storageFree, "INTERNAL STORAGE", Cyan)
    InfoCard("DEVICE", stats.deviceName)
    InfoCard("ANDROID VERSION", "Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}")
    ActionButton("↻  REFRESH DEVICE STATS", Cyan, onRefresh)
    Text("RAM availability changes as apps open and close. Values are estimates reported by Android.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun GamesPage(status: androidx.compose.runtime.MutableState<String>, onLaunch: () -> Unit) {
    PageTitle("MY GAMES", "Launch a supported game installed on this phone")
    InfoCard("FREE FIRE", "Garena Free Fire")
    ActionButton("▶  LAUNCH FREE FIRE", Purple, onLaunch)
    InfoCard("FREE FIRE MAX", "Garena Free Fire MAX")
    Text("The launcher checks whether either supported game is installed. Other games are not listed yet.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun ToolsPage(onRefresh: () -> Unit, onBooster: () -> Unit, onCleaner: () -> Unit, onDevice: () -> Unit) {
    PageTitle("GAMING TOOLS", "Quick shortcuts for your device")
    ActionButton("↻  REFRESH DEVICE INFO", Cyan, onRefresh)
    ActionButton("⚡  OPEN ANDROID GAME SETTINGS", Green, onBooster)
    ActionButton("▤  OPEN APP STORAGE SETTINGS", Gold, onCleaner)
    ActionButton("📱  DEVICE SETTINGS", Purple, onDevice)
    NoteCard()
}

@Composable
private fun AboutPage() {
    PageTitle("ABOUT OSEN TURBOPLAY", "Version 1.3")
    InfoCard("DEVELOPER", "Abu Rafid")
    InfoCard("PURPOSE", "Game launcher and device information dashboard")
    NoteCard()
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.55f), RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text(value, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = color), shape = RoundedCornerShape(14.dp)) {
        Text(label, color = White, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun NoteCard() {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = 0.7f), RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF160C2C)), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("💡  GAMING NOTE", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("TurboPlay provides game launch and device settings shortcuts. Android controls performance automatically; this app cannot promise extra FPS or safely force-close other apps.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun NeonCircle(symbol: String, color: Color, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(68.dp)
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(listOf(color.copy(alpha = 0.32f), Color(0xFF100B24), Color(0xFF080615))),
                RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), color, Cyan.copy(alpha = 0.8f))),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(vertical = 7.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(symbol, color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.45.sp)
    }
}

@Composable
private fun NeonMetric(icon: String, title: String, value: String, footer: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, accent, RoundedCornerShape(19.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(19.dp)) {
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
    Card(modifier = modifier.height(126.dp).clickable(onClick = onClick).border(1.dp, accent, RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
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
    return DeviceStats(String.format(Locale.getDefault(), "%.1f GB", availableRamGb), String.format(Locale.getDefault(), "%.1f GB", freeStorageGb), "$manufacturer ${Build.MODEL}")
}

private fun launchFreeFire(context: Context): String {
    for (packageName in listOf("com.dts.freefireth", "com.dts.freefiremax")) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Game launched successfully"
        }
    }
    return "Free Fire not found. Install Free Fire or Free Fire MAX first."
}

private fun openAppSettings(context: Context) {
    safeOpen(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${context.packageName}") })
}

private fun openDeviceSettings(context: Context) {
    safeOpen(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
}

private fun openGameSettings(context: Context, status: androidx.compose.runtime.MutableState<String>) {
    try {
        safeOpen(context, Intent("android.settings.GAME_SETTINGS"))
        status.value = "Opened Android game settings if supported by this phone"
    } catch (_: Exception) {
        status.value = "Game settings are not available on this Android version"
    }
}

private fun safeOpen(context: Context, intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        // Some Android builds do not provide every optional settings screen.
    }
}
