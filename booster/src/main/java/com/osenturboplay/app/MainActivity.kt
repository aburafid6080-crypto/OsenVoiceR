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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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

class TurboPlayActivity : ComponentActivity() {
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
    val boostAnimation = remember { mutableStateOf(false) }

    LaunchedEffect(boostAnimation.value) {
        if (boostAnimation.value) {
            delay(1500)
            boostAnimation.value = false
        }
    }

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
                PremiumHeader(
                    onSettings = { openAppSettings(context) },
                    onBoost = {
                        stats.value = readDeviceStats(context)
                        boostAnimation.value = true
                        status.value = "BOOST animation complete • No system performance settings changed"
                    },
                    isBoosting = boostAnimation.value
                )

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
                                onCleaner = { openAppSettings(context) },
                                onPhoneSettings = { openDeviceSettings(context) },
                                onDeviceInfo = { selectedTab.value = "Stats" }
                            )
                            else -> AboutPage()
                        }
                    }
                }

                Text(status.value, color = Cyan, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), maxLines = 1)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(22.dp), clip = false)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF251044), Color(0xFF100B20), Color(0xFF070615))),
                            RoundedCornerShape(22.dp)
                        )
                        .border(1.5.dp, Brush.horizontalGradient(listOf(Purple, Cyan, Violet)), RoundedCornerShape(22.dp))
                        .padding(horizontal = 7.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("⌂" to "Home", "▥" to "Stats", "🎮" to "Games", "◇" to "Tools", "♛" to "About").forEach { item ->
                        val isSelected = selectedTab.value == item.second
                        val accent = if (item.second == "Stats") Cyan else Purple
                        Column(
                            modifier = Modifier.weight(1f).height(62.dp)
                                .shadow(if (isSelected) 13.dp else 0.dp, RoundedCornerShape(15.dp), clip = false)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    if (isSelected) Brush.verticalGradient(listOf(Color(0xFF5B12A5), Color(0xFF210B49), Color(0xFF100B25)))
                                    else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .border(
                                    if (isSelected) 1.4.dp else 0.dp,
                                    if (isSelected) Brush.verticalGradient(listOf(Color(0xFFFF75FF), Cyan, Purple))
                                    else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                                    RoundedCornerShape(15.dp)
                                )
                                .clickable { selectedTab.value = item.second }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                item.first,
                                color = if (isSelected) Color.White else Muted,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(shadow = Shadow(if (isSelected) accent else Purple.copy(alpha = 0.55f), Offset(0f, 0f), if (isSelected) 13f else 5f))
                            )
                            Text(
                                item.second,
                                color = if (isSelected) White else Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(shadow = Shadow(if (isSelected) Purple else Color.Black, Offset(1f, 2f), 4f))
                            )
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
private fun PremiumOsenMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.47f
        // Original OSEN crest: layered neon rings, crown and central lightning mark.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF34106E), Color(0xFF071B3B), Color(0xFF050817)),
                center = Offset(cx, cy), radius = r
            ), radius = r, center = Offset(cx, cy)
        )
        drawCircle(Color(0xFF00D9FF).copy(alpha = 0.9f), r * 0.96f, Offset(cx, cy), style = Stroke(width = r * 0.025f))
        drawCircle(Color(0xFF9C18FF).copy(alpha = 0.9f), r * 0.86f, Offset(cx, cy), style = Stroke(width = r * 0.018f))
        val crown = Path().apply {
            moveTo(cx - r * 0.48f, cy - r * 0.26f)
            lineTo(cx - r * 0.38f, cy - r * 0.52f)
            lineTo(cx - r * 0.12f, cy - r * 0.34f)
            lineTo(cx, cy - r * 0.62f)
            lineTo(cx + r * 0.14f, cy - r * 0.34f)
            lineTo(cx + r * 0.39f, cy - r * 0.52f)
            lineTo(cx + r * 0.49f, cy - r * 0.26f)
            close()
        }
        drawPath(crown, Brush.horizontalGradient(listOf(Color(0xFFFFC83D), Color(0xFFFFE88A), Color(0xFFFF9D18))))
        drawPath(crown, Color(0xFFFFE7A0), style = Stroke(width = r * 0.025f))
        val bolt = Path().apply {
            moveTo(cx + r * 0.10f, cy - r * 0.22f)
            lineTo(cx - r * 0.20f, cy + r * 0.12f)
            lineTo(cx - r * 0.015f, cy + r * 0.12f)
            lineTo(cx - r * 0.12f, cy + r * 0.48f)
            lineTo(cx + r * 0.25f, cy + r * 0.02f)
            lineTo(cx + r * 0.045f, cy + r * 0.02f)
            close()
        }
        drawPath(bolt, Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFB9F8FF), Color(0xFF9C18FF))))
        drawPath(bolt, Color.White.copy(alpha = 0.9f), style = Stroke(width = r * 0.018f))
        drawCircle(Color(0xFF00D9FF), r * 0.035f, Offset(cx - r * 0.60f, cy + r * 0.42f))
        drawCircle(Color(0xFFE020FF), r * 0.035f, Offset(cx + r * 0.60f, cy + r * 0.42f))
    }
}

