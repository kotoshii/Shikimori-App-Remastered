package com.gnoemes.shikimori.utils.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spaces out every call to shikimori so bursts stop coming back as 429.
 *
 * The host enforces two separate limits, both measured against the live api:
 *
 * - a burst one: nine requests sent at once got four 429s, while requests 200ms apart got none;
 * - a sustained one: even at a clean 200ms spacing the 91st request inside a minute got a 429.
 *
 * So spacing alone is not enough - 200ms sustained is 300 requests per minute, far past what the
 * host allows. Both tiers are enforced here.
 *
 * The app bursts by design: a details screen loads details, roles, related, links, screenshots and
 * similar at once, and chronology follows its franchise call with one search per chunk of ids
 * through `Single.zip`. OkHttp happily runs five of those per host in parallel.
 *
 * Each caller reserves the next free slot and sleeps until it, so arrival order is kept and no
 * request is ever dropped - they just start one after another.
 *
 * Shared by the rest clients and the graphql one, so poster lookups queue behind the screens' own
 * calls instead of competing with them. Glide builds its own client and is unaffected.
 */
@Singleton
class ShikimoriRateLimiter @Inject constructor() : Interceptor {

    private val lock = Any()

    /** Earliest departure the burst limit allows. */
    private var nextSlotNanos = System.nanoTime()

    /** Departure times already handed out inside the current window, oldest first. */
    private val window = ArrayDeque<Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        awaitSlot()
        return chain.proceed(chain.request())
    }

    private fun awaitSlot() {
        val waitNanos = reserveSlot()

        if (waitNanos > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(waitNanos)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /** Books the next departure and returns how long the caller has to wait for it. */
    private fun reserveSlot(): Long = synchronized(lock) {
        val now = System.nanoTime()

        //subtraction, not comparison - nanoTime is only meaningful as a difference
        var slot = if (nextSlotNanos - now > 0) nextSlotNanos else now
        forgetDeparturesBefore(slot)

        //window still full at that point, so wait for the oldest departure to age out of it
        if (window.size >= MAX_PER_WINDOW) {
            slot = window.peekFirst() + WINDOW_NANOS
            forgetDeparturesBefore(slot)
        }

        window.addLast(slot)
        nextSlotNanos = slot + MIN_INTERVAL_NANOS
        slot - now
    }

    private fun forgetDeparturesBefore(slot: Long) {
        while (!window.isEmpty() && slot - window.peekFirst() >= WINDOW_NANOS) window.pollFirst()
    }

    companion object {
        /** 200ms measured as the point where throttling stops */
        private val MIN_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(200)

        private val WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1)

        /**
         * The 91st request inside a minute was refused, so 90 is the ceiling.
         */
        private const val MAX_PER_WINDOW = 90
    }
}
