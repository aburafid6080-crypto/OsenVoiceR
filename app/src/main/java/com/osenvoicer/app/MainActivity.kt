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

private val ElectricBlue = Color(0xFF3985FF)
private val Cyan = Color(0xFF48D9F5)
private val DeepNavy = Color(0xFF050B18)

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
    Surface(modifier = Modifier.fillMaxSize(), color = DeepNavy) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF102B58), DeepNavy, Color(0xFF02050C))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("OSEN", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("BOOSTVOX", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("RX", color = ElectricBlue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
            Spacer(Modifier.height(12.dp))
            Text("GAME BOOSTER  ×  VOICE TOOLS", color = Color(0xFFD5E7FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onBooster,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
            ) {
                Text("🎮  OPEN GAME BOOSTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVoice,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10284A), contentColor = Cyan)
            ) {
                Text("🎙  OPEN VOICE TOOLS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Voice effects are for recording and playback. Direct live voice routing into Free Fire is not supported.",
                color = Color(0xFFB8CBE5), fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}