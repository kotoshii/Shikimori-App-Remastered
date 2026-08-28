package com.gnoemes.shikimori.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File
import java.nio.ByteBuffer

/**
 * Joins a video only file and its separate audio file into one mp4.
 *
 * Some hostings stopped serving a muxed file per quality and hand out the picture and the sound as
 * two downloads instead, see `CdaParserImpl`. Nothing is re-encoded here, the samples are copied
 * across as they are, so this is only as slow as reading and writing the two files.
 */
object VideoMuxer {

    private const val DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024

    /** Reports 0..100 while samples are copied. Muxing a long episode is not instant. */
    private val NO_PROGRESS: (Int) -> Unit = {}

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2

    /**
     * Returns true only when [outputPath] holds a complete file, so the caller can decide whether
     * it is safe to delete the parts.
     */
    fun mux(videoPath: String, audioPath: String, outputPath: String,
            onProgress: (Int) -> Unit = NO_PROGRESS): Boolean {
        if (!isSupported) return false

        var video: MediaExtractor? = null
        var audio: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            video = MediaExtractor().apply { setDataSource(videoPath) }
            audio = MediaExtractor().apply { setDataSource(audioPath) }

            val videoTrack = video.findTrack("video/")
            val audioTrack = audio.findTrack("audio/")
            if (videoTrack < 0 || audioTrack < 0) return false

            val videoFormat = video.getTrackFormat(videoTrack)
            val audioFormat = audio.getTrackFormat(audioTrack)

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideo = muxer.addTrack(videoFormat)
            val outAudio = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(maxOf(videoFormat.maxInputSize(), audioFormat.maxInputSize()))
            //picture first, then sound, so the bar covers each half of the work
            video.copyTrack(videoTrack, muxer, outVideo, buffer, videoFormat.durationUs(), 0, 50, onProgress)
            audio.copyTrack(audioTrack, muxer, outAudio, buffer, audioFormat.durationUs(), 50, 100, onProgress)

            muxer.stop()
            true
        } catch (e: Throwable) {
            //a half written file is worse than none, the parts are kept instead
            File(outputPath).takeIf { it.exists() }?.delete()
            false
        } finally {
            video.releaseQuietly()
            audio.releaseQuietly()
            muxer.releaseQuietly()
        }
    }

    /**
     * Rewrites a single container into mp4 without re-encoding, used for the mpeg-ts that comes out
     * of an hls download. Every audio and video track is copied in one pass so the interleaving -
     * and with it a/v sync - survives; copying track by track would not preserve it.
     *
     * Returns false when the device cannot do it, so the caller can keep the original file rather
     * than lose the download.
     */
    fun remux(inputPath: String, outputPath: String, onProgress: (Int) -> Unit = NO_PROGRESS): Boolean {
        if (!isSupported) return false

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            val source = MediaExtractor().also { extractor = it }
            source.setDataSource(inputPath)

            val output = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }

            val tracks = HashMap<Int, Int>()
            var bufferSize = DEFAULT_BUFFER_SIZE
            var duration = 0L

            for (i in 0 until source.trackCount) {
                val format = source.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime == null || (!mime.startsWith("video/") && !mime.startsWith("audio/"))) continue

                tracks[i] = output.addTrack(format)
                bufferSize = maxOf(bufferSize, format.maxInputSize())
                if (mime.startsWith("video/")) duration = format.durationUs()
                source.selectTrack(i)
            }

            if (tracks.isEmpty()) return false

            output.start()

            val buffer = ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val size = source.readSampleData(buffer, 0)
                if (size < 0) break

                if (duration > 0) onProgress((source.sampleTime * 100 / duration).toInt().coerceIn(0, 100))

                val outTrack = tracks[source.sampleTrackIndex]
                if (outTrack != null) {
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = source.sampleTime
                    info.flags = source.sampleFlags

                    output.writeSampleData(outTrack, buffer, info)
                }
                source.advance()
            }

            output.stop()
            true
        } catch (e: Throwable) {
            File(outputPath).takeIf { it.exists() }?.delete()
            false
        } finally {
            extractor.releaseQuietly()
            muxer.releaseQuietly()
        }
    }

    private fun MediaExtractor.findTrack(mimePrefix: String): Int {
        for (i in 0 until trackCount) {
            val mime = getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith(mimePrefix)) return i
        }
        return -1
    }

    private fun MediaFormat.maxInputSize(): Int =
            if (containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            else DEFAULT_BUFFER_SIZE

    private fun MediaFormat.durationUs(): Long =
            if (containsKey(MediaFormat.KEY_DURATION)) getLong(MediaFormat.KEY_DURATION) else 0L

    private fun MediaExtractor.copyTrack(track: Int, muxer: MediaMuxer, outTrack: Int, buffer: ByteBuffer,
                                         duration: Long = 0L, from: Int = 0, to: Int = 0,
                                         onProgress: (Int) -> Unit = NO_PROGRESS) {
        selectTrack(track)
        val info = MediaCodec.BufferInfo()

        while (true) {
            val size = readSampleData(buffer, 0)
            if (size < 0) break

            if (duration > 0 && to > from) {
                val done = (sampleTime * (to - from) / duration).toInt()
                onProgress((from + done).coerceIn(from, to))
            }

            info.offset = 0
            info.size = size
            info.presentationTimeUs = sampleTime
            //SAMPLE_FLAG_SYNC and BUFFER_FLAG_KEY_FRAME share the same value
            info.flags = sampleFlags

            muxer.writeSampleData(outTrack, buffer, info)
            advance()
        }

        unselectTrack(track)
    }

    private fun MediaExtractor?.releaseQuietly() = try {
        this?.release()
    } catch (e: Throwable) {
        Unit
    }

    private fun MediaMuxer?.releaseQuietly() = try {
        this?.release()
    } catch (e: Throwable) {
        Unit
    }
}
