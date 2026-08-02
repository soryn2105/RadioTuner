package com.sorin.radiotuner.util

/**
 * (RO)
 * Strategie de reconectare cu backoff progresiv, ca să evităm bucla de
 *  reîncercări instant-la-nesfârșit atunci când internetul revine dar
 *  streamul mai are nevoie de o clipă, sau când conexiunea e instabilă.
 *
 * Folosire tipică:
 *    val strategy = ReconnectStrategy()
 *    la fiecare eșec: delay(strategy.nextDelayMillis())
 *    la succes: strategy.reset()
 * (EN)
 * Reconnect strategy with progressive backoff, to avoid the loop of
 * instant retries when the internet comes back but the stream still needs a moment,
 * or when the connection is unstable.
 * Typical usage:
 *   val strategy = ReconnectStrategy()
 *   on each failure: delay(strategy.nextDelayMillis())
 *   on success: strategy.reset()
 */
class ReconnectStrategy(
    private val baseDelayMillis: Long = 2_000L,
    private val maxDelayMillis: Long = 30_000L
) {
    private var attempt = 0

    /** Calculează următoarea întârziere (2s, 4s, 8s, 16s, ..., plafonat la maxDelayMillis). */
    fun nextDelayMillis(): Long {
        val delay = baseDelayMillis * (1L shl attempt.coerceAtMost(10))
        attempt++
        return delay.coerceAtMost(maxDelayMillis)
    }

    /** Se apelează după o reconectare reușită — reîncepem de la cea mai mică întârziere. */
    fun reset() {
        attempt = 0
    }
}