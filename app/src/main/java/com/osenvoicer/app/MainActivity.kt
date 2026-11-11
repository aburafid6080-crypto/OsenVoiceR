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
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070416)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF180A35), Color(0xFF070416), Color(0xFF061A2A))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("OSEN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("TURBOPLAY", color = Color(0xFF00D9FF), fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text("+ VOICE R", color = Color(0xFFC04BFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Your gaming and voice tools in one app", color = Color(0xFFD9D1EA), fontSize = 13.sp)
            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onBooster,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6420C8))
            ) {
                Text("🎮  OPEN GAME BOOSTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVoice,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176A9A))
            ) {
                Text("🎙  OPEN VOICE TOOLS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Voice effects are for recording and playback. Direct live voice routing into Free Fire is not supported.",
                color = Color(0xFFFFC6E9), fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}