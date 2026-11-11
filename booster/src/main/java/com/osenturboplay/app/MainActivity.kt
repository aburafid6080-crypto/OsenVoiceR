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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("♛  OSEN GAMING", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("OSEN TURBOPLAY", color = White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("━  GAME BOOSTER  ━", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonCircle("⚙", Violet, "SETTINGS") { openAppSettings(context) }
                        NeonCircle("⚡", Cyan, "BOOST") { selectedTab.value = "Tools" }
                    }
                }

                AnimatedContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    targetState = selectedTab.value,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 120)) togetherWith
                            fadeOut(animationSpec = tween(120))
                    },
                    label = "page_transition"
                ) { tab ->
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (tab) {
                            "Home" -> HomePage(context, stats.value, status) { stats.value = readDeviceStats(context); status.value = "Device information refreshed" }
                            "Stats" -> StatsPage(stats.value) {
                                stats.value = readDeviceStats(context)
                                status.value = "Device information refreshed"
                            }
                            "Games" -> GamesPage(status) { status.value = launchFreeFire(context) }
                            "Tools" -> ToolsPage(
                                onRefresh = {
                                    stats.value = readDeviceStats(context)
                                    status.value = "Device information refreshed"
                                },
                                onBooster = { openGameSettings(context, status) },
                                onCleaner = { openAppSettings(context) },
                                onDevice = { openDeviceSettings(context) }
                            )
                            else -> AboutPage()
                        }
                    }
                }

                Text(status.value, color = Cyan, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), maxLines = 1)
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF100B20), RoundedCornerShape(18.dp))
                        .border(1.dp, Purple.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).padding(vertical = 10.dp),
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

private fun statsRefresh(context: Context, status: androidx.compose.runtime.MutableState<String>) {
    // Re-read device information when the user taps Refresh.
    status.value = "Device information refreshed"
}

@Composable
private fun AnimeGamerPortrait(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 200f
        val sy = size.height / 200f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7A16FF).copy(alpha = 0.52f), Color(0xFF00D9FF).copy(alpha = 0.20f), Color.Transparent),
                center = p(100f, 100f), radius = 98f * sx
            ),
            radius = 98f * sx, center = p(100f, 100f)
        )
        drawCircle(Color(0xFF00D9FF).copy(alpha = 0.75f), 97f * sx, p(100f, 100f), style = Stroke(width = 2f * sx))
        val hoodie = Path().apply {
            moveTo(34f * sx, 190f * sy)
            cubicTo(39f * sx, 150f * sy, 65f * sx, 137f * sy, 100f * sx, 137f * sy)
            cubicTo(137f * sx, 137f * sy, 164f * sx, 153f * sy, 169f * sx, 190f * sy)
            close()
        }
        drawPath(hoodie, Color(0xFF17102F))
        drawPath(hoodie, Color(0xFF8C24FF), style = Stroke(width = 3f * sx))
        drawRoundRect(Color(0xFF272044), p(83f, 124f), Size(34f * sx, 30f * sy), androidx.compose.ui.geometry.CornerRadius(8f * sx))
        drawOval(Color(0xFFE9B18B), p(62f, 47f), Size(76f * sx, 91f * sy))
        val hair = Path().apply {
            moveTo(57f * sx, 78f * sy)
            lineTo(38f * sx, 47f * sy)
            lineTo(62f * sx, 55f * sy)
            lineTo(55f * sx, 23f * sy)
            lineTo(82f * sx, 43f * sy)
            lineTo(91f * sx, 8f * sy)
            lineTo(108f * sx, 39f * sy)
            lineTo(135f * sx, 19f * sy)
            lineTo(128f * sx, 52f * sy)
            lineTo(157f * sx, 43f * sy)
            lineTo(139f * sx, 83f * sy)
            lineTo(130f * sx, 70f * sy)
            lineTo(67f * sx, 82f * sy)
            close()
        }
        drawPath(hair, Color(0xFFE9EEFF))
        drawPath(hair, Color(0xFF8E58FF), style = Stroke(width = 2.5f * sx))
        drawLine(Color(0xFF9C18FF), p(72f, 42f), p(93f, 20f), 3f * sx, cap = StrokeCap.Round)
        drawLine(Color(0xFF00D9FF), p(103f, 40f), p(132f, 29f), 3f * sx, cap = StrokeCap.Round)
        drawLine(Color(0xFF7B2BFF), p(72f, 91f), p(91f, 94f), 5f * sx, cap = StrokeCap.Round)
        drawLine(Color(0xFF7B2BFF), p(109f, 94f), p(128f, 91f), 5f * sx, cap = StrokeCap.Round)
        val mask = Path().apply {
            moveTo(61f * sx, 101f * sy)
            lineTo(100f * sx, 111f * sy)
            lineTo(139f * sx, 101f * sy)
            lineTo(130f * sx, 132f * sy)
            lineTo(100f * sx, 145f * sy)
            lineTo(70f * sx, 132f * sy)
            close()
        }
        drawPath(mask, Color(0xFF080916))
        drawPath(mask, Color(0xFF00D9FF), style = Stroke(width = 2f * sx))
        drawLine(Color(0xFF9C18FF), p(81f, 123f), p(119f, 123f), 2f * sx)
        drawLine(Color(0xFF00D9FF), p(45f, 177f), p(72f, 158f), 4f * sx, cap = StrokeCap.Round)
        drawLine(Color(0xFFB51CFF), p(128f, 158f), p(157f, 179f), 4f * sx, cap = StrokeCap.Round)
    }
}