@Composable
private fun HeroBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().height(238.dp)
            .border(2.dp, Brush.linearGradient(listOf(Purple, Cyan, Violet, Gold)), RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07091B))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF18002E), Color(0xFF080B25), Color(0xFF031B35), Color(0xFF160426))))
                .padding(13.dp)
        ) {
            // Layered neon energy streaks behind the character, matching the supplied gaming reference.
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val streak = Path().apply {
                    moveTo(-w * 0.08f, h * 0.72f)
                    cubicTo(w * 0.18f, h * 0.30f, w * 0.22f, h * 0.92f, w * 0.48f, h * 0.18f)
                    cubicTo(w * 0.62f, h * 0.02f, w * 0.78f, h * 0.24f, w * 1.08f, -h * 0.08f)
                }
                drawPath(streak, Purple.copy(alpha = 0.42f), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(streak, Cyan.copy(alpha = 0.32f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                for (i in 0..5) {
                    val y = h * (0.12f + i * 0.14f)
                    drawLine(
                        color = if (i % 2 == 0) Cyan.copy(alpha = 0.22f) else Purple.copy(alpha = 0.25f),
                        start = Offset(w * (0.48f + i * 0.035f), y),
                        end = Offset(w * 1.04f, y - h * 0.12f),
                        strokeWidth = (1.2f + i % 2).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Purple.copy(alpha = 0.48f), Cyan.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * 0.78f, h * 0.48f),
                        radius = h * 0.60f
                    ),
                    radius = h * 0.60f,
                    center = Offset(w * 0.78f, h * 0.48f)
                )
            }
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 1.dp).size(176.dp)
                    .border(1.5.dp, Cyan.copy(alpha = 0.62f), CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF38106D).copy(alpha = 0.55f), Color(0xFF042D50).copy(alpha = 0.35f), Color.Transparent)),
                        CircleShape
                    )
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.66f).align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("✦  OSEN TURBOPLAY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                Spacer(Modifier.height(5.dp))
                Text("PLAY SMARTER", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("BE STRONGER", color = Color(0xFFE020FF), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("FREE FIRE  •  FREE FIRE MAX", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("Your gaming space. Your next match starts here.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2)
                Text("🔥  READY FOR YOUR NEXT MATCH", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            PremiumOsenMark(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).size(154.dp)
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
                Text("Prepare your phone, then launch your game.", color = Muted, fontSize = 12.sp)
                PremiumActionButton(
                    label = "⚡  START BOOST",
                    onClick = {
                        onRefresh()
                        status.value = "Safe preparation complete • Android manages game performance"
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    start = Cyan,
                    end = Violet
                )
                PremiumActionButton(label = "▶  PLAY FREE FIRE  🔥", onClick = { status.value = launchFreeFire(context) }, modifier = Modifier.fillMaxWidth().height(52.dp))
                Text("⚡  Free Fire & Free Fire MAX supported when installed", color = Cyan, fontSize = 10.sp)
            }
        }
        ThreeDMetalText("DEVICE OVERVIEW", 16, White, Cyan)
        Text("LIVE DEVICE INFORMATION", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonMetric("▦", "RAM AVAILABLE", stats.ramAvailable, "Memory ${stats.ramUsedPercent}%", Purple, Modifier.weight(1f), stats.ramProgress)
            NeonMetric("▤", "STORAGE FREE", stats.storageFree, "Internal ${stats.storageUsedPercent}%", Cyan, Modifier.weight(1f), stats.storageProgress)
        }
        Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(21.dp), clip = false).clickable { openDeviceSettings(context) }.border(1.8.dp, Brush.linearGradient(listOf(Color.White, Cyan, Violet)), RoundedCornerShape(21.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1024)), shape = RoundedCornerShape(21.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📱", fontSize = 31.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ThreeDMetalText("DEVICE INFORMATION", 10, Cyan, Violet)
                    Text(stats.deviceName, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Android ${Build.VERSION.RELEASE}  •  API ${Build.VERSION.SDK_INT}", color = Muted, fontSize = 11.sp)
                }
                Text("VIEW DETAILS  ➜", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        ThreeDMetalText("🎮  GAMING TOOLS", 19, Color(0xFFE7B8FF), Purple)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolTile("🚀", "GAME LAUNCHER", "Open games", Purple, Modifier.weight(1f)) { status.value = launchFreeFire(context) }
            ToolTile("⟳", "REFRESH", "Update info", Cyan, Modifier.weight(1f), onRefresh)
            ToolTile("⚡", "BOOSTER", "Performance info", Green, Modifier.weight(1f)) { status.value = "Android manages game performance automatically" }
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
private fun ToolsPage(onRefresh: () -> Unit, onCleaner: () -> Unit, onPhoneSettings: () -> Unit, onDeviceInfo: () -> Unit) {
    PageTitle("GAMING TOOLS", "Quick shortcuts for your device")
    ActionButton("📱  PHONE SETTINGS", Cyan, onPhoneSettings)
    ActionButton("▤  APP INFO", Gold, onCleaner)
    ActionButton("📊  DEVICE INFO", Purple, onDeviceInfo)
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
    PremiumActionButton(label, onClick, Modifier.fillMaxWidth().height(54.dp), color, Violet)
}

@Composable
private fun PremiumActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, start: Color = Purple, end: Color = Cyan) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.shadow(12.dp, shape).clip(shape)
            .background(Brush.horizontalGradient(listOf(start, Purple, end)))
            .border(1.4.dp, Brush.horizontalGradient(listOf(Color.White, start, end)), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = White, fontSize = 14.sp, fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = Shadow(Color(0xFF16002F), Offset(2f, 3f), 5f)))
    }
}

@Composable
private fun ThreeDMetalText(text: String, size: Int, face: Color, edge: Color, maxLines: Int = 1) {
    // Two crisp layers create a 3D embossed look without the doubled/ghosted lettering.
    Box {
        Text(
            text, color = Color(0xFF16002F), fontSize = size.sp,
            fontWeight = FontWeight.Black, letterSpacing = 0.6.sp,
            maxLines = maxLines,
            modifier = Modifier.offset(x = 1.5.dp, y = 2.5.dp)
        )
        Text(
            text, color = face, fontSize = size.sp,
            fontWeight = FontWeight.Black, letterSpacing = 0.6.sp,
            maxLines = maxLines,
            style = TextStyle(shadow = Shadow(edge.copy(alpha = 0.95f), Offset(0f, 0f), 7f))
        )
    }
}

@Composable
private fun NoteCard() {
    Card(modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(18.dp), clip = false).border(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Purple, Cyan)), RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF160C2C)), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ThreeDMetalText("💡  GAMING NOTE", 12, Cyan, Purple)
            Text("TurboPlay provides game launch and device settings shortcuts. Android controls performance automatically; this app cannot promise extra FPS or safely force-close other apps.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PremiumHeader(onSettings: () -> Unit, onBoost: () -> Unit, isBoosting: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF3B176B), Color(0xFF090B20))), RoundedCornerShape(15.dp))
                .border(1.5.dp, Brush.linearGradient(listOf(Gold, Purple, Cyan)), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-5).dp)) {
                Text("♛", color = Gold, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("OG", color = White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            ThreeDMetalText("OSEN", 19, White, Gold)
            ThreeDMetalText("TURBOPLAY", 17, Gold, Purple, maxLines = 1)
            Text("━  GAME BOOSTER  ━", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, maxLines = 1)
        }
        NeonCircle("⚙", Violet, "SETTINGS", onSettings)
        NeonCircle("⚡", Cyan, if (isBoosting) "BOOSTED" else "BOOST", onBoost, isBoosting)
    }
}

