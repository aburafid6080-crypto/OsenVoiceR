package com.osenvoicer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFD6AD55)
private val Black = Color(0xFF090909)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CombinedHome(
                    onBooster = { startActivity(Intent(this, com.osenturboplay.app.TurboPlayActivity::class.java)) },
                    onVoice = { startActivity(Intent(this, VoiceActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun CombinedHome(onBooster: () -> Unit, onVoice: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Black) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF242016), Black, Color(0xFF080808))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("OSEN", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("BOOSTVOX", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("RX", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
            Spacer(Modifier.height(12.dp))
            Text("GAME BOOSTER  ×  VOICE TOOLS", color = Color(0xFFE2D6B9), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onBooster,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B762F), contentColor = Color.White)
            ) {
                Text("🎮  OPEN GAME BOOSTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVoice,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29251C), contentColor = Gold)
            ) {
                Text("🎙  OPEN VOICE TOOLS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Voice effects are for recording and playback. Direct live voice routing into Free Fire is not supported.",
                color = Color(0xFFD8CBAE), fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}