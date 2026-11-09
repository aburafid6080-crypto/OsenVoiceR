 fun play() {
        if (recording || playing) return
        if (!file.exists() || file.length() < 2) { status = "Record your voice first"; return }
        val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) { status = "Audio playback unavailable"; return }
        try {
            val fmt = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            val t = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(fmt).setBufferSizeInBytes(maxOf(min * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            if (effect == Effect.Chipmunk) t.playbackRate = 56000
            if (effect == Effect.Pitch) t.playbackRate = 52000
            track = t; playOn = true; playing = true; status = "Playing ${effect.label}…"
            thread {
                val delay = ShortArray(RATE / 3)
                var di = 0
                var low = 0.0
                var idx = 0L
                try {
                    t.play()
                    FileInputStream(file).use { input ->
                        val bytes = ByteArray(4096)
                        while (playOn) {
                            val n = input.read(bytes)
                            if (n <= 0) break
                            val size = n - n % 2
                            val out = ByteArray(size)
                            var p = 0
                            while (p < size) {
                                val raw = ((bytes[p + 1].toInt() shl 8) or (bytes[p].toInt() and 255)).toShort()
                                var x = raw.toDouble()
                                when (effect) {
                                    Effect.Normal -> Unit
                                    Effect.Deep -> { low += .15 * (x - low); x = low * 1.2 }
                                    Effect.Robot -> x *= sin(2 * PI * 38 * idx / RATE) * .9
                                    Effect.Echo, Effect.Reverb -> {
                                        val old = delay[di].toDouble()
                                        delay[di] = (x * if (effect == Effect.Echo) 1.0 else .55)
                                            .toInt().coerceIn(-32768, 32767).toShort()
                                        di = (di + 1) % delay.size
                                        x += old * if (effect == Effect.Echo) .48 else .30
                                    }
                                    Effect.Radio -> { low += .22 * (x - low); x = (x - low) * 1.8 }
                                    Effect.Monster -> { low += .10 * (x - low); x = (low * 1.7).coerceIn(-30000.0, 30000.0) }
                                    Effect.Chipmunk, Effect.Pitch -> x *= .95
                                }
                                val s = x.toInt().coerceIn(-32768, 32767)
                                out[p] = s.toByte(); out[p + 1] = (s shr 8).toByte()
                                p += 2; idx++
                            }
                            var off = 0
                            while (off < size && playOn) {
                                val w = t.write(out, off, size - off)
                                if (w <= 0) break
                                off += w
                            }
                        }
                    }
                } catch (_: Exception) {
                    (ctx as ComponentActivity).runOnUiThread { status = "Playback error" }
                } finally {
                    try { t.stop(); t.release() } catch (_: Exception) {}
                    (ctx as ComponentActivity).runOnUiThread {
                        track = null; playing = false; playOn = false
                        if (status.startsWith("Playing")) status = "Playback finished"
                    }
                }
            }
        } catch (_: Exception) { status = "Could not play audio"; playing = false }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status = if (granted) "Permission granted • Press Start Recording" else "Microphone permission denied"
    }

    DisposableEffect(Unit) {
        onDispose {
            recOn = false; playOn = false
            try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
            try { track?.stop(); track?.release() } catch (_: Exception) {}
        }
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0B0A18)) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("OSEN VOICE R", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("GAMING VOICE STUDIO", color = Color(0xFF55C8F2), fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().background(Color(0xFF191A31), RoundedCornerShape(24.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VOICE STUDIO", color = Color(0xFF9B6BFF))
                Spacer(Modifier.height(10.dp))
                Text("Create your sound.", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(status, color = Color(0xFFCECCDA), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRec()
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = !recording && !playing, modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B50F6))) {
                    Text("START RECORDING", color = Color(0xFF171027))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { stopRec() }, enabled = recording, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("STOP", color = Color(0xFFB58BFF))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { if (playing) stopPlay() else play() },
                    enabled = !recording && file.exists(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (playing) "STOP PLAYBACK" else "PLAY WITH EFFECT", color = Color(0xFFB58BFF))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("VOICE EFFECTS", color = Color(0xFF55C8F2), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Effect.values().toList()) { item ->
                    Surface(Modifier.fillMaxWidth().height(66.dp).clickable(enabled = !recording && !playing) {
                        effect = item; status = "${item.label} selected"
                    }, color = if (effect == item) Color(0xFF493078) else Color(0xFF202139),
                        shape = RoundedCornerShape(15.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(item.label, color = Color.White, fontSize = 17.sp,
