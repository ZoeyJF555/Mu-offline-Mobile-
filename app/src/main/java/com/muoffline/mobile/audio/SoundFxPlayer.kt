package com.muoffline.mobile.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.muoffline.mobile.model.ClassId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundFxPlayer {

    @Volatile
    var muted: Boolean = false

    @Volatile
    var sfxVolume: Float = 0.8f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 22050

    fun playAttack(classId: ClassId) {
        when (classId) {
            ClassId.DK -> playSweep(340.0, 120.0, 95, 0.22f)
            ClassId.DW -> playSweep(620.0, 920.0, 110, 0.18f)
            ClassId.ELF -> playSweep(880.0, 460.0, 85, 0.18f)
            ClassId.MG -> playSweep(420.0, 760.0, 105, 0.21f)
            ClassId.DL -> playSweep(280.0, 620.0, 115, 0.22f)
        }
    }

    fun playSkill() = playSweep(420.0, 960.0, 170, 0.24f)

    fun playPotion() = playNotes(listOf(523.25 to 70, 659.25 to 90), 0.20f)

    fun playJewelDrop() = playNotes(listOf(1046.5 to 85, 1318.5 to 110, 1567.98 to 150), 0.24f)

    fun playLevelUp() = playNotes(listOf(523.25 to 90, 659.25 to 90, 783.99 to 100, 1046.5 to 200), 0.26f)

    fun playChaosSuccess() = playNotes(listOf(659.25 to 90, 783.99 to 90, 1046.5 to 130, 1318.5 to 210), 0.26f)

    fun playChaosFail() = playSweep(280.0, 95.0, 260, 0.25f)

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, baseVol: Float) {
        if (muted || sfxVolume <= 0.01f) return
        val volume = baseVol * sfxVolume
        scope.launch {
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return@launch
            val pcm = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startHz + (endHz - startHz) * t
                val env = (1.0 - t)
                phase += 2.0 * PI * freq / sampleRate
                pcm[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            emitPcm(pcm)
        }
    }

    private fun playNotes(notes: List<Pair<Double, Int>>, baseVol: Float) {
        if (muted || sfxVolume <= 0.01f) return
        val volume = baseVol * sfxVolume
        scope.launch {
            val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
            if (totalSamples <= 0) return@launch
            val pcm = ShortArray(totalSamples)
            var cursor = 0
            for ((freq, dur) in notes) {
                val n = (sampleRate * dur) / 1000
                var phase = 0.0
                for (i in 0 until n) {
                    val env = 1.0 - (i.toDouble() / n) * 0.7
                    phase += 2.0 * PI * freq / sampleRate
                    pcm[cursor++] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            emitPcm(pcm)
        }
    }

    private fun emitPcm(pcm: ShortArray) {
        runCatching {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((pcm.size * 1000L) / sampleRate + 30L)
            track.stop()
            track.release()
        }
    }
}
