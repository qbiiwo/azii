package io.azii

object WavEncoder {

    fun encode(
        pcm: ByteArray,
        sampleRate: Int = 16000
    ): ByteArray {

        require(pcm.isNotEmpty()) {
            "Audio is empty"
        }

        require(pcm.size % 2 == 0) {
            "Invalid PCM data"
        }

        require(sampleRate > 0) {
            "Invalid sample rate"
        }

        val channels = 1
        val bitsPerSample = 16
        val bytesPerSample = bitsPerSample / 8

        val wav = ByteArray(44 + pcm.size)

        fun writeString(offset: Int, value: String) {
            value.encodeToByteArray().copyInto(
                wav,
                destinationOffset = offset
            )
        }

        fun writeShort(offset: Int, value: Int) {
            wav[offset] = (value and 0xFF).toByte()
            wav[offset + 1] = (value shr 8).toByte()
        }

        fun writeInt(offset: Int, value: Long) {
            for (i in 0..3) {
                wav[offset + i] =
                    (value shr (i * 8)).toByte()
            }
        }

        // RIFF header
        writeString(0, "RIFF")
        writeInt(4, 36L + pcm.size)

        writeString(8, "WAVE")

        // Format chunk
        writeString(12, "fmt ")
        writeInt(16, 16)

        writeShort(20, 1)
        writeShort(22, channels)

        writeInt(24, sampleRate.toLong())

        val byteRate =
            sampleRate * channels * bytesPerSample

        writeInt(28, byteRate.toLong())

        writeShort(
            32,
            channels * bytesPerSample
        )

        writeShort(34, bitsPerSample)

        // Audio data
        writeString(36, "data")
        writeInt(40, pcm.size.toLong())

        pcm.copyInto(
            destination = wav,
            destinationOffset = 44
        )

        return wav
    }
}