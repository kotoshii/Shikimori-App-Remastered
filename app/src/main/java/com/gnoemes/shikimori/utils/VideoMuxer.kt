package com.gnoemes.shikimori.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Joins a video only file and its separate audio file into one mp4.
 *
 * Some hostings stopped serving a muxed file per quality and hand out the picture and the sound as
 * two downloads instead, see `CdaParser`. Nothing is re-encoded here, the samples are copied
 * across as they are, so this is only as slow as reading and writing the two files.
 */
object VideoMuxer {

    private const val DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024

    //audio samples in one AAC frame
    private const val AAC_FRAME_SAMPLES = 1024

    //a run's frames may end this much past the next run's start and still be taken at their own time:
    //the frame times are rounded down to whole microseconds
    private const val OVERLAP_SLACK_US = 1000L

    //the sample rates an ADTS header can name, by its index
    private val ADTS_SAMPLE_RATES = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)

    /**
     * Reports 0..100 while samples are copied - muxing a long episode is not instant - and returns
     * false to abort. The callback is the **only** way to stop this: `MediaExtractor` and
     * `MediaMuxer` ignore thread interrupts, so cancelling the executor does not reach them.
     */
    private val NO_PROGRESS: (Int) -> Boolean = { true }

    /**
     * Returns true only when [outputPath] holds a complete file, so the caller can decide whether
     * it is safe to delete the parts.
     */
    fun mux(videoPath: String, audioPath: String, outputPath: String,
            onProgress: (Int) -> Boolean = NO_PROGRESS): Boolean = writeMp4(outputPath) { job ->
        val video = job.open(videoPath)
        val audio = job.open(audioPath)

        val videoTrack = video.findTrack("video/")
        val audioTrack = audio.findTrack("audio/")
        if (videoTrack < 0 || audioTrack < 0) return@writeMp4 false

        val videoFormat = video.getTrackFormat(videoTrack)
        val audioFormat = audio.getTrackFormat(audioTrack)

        val muxer = job.createMuxer()
        val outVideo = OutTrack(muxer.addTrack(videoFormat))
        val outAudio = OutTrack(muxer.addTrack(audioFormat))
        muxer.start()

        val buffer = ByteBuffer.allocate(maxOf(videoFormat.maxInputSize(), audioFormat.maxInputSize()))
        video.selectTrack(videoTrack)
        audio.selectTrack(audioTrack)
        //picture first, then sound, so the bar covers each half of the work
        val done = video.copySamples(mapOf(videoTrack to outVideo), muxer, buffer, videoFormat.durationUs(), 0, 50, onProgress) &&
                audio.copySamples(mapOf(audioTrack to outAudio), muxer, buffer, audioFormat.durationUs(), 50, 100, onProgress)

        if (done) muxer.stop()
        done
    }

    /**
     * Rewrites a single container into mp4 without re-encoding, used for the mpeg-ts that comes out
     * of an hls download. Every audio and video track is copied in one pass so the interleaving -
     * and with it a/v sync - survives; copying track by track would not preserve it.
     *
     * Returns false when the device cannot do it, so the caller can keep the original file rather
     * than lose the download.
     */
    fun remux(inputPath: String, outputPath: String, onProgress: (Int) -> Boolean = NO_PROGRESS): Boolean = writeMp4(outputPath) { job ->
        val source = job.open(inputPath)
        val muxer = job.createMuxer()

        val tracks = HashMap<Int, OutTrack>()
        var bufferSize = DEFAULT_BUFFER_SIZE
        var duration = 0L

        for (i in 0 until source.trackCount) {
            val format = source.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime == null || (!mime.startsWith("video/") && !mime.startsWith("audio/"))) continue

            tracks[i] = OutTrack(muxer.addTrack(format), mayBeAdts = mime == MediaFormat.MIMETYPE_AUDIO_AAC)
            bufferSize = maxOf(bufferSize, format.maxInputSize())
            if (mime.startsWith("video/")) duration = format.durationUs()
            source.selectTrack(i)
        }

        if (tracks.isEmpty()) return@writeMp4 false

        muxer.start()
        val done = source.copySamples(tracks, muxer, ByteBuffer.allocate(bufferSize), duration, 0, 100, onProgress)

        if (done) muxer.stop()
        done
    }

    /** What [writeMp4] cleans up after: the extractors opened for it and the muxer. */
    private class Job(private val outputPath: String) {
        val extractors = mutableListOf<MediaExtractor>()
        var muxer: MediaMuxer? = null

        fun open(path: String): MediaExtractor = MediaExtractor().also {
            extractors += it
            it.setDataSource(path)
        }

        fun createMuxer(): MediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
    }

    /**
     * Runs [write], which returns true once the muxer is stopped and the file complete, and cleans up
     * whatever happens: the extractors and the muxer released, the output deleted unless complete.
     */
    private fun writeMp4(outputPath: String, write: (Job) -> Boolean): Boolean {
        val job = Job(outputPath)
        var complete = false

        return try {
            complete = write(job)
            complete
        } catch (e: Throwable) {
            false
        } finally {
            job.extractors.forEach { it.releaseQuietly() }
            job.muxer.releaseQuietly()
            //deleted only after the muxer is released, so nothing still holds the file. A half
            //written mp4 is worse than none - the parts are kept and the caller decides.
            if (!complete) File(outputPath).takeIf { it.exists() }?.delete()
        }
    }

    /**
     * An output track. [mayBeAdts] for an AAC track out of mpeg-ts, whose samples may be runs of
     * ADTS frames; its first sample tells, see [AdtsTrack].
     */
    private class OutTrack(val index: Int, val mayBeAdts: Boolean = false) {
        var isChecked = false
        var adts: AdtsTrack? = null
    }

    /**
     * Copies the samples of the selected tracks in [tracks] - by the extractor's track index - in the
     * order they come. False when [onProgress] asked to stop, so the caller can discard a half written
     * file.
     */
    private fun MediaExtractor.copySamples(tracks: Map<Int, OutTrack>, muxer: MediaMuxer, buffer: ByteBuffer,
                                           duration: Long, from: Int, to: Int, onProgress: (Int) -> Boolean): Boolean {
        val info = MediaCodec.BufferInfo()

        while (true) {
            val size = readSampleData(buffer, 0)
            if (size < 0) break

            val percent = if (duration > 0 && to > from) {
                (from + (sampleTime * (to - from) / duration).toInt()).coerceIn(from, to)
            } else from
            //checked every sample, so a cancel lands quickly instead of at the end of the track
            if (!onProgress(percent)) return false

            val out = tracks[sampleTrackIndex]
            if (out != null) {
                if (out.mayBeAdts && !out.isChecked) {
                    out.isChecked = true
                    if (adtsFrame(buffer, 0, size) != null) out.adts = AdtsTrack(muxer, out.index)
                }

                //SAMPLE_FLAG_SYNC and BUFFER_FLAG_KEY_FRAME share the same value
                val adts = out.adts
                if (adts != null) adts.add(buffer, size, sampleTime, sampleFlags)
                else {
                    info.set(0, size, sampleTime, sampleFlags)
                    muxer.writeSampleData(out.index, buffer, info)
                }
            }
            advance()
        }

        tracks.values.forEach { it.adts?.flush() }
        return true
    }

    /**
     * An AAC track whose samples are runs of ADTS frames, headers included, where an mp4 track wants
     * one bare AAC frame per sample (BUGS #28 - such a file buffers forever in Google Photos). Each
     * frame goes in on its own, without its header, an AAC frame's time after the one before.
     *
     * A run is held until the next one comes, as its own time can be wrong: MediaExtractor counts the
     * time from the first picture and puts sound that starts before it at 0, so the first run would
     * overlap the second. Such a run is laid out to end where the next one starts, and what then falls
     * before 0 - sound from before the first picture - is dropped. So is a frame that would not come
     * after the one before, which the muxer refuses.
     */
    private class AdtsTrack(private val muxer: MediaMuxer, private val index: Int) {
        private var run: ByteArray? = null
        private var runTimeUs = 0L
        private var runFlags = 0
        private var lastTimeUs = -1L
        private val info = MediaCodec.BufferInfo()

        fun add(buffer: ByteBuffer, size: Int, timeUs: Long, flags: Int) {
            flush(nextTimeUs = timeUs)

            //a copy: the buffer is filled again with the next sample
            run = ByteArray(size).also { buffer.duplicate().apply { position(0); limit(size) }.get(it) }
            runTimeUs = timeUs
            runFlags = flags
        }

        /** Writes the run held back; [nextTimeUs] is where the next one starts, null after the last. */
        fun flush(nextTimeUs: Long? = null) {
            val bytes = run ?: return
            run = null

            val data = ByteBuffer.wrap(bytes)
            val frames = adtsFrames(data, bytes.size)
            val runUs = frames.fold(0L) { sum, frame -> sum + frame.durationUs }
            var time =
                    if (nextTimeUs != null && runTimeUs + runUs > nextTimeUs + OVERLAP_SLACK_US) nextTimeUs - runUs
                    else runTimeUs

            for (frame in frames) {
                write(data, frame.start + frame.headerSize, frame.length - frame.headerSize, time)
                time += frame.durationUs
            }

            //what does not read as an ADTS frame goes in as it is, as all of it did before
            val rest = frames.lastOrNull()?.let { it.start + it.length } ?: 0
            if (rest < bytes.size) write(data, rest, bytes.size - rest, time)
        }

        private fun write(data: ByteBuffer, offset: Int, size: Int, timeUs: Long) {
            if (timeUs < 0 || timeUs <= lastTimeUs) return

            info.set(offset, size, timeUs, runFlags)
            muxer.writeSampleData(index, data, info)
            lastTimeUs = timeUs
        }
    }

    private class AdtsFrame(val start: Int, val length: Int, val headerSize: Int, val sampleRate: Int) {
        val durationUs: Long
            get() = AAC_FRAME_SAMPLES * 1_000_000L / sampleRate
    }

    //the ADTS frames of a run, from its start up to the first bytes that do not read as one
    private fun adtsFrames(buffer: ByteBuffer, size: Int): List<AdtsFrame> {
        val frames = mutableListOf<AdtsFrame>()
        var start = 0
        while (true) {
            val frame = adtsFrame(buffer, start, size) ?: break
            frames += frame
            start += frame.length
        }
        return frames
    }

    //the ADTS frame at [start]: null when there is none, or when it holds more than one AAC frame
    private fun adtsFrame(buffer: ByteBuffer, start: Int, size: Int): AdtsFrame? {
        if (size - start < 7) return null
        fun byte(i: Int) = buffer.get(start + i).toInt() and 0xFF

        //the 12-bit sync word, then the layer, always 0
        if (byte(0) != 0xFF || (byte(1) and 0xF6) != 0xF0) return null

        //9 bytes when a CRC follows the header
        val headerSize = if ((byte(1) and 0x01) == 1) 7 else 9
        val sampleRate = ADTS_SAMPLE_RATES.getOrNull((byte(2) and 0x3C) shr 2) ?: return null
        val length = ((byte(3) and 0x03) shl 11) or (byte(4) shl 3) or (byte(5) shr 5)
        val aacFrames = (byte(6) and 0x03) + 1

        if (aacFrames != 1 || length <= headerSize || start + length > size) return null
        return AdtsFrame(start, length, headerSize, sampleRate)
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