@Composable
private fun NeonCircle(symbol: String, color: Color, label: String, onClick: () -> Unit, isBoosting: Boolean = false) {
    val pulseScale = animateFloatAsState(targetValue = if (isBoosting) 1.12f else 1f, animationSpec = tween(durationMillis = 260), label = "boost_pulse")
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = pulseScale.value; scaleY = pulseScale.value }
            .width(66.dp)
            .height(76.dp)
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
        Text(if (isBoosting) "✓" else symbol, color = if (isBoosting) Green else White, fontSize = 30.sp, fontWeight = FontWeight.Black)
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
            Text(value, color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.9f), Offset(2f, 3f), 9f)))
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
    Card(modifier = modifier.height(132.dp).shadow(15.dp, RoundedCornerShape(17.dp), clip = false).clickable(onClick = onClick).border(1.6.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), accent, Color.White.copy(alpha = 0.35f), accent)), RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = Shadow(accent, Offset(0f, 0f), 12f)))
            Text(title, color = White, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(accent, Offset(1.5f, 2f), 6f)))
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
    // "Phone Settings" should open the main Android Settings screen, not Device Info.
    safeOpen(context, Intent(Settings.ACTION_SETTINGS))
}

private fun safeOpen(context: Context, intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        // Some Android builds do not provide every optional settings screen.
    }
}