@Composable
private fun HeroBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().height(208.dp)
            .border(2.dp, Brush.linearGradient(listOf(Purple, Cyan, Violet, Gold)), RoundedCornerShape(27.dp)),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07091B))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF250044), Color(0xFF080B25), Color(0xFF031B35), Color(0xFF17052D))))
                .padding(15.dp)
        ) {
            // Neon energy streaks and layered character panel recreate the requested gaming-banner layout.
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).size(174.dp)
                    .background(Brush.radialGradient(listOf(Purple.copy(alpha = 0.62f), Cyan.copy(alpha = 0.22f), Color.Transparent)), CircleShape)
                    .border(1.dp, Cyan.copy(alpha = 0.5f), CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.64f).align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("♛  OSEN TURBOPLAY", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("PLAY SMARTER", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("BE STRONGER", color = Color(0xFFE020FF), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("FREE FIRE  •  FREE FIRE MAX", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp))
                Text("Your gaming space. Your next match starts here.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2)
                Text("🔥  READY FOR YOUR NEXT MATCH", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            AnimeGamerPortrait(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).size(132.dp)
            )
            Text("✦ LIVE", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun HomePage(context: Context, stats: DeviceStats, status: androidx.compose.runtime.MutableState<String>, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HeroBanner()

        Card(modifier = Modifier.fillMaxWidth().border(1.5.dp, Brush.linearGradient(listOf(Cyan, Purple, Violet)), RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🚀  YOUR NEXT MATCH STARTS HERE", color = White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("Launch Free Fire and jump straight into the action!", color = Muted, fontSize = 12.sp)
                Button(onClick = { status.value = launchFreeFire(context) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E13F8)), shape = RoundedCornerShape(17.dp)) {
                    Text("▶  PLAY FREE FIRE  🔥", color = White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Text("⚡  Free Fire & Free Fire MAX supported when installed", color = Cyan, fontSize = 10.sp)
            }
        }
        Text("DEVICE OVERVIEW", color = White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
        Text("LIVE DEVICE INFORMATION", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonMetric("▦", "RAM AVAILABLE", stats.ramAvailable, "Memory ${stats.ramUsedPercent}%", Purple, Modifier.weight(1f), stats.ramProgress)
            NeonMetric("▤", "STORAGE FREE", stats.storageFree, "Internal ${stats.storageUsedPercent}%", Cyan, Modifier.weight(1f), stats.storageProgress)
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { openDeviceSettings(context) }.border(1.5.dp, Cyan, RoundedCornerShape(21.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1024)), shape = RoundedCornerShape(21.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📱", fontSize = 31.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("DEVICE INFORMATION", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(stats.deviceName, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = Muted, fontSize = 11.sp)
                }
                Text("VIEW DETAILS  ➜", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        Text("🎮  GAMING TOOLS", color = Purple, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolTile("🚀", "GAME LAUNCHER", "Open games", Purple, Modifier.weight(1f)) { status.value = launchFreeFire(context) }
            ToolTile("⟳", "REFRESH", "Update info", Cyan, Modifier.weight(1f), onRefresh)
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
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageTitle("MY GAMES", "Choose a game installed on this phone")
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("FREE FIRE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Garena Free Fire", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                ActionButton("▶  LAUNCH FREE FIRE", Purple, onLaunch)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.75f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("FREE FIRE MAX", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Garena Free Fire MAX", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                ActionButton("▶  LAUNCH FREE FIRE MAX", Cyan) {
                    status.value = launchSpecificGame(context, "com.dts.freefiremax", "Free Fire MAX")
                }
            }
        }
        Text(
            "Only installed games can be opened. This launcher does not modify game files or guarantee extra FPS.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
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
            .width(76.dp)
            .height(82.dp)
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(listOf(color.copy(alpha = 0.42f), Color(0xFF100B24), Color(0xFF080615))),
                RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), color, Cyan.copy(alpha = 0.85f))),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(symbol, color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun NeonMetric(
    icon: String, title: String, value: String, footer: String, accent: Color,
    modifier: Modifier = Modifier, progress: Float = 0.5f
) {
    Card(
        modifier = modifier.border(1.5.dp, Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.45f), Violet)), RoundedCornerShape(21.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090B20)),
        shape = RoundedCornerShape(21.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(7.dp))
                Text(title, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("GOOD", color = White, fontSize = 8.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(Brush.horizontalGradient(listOf(Purple, accent)), RoundedCornerShape(20.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
            }
            Text(value, color = White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF24145A), RoundedCornerShape(20.dp))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0.03f, 1f)).height(8.dp)
                    .background(Brush.horizontalGradient(listOf(Purple, accent)), RoundedCornerShape(20.dp)))
            }
            Text(footer, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToolTile(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(132.dp).clickable(onClick = onClick).border(1.3.dp, Brush.linearGradient(listOf(accent, Color.White.copy(alpha = 0.45f), accent)), RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(title, color = White, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

private data class DeviceStats(
    val ramAvailable: String,
    val storageFree: String,
    val deviceName: String,
    val ramProgress: Float,
    val storageProgress: Float,
    val ramUsedPercent: Int,
    val storageUsedPercent: Int
)

private fun readDeviceStats(context: Context): DeviceStats {
    val memory = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    val totalRam = memory.totalMem.coerceAtLeast(1L)
    val availableRam = memory.availMem.coerceIn(0L, totalRam)
    val ramProgress = (1f - availableRam.toFloat() / totalRam.toFloat()).coerceIn(0f, 1f)
    val storage = StatFs(Environment.getDataDirectory().path)
    val totalStorage = storage.totalBytes.coerceAtLeast(1L)
    val freeStorage = storage.availableBytes.coerceIn(0L, totalStorage)
    val storageProgress = (1f - freeStorage.toFloat() / totalStorage.toFloat()).coerceIn(0f, 1f)
    val availableRamGb = availableRam.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val freeStorageGb = freeStorage.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return DeviceStats(
        String.format(Locale.getDefault(), "%.1f GB", availableRamGb),
        String.format(Locale.getDefault(), "%.1f GB", freeStorageGb),
        "$manufacturer ${Build.MODEL}",
        ramProgress,
        storageProgress,
        (ramProgress * 100).toInt(),
        (storageProgress * 100).toInt()
    )
}

private fun launchSpecificGame(context: Context, packageName: String, gameName: String): String {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?: return "$gameName is not installed."
    return try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        "$gameName launched successfully"
    } catch (_: Exception) {
        "Could not open $gameName on this device."
    }
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
