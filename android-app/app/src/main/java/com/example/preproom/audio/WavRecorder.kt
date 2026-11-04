package com.example.preproom.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class WavRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val outputStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return
        outputStream.reset()
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val audioData = ByteArray(bufferSize)
            while (isRecording) {
                val readSize = audioRecord?.read(audioData, 0, audioData.size) ?: 0
                if (readSize > 0) {
                    outputStream.write(audioData, 0, readSize)
                }
            }
        }.start()
    }

    suspend fun stopRecording(): ByteArray = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext ByteArray(0)
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val pcmData = outputStream.toByteArray()
        addWavHeader(pcmData)
    }

    fun cancelRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        outputStream.reset()
    }

    private fun addWavHeader(pcmData: ByteArray): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * 1 * 16 / 8
        val channels = 1

        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)

        try {
            dos.writeBytes("RIFF")
            dos.writeInt(Integer.reverseBytes(totalDataLen))
            dos.writeBytes("WAVE")
            dos.writeBytes("fmt ")
            dos.writeInt(Integer.reverseBytes(16)) // Subchunk1Size
            dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (PCM)
            dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
            dos.writeInt(Integer.reverseBytes(sampleRate))
            dos.writeInt(Integer.reverseBytes(byteRate))
            dos.writeShort(java.lang.Short.reverseBytes((channels * 16 / 8).toShort()).toInt()) // BlockAlign
            dos.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt()) // BitsPerSample
            dos.writeBytes("data")
            dos.writeInt(Integer.reverseBytes(pcmData.size))
            dos.write(pcmData)
            dos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return out.toByteArray()
    }
}
